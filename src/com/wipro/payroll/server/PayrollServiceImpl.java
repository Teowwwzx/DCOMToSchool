package com.wipro.payroll.server;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.wipro.payroll.common.*;
import org.mindrot.jbcrypt.BCrypt;

public class PayrollServiceImpl extends UnicastRemoteObject implements PayrollService {

    private Connection dbConnection;

    public PayrollServiceImpl() throws RemoteException {
        super();
        try {
            String dbUrl = "jdbc:postgresql://ep-withered-flower-a811hc2n-pooler.eastus2.azure.neon.tech:5432/neondb?sslmode=require&channel_binding=require";
            String dbUser = "neondb_owner";
            String dbPassword = "npg_4A1VduWMcaYT";
            dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("✅ Successfully connected to the PostgreSQL database.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RemoteException("Server could not connect to the database.", e);
        }
    }

    @Override
    public User login(String username, String password) throws RemoteException {
        System.out.println("[SERVER] Login attempt for username: " + username);
        String sql = "SELECT u.*, jt.dept_id FROM \"user\" u " +
                "LEFT JOIN \"job_titles\" jt ON u.job_title_id = jt.id " +
                "WHERE u.username = ? AND u.status = 'ACTIVE'";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("pwd_hash");
                String status = rs.getString("status");

                if (!status.equals("ACTIVE")) {
                    System.out.println("[SERVER] Login failed: User '" + username + "' found, but their account status is '" + status + "'.");
                    return null; // For security, send a generic message to the client
                }

                if (BCrypt.checkpw(password, storedHash)) {
                    int userId = rs.getInt("id");
                    System.out.println("[SERVER] Login successful for user ID: " + userId);
                    return buildUserFromResultSet(rs); // Use a helper to build the full User object
                } else {
                    // PASSWORD IS WRONG!
                    System.out.println("[SERVER] Login failed: User '" + username + "' found, but the password was incorrect.");
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error during login for user: " + username, e);
        }

        System.out.println("[SERVER] Login failed for username: " + username + password);
        return null; // Return null if user not found or password incorrect
    }

    @Override
    public String registerUser(User newUser, String password) throws RemoteException {
        System.out.println("[SERVER] Registration attempt for username: " + newUser.getUsername());
        // Using a transaction to ensure both user and user_role are created, or neither.
        try {
            dbConnection.setAutoCommit(false); // Start transaction

            // 1. Insert into the "user" table
            String userSql = "INSERT INTO \"user\" (dept_id, username, f_name, l_name, email, ic, pwd_hash, status, phone_number) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";

            // Get the generated user ID back
            PreparedStatement userStmt = dbConnection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userStmt.setInt(1, newUser.getDepartmentId());
            userStmt.setString(2, newUser.getUsername());
            userStmt.setString(3, newUser.getFirstName());
            userStmt.setString(4, newUser.getLastName());
            userStmt.setString(5, newUser.getEmail());
            userStmt.setString(6, newUser.getIc());
            userStmt.setString(7, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            userStmt.setString(8, newUser.getPhoneNumber());
            userStmt.executeUpdate();

            // Get the new user's generated ID
            ResultSet generatedKeys = userStmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                dbConnection.rollback();
                return "Error: Could not create user.";
            }
            int newUserId = generatedKeys.getInt(1);

            // 2. Assign the default 'EMPLOYEE' role (assuming role_id=1)
            String roleSql = "INSERT INTO \"user_role\" (user_id, role_id) VALUES (?, ?)";
            PreparedStatement roleStmt = dbConnection.prepareStatement(roleSql);
            roleStmt.setInt(1, newUserId);
            roleStmt.setInt(2, 1); // Role ID 1 = EMPLOYEE
            roleStmt.executeUpdate();

            dbConnection.commit(); // Finalize transaction
            System.out.println("[SERVER] User registered successfully with ID: " + newUserId);
            return "Registration successful for " + newUser.getUsername();

        } catch (SQLException e) {
            try { dbConnection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            throw new RemoteException("Database error during registration.", e);
        } finally {
            try { dbConnection.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }


    @Override
    public Payslip getMyLatestPayslip(int userId) throws RemoteException {
        System.out.println("[SERVER] Fetching latest payslip for user ID: " + userId);
        Payslip payslip = null;
        String payslipSql = "SELECT * FROM \"payslip\" WHERE user_id = ? ORDER BY pay_period_end_date DESC LIMIT 1";

        try (PreparedStatement stmt = dbConnection.prepareStatement(payslipSql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                payslip = new Payslip();
                payslip.setId(rs.getInt("id"));
                payslip.setUserId(rs.getInt("user_id"));
                payslip.setPayPeriodStartDate(rs.getDate("pay_period_start_date").toLocalDate());
                payslip.setPayPeriodEndDate(rs.getDate("pay_period_end_date").toLocalDate());
                payslip.setRemark(rs.getString("remark"));

                // Now fetch the associated pay items
                List<PayItem> items = getPayItemsForPayslip(payslip.getId());
                payslip.setPayItems(items);

                // Calculate totals on the fly
                calculatePayslipTotals(payslip);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Could not fetch latest payslip for user " + userId, e);
        }
        return payslip;
    }


    @Override
    public UserBankDetails getMyBankDetails(int userId) throws RemoteException {
        String sql = "SELECT * FROM \"user_bank_details\" WHERE user_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                UserBankDetails details = new UserBankDetails();
                details.setUserId(rs.getInt("user_id"));
                details.setBankName(rs.getString("bank_name"));
                details.setAccountNumber(rs.getString("acc_no"));
                details.setAccountHolderName(rs.getString("acc_name"));
                return details;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error fetching bank details for user " + userId, e);
        }
        return null; // Return null if no details are found
    }


    @Override
    public boolean updateMyBankDetails(int userId, UserBankDetails details) throws RemoteException {
        System.out.println("[SERVER] Attempting to update bank details for user ID: " + userId);

        // This SQL command is an "UPSERT". It attempts to INSERT a new row.
        // If a row with the same user_id already exists (ON CONFLICT), it performs an UPDATE instead.
        // This is a robust way to handle both creating and updating details.
        String sql = "INSERT INTO \"user_bank_details\" (user_id, bank_name, acc_no, acc_name) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (user_id) DO UPDATE SET " +
                "bank_name = EXCLUDED.bank_name, " +
                "acc_no = EXCLUDED.acc_no, " +
                "acc_name = EXCLUDED.acc_name";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, details.getBankName());
            stmt.setString(3, details.getAccountNumber());
            stmt.setString(4, details.getAccountHolderName());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // Returns true if the insert or update was successful

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error while updating bank details for user " + userId, e);
        }
    }


    @Override
    public String runMonthlyPayroll(int adminUserId) throws RemoteException {
        System.out.println("[SERVER] Payroll run initiated by admin ID: " + adminUserId);

        // --- Security Check ---
        // In a real system, you'd verify the token. Here, we'll check the role.
        if (!isUserInRole(adminUserId, "HR")) {
            throw new SecurityException("User does not have permission to run payroll.");
        }

        // For simplicity, we'll run for the previous month
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(1).minusDays(1);

        List<User> activeUsers = getAllUsers(adminUserId); // Reuse existing method
        int successCount = 0;

        for (User user : activeUsers) {
            try {
                dbConnection.setAutoCommit(false); // Transaction per user

                // 1. Create the payslip header
                String payslipSql = "INSERT INTO \"payslip\" (user_id, pay_period_start_date, pay_period_end_date, remark) VALUES (?, ?, ?, ?)";
                PreparedStatement payslipStmt = dbConnection.prepareStatement(payslipSql, Statement.RETURN_GENERATED_KEYS);
                payslipStmt.setInt(1, user.getId());
                payslipStmt.setDate(2, Date.valueOf(startOfMonth));
                payslipStmt.setDate(3, Date.valueOf(endOfMonth));
                payslipStmt.setString(4, "Payroll for " + startOfMonth.getMonth().toString());
                payslipStmt.executeUpdate();

                ResultSet generatedKeys = payslipStmt.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    throw new SQLException("Failed to create payslip for user " + user.getId());
                }
                int payslipId = generatedKeys.getInt(1);

                // 2. Define business rules & add pay items
                // This is where your predefined formulas go!
                BigDecimal baseSalary = new BigDecimal("5000.00"); // Example fixed salary

                insertPayItem(payslipId, "Base Salary", PayItemType.EARNING, baseSalary);

                // Rule: EPF is 11% of base salary
                BigDecimal epf = baseSalary.multiply(new BigDecimal("0.11"));
                insertPayItem(payslipId, "EPF Contribution", PayItemType.DEDUCTION, epf);

                // Rule: SOCSO is a fixed small amount
                BigDecimal socso = new BigDecimal("24.75");
                insertPayItem(payslipId, "SOCSO Contribution", PayItemType.DEDUCTION, socso);

                dbConnection.commit();
                successCount++;

            } catch (SQLException e) {
                try { dbConnection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                System.err.println("Failed to process payroll for user ID: " + user.getId());
                e.printStackTrace();
            } finally {
                try { dbConnection.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return String.format("Payroll run complete. Successfully processed %d out of %d users.", successCount, activeUsers.size());
    }


    @Override
    public List<User> getAllUsers(int adminUserId) throws RemoteException {
        if (!isUserInRole(adminUserId, "HR")) {
            throw new SecurityException("User does not have permission to view all users.");
        }

        List<User> userList = new ArrayList<>();
        String sql = "SELECT u.* FROM \"user\" u " +
                "JOIN \"user_role\" ur ON u.id = ur.user_id " +
                "JOIN \"role\" r ON ur.role_id = r.id " +
                "WHERE u.status = 'ACTIVE' AND r.name = 'EMPLOYEE'";

        try (Statement stmt = dbConnection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                userList.add(buildUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Could not fetch all users.", e);
        }
        return userList;
    }

    // --- Helper Methods ---

    /** A private helper to build a full User object from a ResultSet row. */
    private User buildUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        int userId = rs.getInt("id");
        user.setId(userId);
        user.setDepartmentId(rs.getInt("dept_id"));
        user.setUsername(rs.getString("username"));
        user.setFirstName(rs.getString("f_name"));
        user.setLastName(rs.getString("l_name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone"));
        user.setIc(rs.getString("ic"));
        // ... set other fields

        // Fetch and set the user's roles
        user.setRoles(getRolesForUser(userId));
        return user;
    }

    /** A private helper to get all roles for a specific user. */
    private List<Role> getRolesForUser(int userId) throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT r.id, r.name FROM \"role\" r JOIN \"user_role\" ur ON r.id = ur.role_id WHERE ur.user_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
                roles.add(role);
            }
        }
        return roles;
    }

    /** A private helper to get all pay items for a specific payslip. */
    private List<PayItem> getPayItemsForPayslip(int payslipId) throws SQLException {
        List<PayItem> items = new ArrayList<>();
        String sql = "SELECT * FROM \"pay_items\" WHERE payslip_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, payslipId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                PayItem item = new PayItem();
                item.setId(rs.getInt("id"));
                item.setPayslipId(rs.getInt("payslip_id"));
                item.setName(rs.getString("name"));
                item.setType(PayItemType.valueOf(rs.getString("type")));
                item.setAmount(rs.getBigDecimal("amount"));
                items.add(item);
            }
        }
        return items;
    }

    /** A helper to check if a user has a specific role. */
    private boolean isUserInRole(int userId, String roleName) {
        try {
            List<Role> roles = getRolesForUser(userId);
            for (Role role : roles) {
                if (role.getName().equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** A helper to insert a pay item record. */
    private void insertPayItem(int payslipId, String name, PayItemType type, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO \"pay_items\" (payslip_id, name, type, amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, payslipId);
            stmt.setString(2, name);
            stmt.setString(3, type.name());
            stmt.setBigDecimal(4, amount);
            stmt.executeUpdate();
        }
    }

    /** A helper to calculate totals for a payslip object. */
    private void calculatePayslipTotals(Payslip payslip) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        for (PayItem item : payslip.getPayItems()) {
            if (item.getType() == PayItemType.EARNING) {
                gross = gross.add(item.getAmount());
            } else {
                deductions = deductions.add(item.getAmount());
            }
        }
        payslip.setGrossEarnings(gross);
        payslip.setTotalDeductions(deductions);
        payslip.setNetPay(gross.subtract(deductions));
    }

    // Add these to PayrollServiceImpl.java

    @Override
    public List<JobTitle> getAllJobTitles(int adminUserId) throws RemoteException {
        if (!isUserInRole(adminUserId, "HR")) {
            throw new SecurityException("User does not have permission to view job titles.");
        }
        List<JobTitle> jobTitles = new ArrayList<>();
        String sql = "SELECT * FROM \"job_titles\" ORDER BY dept_id, level";
        try (Statement stmt = dbConnection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JobTitle jobTitle = new JobTitle();
                jobTitle.setId(rs.getInt("id"));
                jobTitle.setDeptId(rs.getInt("dept_id"));
                jobTitle.setTitle(rs.getString("title"));
                jobTitle.setLevel(rs.getString("level"));
                jobTitles.add(jobTitle);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching job titles.", e);
        }
        return jobTitles;
    }

    @Override
    public boolean updatePayTemplateItem(int adminUserId, int payTemplateItemId, BigDecimal newAmount) throws RemoteException {
        if (!isUserInRole(adminUserId, "HR")) {
            throw new SecurityException("User does not have permission to update pay templates.");
        }
        String sql = "UPDATE \"pay_templates\" SET amount = ? WHERE id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newAmount);
            stmt.setInt(2, payTemplateItemId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RemoteException("Error updating pay template item.", e);
        }
    }

    // Add this method to PayrollServiceImpl.java

    @Override
    public List<PayTemplate> getPayTemplatesForJobTitle(int adminUserId, int jobTitleId) throws RemoteException {
        if (!isUserInRole(adminUserId, "HR")) {
            throw new SecurityException("User does not have permission to view pay templates.");
        }

        List<PayTemplate> templates = new ArrayList<>();
        String sql = "SELECT * FROM \"pay_templates\" WHERE job_title_id = ?";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, jobTitleId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PayTemplate item = new PayTemplate();
                item.setId(rs.getInt("id"));
                item.setJob_title_id(rs.getInt("job_title_id"));
                item.setDescription(rs.getString("description"));
                // Convert the String from the DB back to a PayItemType enum
                item.setType(PayItemType.valueOf(rs.getString("type")));
                item.setAmount(rs.getBigDecimal("amount"));
                templates.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error fetching pay templates for job title ID: " + jobTitleId, e);
        }
        return templates;
    }
}
