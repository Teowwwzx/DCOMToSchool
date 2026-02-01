package com.wipro.payroll.server;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wipro.payroll.common.*;

public class PayrollServiceImpl extends UnicastRemoteObject implements PayrollService {
    private static final int RMI_OBJECT_PORT = 1100;

    public PayrollServiceImpl() throws RemoteException {
        super(RMI_OBJECT_PORT);
        System.out.println("✅ PayrollServiceImpl instance created and ready.");
    }

    // --- AUTHENTICATION ---
    @Override
    public User login(String username, String password) throws RemoteException {
        User user = null;

        String sql = "SELECT id, username, f_name, l_name, email, phone, ic, status, role " +
                "FROM public.user WHERE username = ? AND pwd_hash = ?";

        try (Connection conn = DatabaseManager.getConnection(); // Assumes you have a DatabaseManager
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashPassword(password)); // Hash the provided password for comparison

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFName(rs.getString("f_name"));
                user.setLName(rs.getString("l_name"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setIc(rs.getString("ic"));

                // Convert the String from DB to a Role enum
                String roleStr = rs.getString("role");
                if (roleStr != null) {
                    user.setRole(Role.valueOf(roleStr.toUpperCase()));
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error during login for user: " + username);
            e.printStackTrace();
            // It's good practice to wrap the SQLException in a RemoteException for the client
            throw new RemoteException("Server database error, please try again later.", e);
        } catch (Exception e) {
            // Catch hashing errors
            e.printStackTrace();
            throw new RemoteException("Server security error.", e);
        }

        return user; // Returns the full User object on success, or null on failure
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @Override
    public boolean verifyCurrentUserPassword(int actorUserId, String password) throws RemoteException {
        String storedHash = "";
        String sql = "SELECT pwd_hash FROM public.\"user\" WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actorUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                storedHash = rs.getString("pwd_hash");
            } else {
                return false; // User not found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error verifying password.", e);
        }

        // Hash the provided password and compare it to the stored hash
        String providedHash = hashPassword(password); // Reuse our existing helper
        return providedHash.equals(storedHash);
    }

    @Override
    public Payslip getPayslipById(int actorUserId, int payslipId) throws RemoteException {
        // Add security check for HR role here
        try {
            return fetchFullPayslipDetails(payslipId);
        } catch (SQLException e) {
            throw new RemoteException("Error fetching payslip by ID.", e);
        }
    }

    private Payslip fetchFullPayslipDetails(int payslipId) throws SQLException, RemoteException {
        Payslip payslip = new Payslip();
        String payslipDetailsSql = "SELECT * FROM payslip WHERE id = ?";
        String itemsSql = "SELECT name, type, amount FROM pay_items WHERE payslip_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement psPayslip = conn.prepareStatement(payslipDetailsSql)) {
                psPayslip.setInt(1, payslipId);
                ResultSet rsPayslip = psPayslip.executeQuery();
                if (rsPayslip.next()) {
                    payslip.setId(payslipId);
                    payslip.setUserId(rsPayslip.getInt("user_id"));
                    payslip.setPayPeriodStartDate(rsPayslip.getDate("pay_period_start_date").toLocalDate());
                    payslip.setPayPeriodEndDate(rsPayslip.getDate("pay_period_end_date").toLocalDate());
                    payslip.setGrossEarnings(rsPayslip.getBigDecimal("gross_earnings"));
                    payslip.setTotalDeductions(rsPayslip.getBigDecimal("total_deductions"));
                    payslip.setNetPay(rsPayslip.getBigDecimal("net_pay"));
                } else {
                    return null; // No payslip with this ID found
                }
            }

            try (PreparedStatement psItems = conn.prepareStatement(itemsSql)) {
                psItems.setInt(1, payslipId);
                ResultSet rsItems = psItems.executeQuery();
                while (rsItems.next()) {
                    PayItem item = new PayItem();
                    item.setName(rsItems.getString("name"));
                    item.setAmount(rsItems.getBigDecimal("amount"));
                    item.setType(PayItemType.valueOf(rsItems.getString("type")));
                    payslip.getPayItems().add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Re-throw to be handled by the calling method
        }
        return payslip;
    }

    @Override
    public boolean deleteUser(int actorUserId, int userIdToDelete) throws RemoteException {
        // Security Check for HR Role
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: You do not have HR privileges.");
            }
        } catch (SQLException e) { throw new RemoteException("Could not verify permissions.", e); }

        // Safety Check: Prevent an admin from deleting themselves
        if (actorUserId == userIdToDelete) {
            System.err.println("Attempt by user " + actorUserId + " to delete their own account blocked.");
            return false;
        }

        String sql = "DELETE FROM public.\"user\" WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userIdToDelete);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error deleting user.", e);
        }
    }



    @Override
    public UserProfile getMyProfile(int userId) throws RemoteException {
        UserProfile userProfile = new UserProfile();

        // Query 1: Get User, Department, Job Title, and Emp Type in one go using JOINs
        String profileSql = "SELECT u.f_name, u.l_name, u.username, u.email, u.phone, " +
                "d.name AS department_name, j.title AS job_title, j.level AS job_level, et.name AS emp_type_name " +
                "FROM public.\"user\" u " +
                "LEFT JOIN public.job_titles j ON u.job_title_id = j.id " +
                "LEFT JOIN public.departments d ON j.dept_id = d.id " +
                "LEFT JOIN public.emp_types et ON u.emp_type_id = et.id " +
                "WHERE u.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psProfile = conn.prepareStatement(profileSql)) {

            psProfile.setInt(1, userId);
            ResultSet rs = psProfile.executeQuery();

            if (rs.next()) {
                userProfile.setFirstName(rs.getString("f_name"));
                userProfile.setLastName(rs.getString("l_name"));
                userProfile.setUsername(rs.getString("username"));
                userProfile.setEmail(rs.getString("email"));
                userProfile.setPhone(rs.getString("phone"));
                userProfile.setDepartmentName(rs.getString("department_name"));
                userProfile.setJobTitle(rs.getString("job_title") + " (" + rs.getString("job_level") + ")");
                userProfile.setEmploymentType(rs.getString("emp_type_name"));
            } else {
                // User not found, return an empty profile or throw an exception
                return null;
            }

            // Query 2: Get Bank Details (using your existing method logic)
            userProfile.setBankDetails(this.getMyBankDetails(userId));

            // Query 3: Get Payslip History (you'll need a method for this)
            // For now, let's assume getMyPayslipHistory(userId) exists and fetches the list
            // userProfile.setPayslipHistory(this.getMyPayslipHistory(userId));
            // We can implement getMyPayslipHistory later. Let's leave it null for now.

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Server database error while fetching profile.", e);
        }

        return userProfile;
    }

    @Override
    public Payslip getMyLatestPayslip(int userId) throws RemoteException {
        String latestPayslipIdSql = "SELECT id FROM payslip WHERE user_id = ? ORDER BY pay_period_end_date DESC LIMIT 1";
        int payslipId = -1;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(latestPayslipIdSql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                payslipId = rs.getInt("id");
            } else {
                return null; // No payslips found for this user
            }
        } catch (SQLException e) {
            throw new RemoteException("Error finding latest payslip.", e);
        }

        // Now call the helper to get the full details
        try {
            return fetchFullPayslipDetails(payslipId);
        } catch (SQLException e) {
            throw new RemoteException("Error fetching latest payslip details.", e);
        }
    }

    @Override
    public List<String> getExistingPayrollPeriods(int actorUserId) throws RemoteException {
        // Add security check for HR role here
        List<String> periods = new ArrayList<>();
        // This query finds all unique YYYY-MM periods from the payslip table
        String sql = "SELECT DISTINCT TO_CHAR(pay_period_start_date, 'YYYY-MM') AS period " +
                "FROM public.payslip ORDER BY period DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                periods.add(rs.getString("period"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error fetching existing payroll periods.", e);
        }
        return periods;
    }

    // Emp
    @Override
    public UserBankDetails getMyBankDetails(int userId) throws RemoteException {
        UserBankDetails details = null;
        String sql = "SELECT user_id, bank_name, acc_no, acc_name FROM user_bank_details WHERE user_id = ?";
        try(Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                details = new UserBankDetails();
                details.setUserId(rs.getInt("user_id"));
                details.setBankName(rs.getString("bank_name"));
                details.setAccountNumber(rs.getString("acc_no"));
                details.setAccountHolderName(rs.getString("acc_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error fetching bank details", e);
        }
        return details;
    }

    @Override
    public boolean updateMyBankDetails(int userId, UserBankDetails details) throws RemoteException {
        String sql = "INSERT INTO user_bank_details (user_id, bank_name, acc_no, acc_name) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (user_id) DO UPDATE SET " +
                "bank_name = EXCLUDED.bank_name, acc_no = EXCLUDED.acc_no, acc_name = EXCLUDED.acc_name";
        try(Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, details.getBankName());
            ps.setString(3, details.getAccountNumber());
            ps.setString(4, details.getAccountHolderName());
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error updating bank details", e);
        }
    }


    // =================================================================
    //  HR Administrator Methods
    // =================================================================

    @Override
    public String createUser(int actorUserId, User newUser, String password) throws RemoteException {
        // Step 1: Security Check - Ensure the person creating the user has the HR role.
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: User with ID " + actorUserId + " does not have HR privileges.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        // Step 2: Prepare the data for insertion
        String hashedPassword = hashPassword(password); // Reuse our SHA-256 hashing method

        String sql = "INSERT INTO public.\"user\" (job_title_id, emp_type_id, username, f_name, l_name, email, phone, ic, pwd_hash, status, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 'EMPLOYEE')"; // New users default to EMPLOYEE role

        System.out.println("\n--- DEBUG: Attempting to create user with the following data ---");
        System.out.println("Actor (HR) User ID: " + actorUserId);
        System.out.println("Job Title ID: " + newUser.getJobTitleId());
        System.out.println("Emp Type ID: " + newUser.getEmpTypeId());
        System.out.println("Username: '" + newUser.getUsername() + "'");
        System.out.println("First Name: '" + newUser.getFName() + "'");
        System.out.println("Last Name: '" + newUser.getLName() + "'");
        System.out.println("Email: '" + newUser.getEmail() + "'");
        System.out.println("Phone: '" + newUser.getPhone() + "'");
        System.out.println("IC: '" + newUser.getIc() + "'");
        System.out.println("Hashed Password: '" + hashedPassword + "'");
        System.out.println("----------------------------------------------------------------");

        // Step 3: Execute the database insert
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newUser.getJobTitleId());
            ps.setInt(2, newUser.getEmpTypeId());
            ps.setString(3, newUser.getUsername());
            ps.setString(4, newUser.getFName());
            ps.setString(5, newUser.getLName());
            ps.setString(6, newUser.getEmail());
            ps.setString(7, newUser.getPhone());
            ps.setString(8, newUser.getIc());
            ps.setString(9, hashedPassword);

            System.out.println("Executing INSERT statement...");
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " row(s) affected.");

            if (rowsAffected > 0) {
                return "Successfully created new employee: " + newUser.getUsername();
            } else {
                return "Failed to create new employee.";
            }

        } catch (SQLException e) {
            // Handle specific errors, like a duplicate username
            System.err.println("\n--- DEBUG: SQLException caught! ---");
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            System.err.println("-------------------------------------\n");

            if (e.getSQLState().equals("23505")) { // 'unique_violation' error code
                return "Error: Username '" + newUser.getUsername() + "' or Email/IC already exists.";
            }
            e.printStackTrace();
            throw new RemoteException("An error occurred during user creation.", e);
        }
    }

    @Override
    public List<User> readAllUsers(int actorUserId) throws RemoteException {
        // Step 1: Security Check
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: User with ID " + actorUserId + " does not have HR privileges.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        List<User> userList = new ArrayList<>();
        // Step 2: Efficiently query users with their job and department info
        String sql = "SELECT u.id, u.username, u.f_name, u.l_name, u.email, u.role, " +
                "j.title, j.level, d.name AS department_name " +
                "FROM public.\"user\" u " +
                "LEFT JOIN public.job_titles j ON u.job_title_id = j.id " +
                "LEFT JOIN public.departments d ON j.dept_id = d.id " +
                "ORDER BY u.id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFName(rs.getString("f_name"));
                user.setLName(rs.getString("l_name"));
                user.setEmail(rs.getString("email"));
                user.setRole(Role.valueOf(rs.getString("role")));

                // Populate the new fields for display
                user.setDepartmentName(rs.getString("department_name"));
                user.setJobTitle(rs.getString("title") + " (" + rs.getString("level") + ")");

                userList.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred while fetching the user list.", e);
        }

        return userList;
    }

    @Override
    public User readUserById(int userId, int targetUserId) throws RemoteException {
        try {
            if (!checkUserRole(userId, Role.HR)) {
                throw new SecurityException("Access Denied: User with ID " + userId + " does not have HR privileges.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        // Add security check for HR role here
        User user = null;
        // This query is similar to the one in readAllUsers to get all necessary details
        String sql = "SELECT u.*, j.title, j.level, d.name AS department_name " +
                "FROM public.\"user\" u " +
                "LEFT JOIN public.job_titles j ON u.job_title_id = j.id " +
                "LEFT JOIN public.departments d ON j.dept_id = d.id " +
                "WHERE u.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, targetUserId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFName(rs.getString("f_name"));
                user.setLName(rs.getString("l_name"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setIc(rs.getString("ic"));
                user.setJobTitleId(rs.getInt("job_title_id"));
                user.setEmpTypeId(rs.getInt("emp_type_id"));
                user.setRole(Role.valueOf(rs.getString("role")));
                // Set display fields
                user.setDepartmentName(rs.getString("department_name"));
                user.setJobTitle(rs.getString("title") + " (" + rs.getString("level") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error fetching user details for ID: " + targetUserId, e);
        }
        return user;
    }

    @Override
    public boolean updateUser(int userId, User user) throws RemoteException {
        // Add security check for HR role here
        String sql = "UPDATE public.\"user\" SET f_name = ?, l_name = ?, email = ?, phone = ?, " +
                "ic = ?, job_title_id = ?, emp_type_id = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFName());
            ps.setString(2, user.getLName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getIc());
            // Handle potential nulls for IDs
            ps.setObject(6, user.getJobTitleId() == 0 ? null : user.getJobTitleId());
            ps.setObject(7, user.getEmpTypeId() == 0 ? null : user.getEmpTypeId());
            ps.setInt(8, user.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RemoteException("Error updating user", e);
        }
    }

    private boolean checkUserRole(int userId, Role requiredRole) throws SQLException {
        String sql = "SELECT role FROM public.\"user\" WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Role userRole = Role.valueOf(rs.getString("role").toUpperCase());
                return userRole == requiredRole;
            }
        }
        return false; // User not found
    }

    @Override
    public List<JobTitle> getAllJobTitles(int userId) throws RemoteException {
        List<JobTitle> jobTitles = new ArrayList<>();
        String sql = "SELECT id, dept_id, title, level, description FROM public.job_titles ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JobTitle job = new JobTitle();
                job.setId(rs.getInt("id"));
                job.setDeptId(rs.getInt("dept_id"));
                job.setTitle(rs.getString("title"));
                job.setLevel(rs.getString("level")); // This was the missing part
                job.setDescription(rs.getString("description"));
                jobTitles.add(job);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching employment types", e);
        }
        return jobTitles;
    }

    @Override
    public List<EmpType> getAllEmpTypes(int userId) throws RemoteException {
        List<EmpType> empTypes = new ArrayList<>();
        String sql = "SELECT id, name FROM public.emp_types ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                EmpType type = new EmpType();
                type.setId(rs.getInt("id"));
                type.setName(rs.getString("name"));
                empTypes.add(type);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching employment types", e);
        }
        return empTypes;
    }

    @Override
    public List<PayTemplate> getPayTemplatesForJobTitle(int actorUserId, int jobTitleId) throws RemoteException {
        // Add security check for HR role here
        List<PayTemplate> templates = new ArrayList<>();
        String sql = "SELECT id, job_title_id, description, type, amount FROM public.pay_templates WHERE job_title_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobTitleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PayTemplate item = new PayTemplate();
                item.setId(rs.getInt("id"));
                item.setJobTitleId(rs.getInt("job_title_id"));
                item.setDescription(rs.getString("description"));
                item.setType(PayItemType.valueOf(rs.getString("type")));
                item.setAmount(rs.getBigDecimal("amount"));
                templates.add(item);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching pay templates", e);
        }
        return templates;
    }

    @Override
    public boolean updatePayTemplateItem(int actorUserId, int payTemplateItemId, BigDecimal newAmount) throws RemoteException {
        // Add security check for HR role here
        String sql = "UPDATE public.pay_templates SET amount = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newAmount);
            ps.setInt(2, payTemplateItemId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RemoteException("Error updating pay template item", e);
        }
    }

    @Override
    public PayTemplate addPayTemplateItem(int actorUserId, PayTemplate newItem) throws RemoteException {
        // Add security check for HR role here
        String sql = "INSERT INTO public.pay_templates (job_title_id, emp_type_id, description, type, amount) " +
                "VALUES (?, ?, ?, CAST(? AS pay_item_type_enum), ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newItem.getJobTitleId());
            ps.setObject(2, newItem.getEmpTypeId() == 0 ? null : newItem.getEmpTypeId());
            ps.setString(3, newItem.getDescription());
            ps.setString(4, newItem.getType().name());
            ps.setBigDecimal(5, newItem.getAmount());

            ResultSet rs = ps.executeQuery(); // Use executeQuery for RETURNING
            if (rs.next()) {
                newItem.setId(rs.getInt(1)); // Set the new ID on the object
                return newItem;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error adding pay template item", e);
        }
        return null;
    }

    @Override
    public boolean deletePayTemplateItem(int userId, int payTemplateItemId) throws RemoteException {
        // Add security check for HR role here
        String sql = "DELETE FROM public.pay_templates WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payTemplateItemId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error deleting pay template item", e);
        }
    }


    private void processPayrollForEmployee(User employee, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<PayItem> finalPayItems = new ArrayList<>();
        BigDecimal grossPay = BigDecimal.ZERO;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // START TRANSACTION
            try {
                // Step A: Gather all RULES from pay_templates
                Map<String, PayTemplate> rules = new HashMap<>();
                String rulesSql = "SELECT * FROM pay_templates WHERE (job_title_id IS NULL AND emp_type_id IS NULL) OR (job_title_id IS NULL AND emp_type_id = ?) OR (job_title_id = ?)";
                try (PreparedStatement ps = conn.prepareStatement(rulesSql)) {
                    ps.setInt(1, employee.getEmpTypeId());
                    ps.setInt(2, employee.getJobTitleId());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        PayTemplate item = new PayTemplate();
                        item.setDescription(rs.getString("description"));
                        item.setType(PayItemType.valueOf(rs.getString("type")));
                        item.setAmount(rs.getBigDecimal("amount"));
                        rules.put(item.getDescription(), item);
                    }
                }

                List<PayItem> variableItems = new ArrayList<>();
                String bonusSql = "SELECT name, amount, type FROM bonuses WHERE user_id = ? AND pay_period_start_date = ? AND is_approved = true";
                try (PreparedStatement ps = conn.prepareStatement(bonusSql)) {
                    ps.setInt(1, employee.getId());
                    ps.setDate(2, java.sql.Date.valueOf(startDate));
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        PayItem bonusItem = new PayItem();
                        bonusItem.setName(rs.getString("name"));
                        bonusItem.setAmount(rs.getBigDecimal("amount"));
                        bonusItem.setType(PayItemType.valueOf(rs.getString("type")));
                        variableItems.add(bonusItem); // Add found bonuses to a temporary list
                    }
                }

                // --- Step C: Calculate Gross Pay & build the final pay item list ---
                // First, add all standard earnings from the templates
                for (PayTemplate rule : rules.values()) {
                    if (rule.getType() == PayItemType.EARNING && !rule.getDescription().contains("Leave Entitlement")) {
                        grossPay = grossPay.add(rule.getAmount());
                        PayItem earningItem = new PayItem();
                        earningItem.setName(rule.getDescription());
                        earningItem.setType(PayItemType.EARNING);
                        earningItem.setAmount(rule.getAmount());
                        finalPayItems.add(earningItem);
                    }
                }
                // NEXT, add all variable earnings (like bonuses) to the gross pay and the final list
                for (PayItem varItem : variableItems) {
                    if (varItem.getType() == PayItemType.EARNING) {
                        grossPay = grossPay.add(varItem.getAmount());
                        finalPayItems.add(varItem);
                    }
                }

                // Step C: Calculate Deductions (including percentages)
                BigDecimal totalDeductions = BigDecimal.ZERO;
                for (PayTemplate rule : rules.values()) {
                    if (rule.getType() == PayItemType.DEDUCTION) {
                        BigDecimal deductionAmount;
                        if (rule.getDescription().contains("EPF") || rule.getDescription().contains("EIS")) {
                            deductionAmount = grossPay.multiply(rule.getAmount());
                        } else {
                            deductionAmount = rule.getAmount();
                        }
                        totalDeductions = totalDeductions.add(deductionAmount);
                        PayItem deductionItem = new PayItem();
                        deductionItem.setName(rule.getDescription());
                        deductionItem.setType(PayItemType.DEDUCTION);
                        deductionItem.setAmount(deductionAmount.setScale(2, RoundingMode.HALF_UP));
                        finalPayItems.add(deductionItem);
                    }
                }
                BigDecimal netPay = grossPay.subtract(totalDeductions);

                // Step D: Save the final historical record
                String payslipSql = "INSERT INTO payslip (user_id, pay_period_start_date, pay_period_end_date, gross_earnings, total_deductions, net_pay) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
                int newPayslipId;
                try (PreparedStatement ps = conn.prepareStatement(payslipSql)) {
                    ps.setInt(1, employee.getId());
                    ps.setDate(2, java.sql.Date.valueOf(startDate));
                    ps.setDate(3, java.sql.Date.valueOf(endDate));
                    ps.setBigDecimal(4, grossPay.setScale(2, RoundingMode.HALF_UP));
                    ps.setBigDecimal(5, totalDeductions.setScale(2, RoundingMode.HALF_UP));
                    ps.setBigDecimal(6, netPay.setScale(2, RoundingMode.HALF_UP));
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    newPayslipId = rs.getInt(1);
                }

                String itemsSql = "INSERT INTO pay_items (payslip_id, name, type, amount) VALUES (?, ?, CAST(? AS pay_item_type_enum), ?)";
                try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                    for (PayItem item : finalPayItems) {
                        ps.setInt(1, newPayslipId);
                        ps.setString(2, item.getName());
                        ps.setString(3, item.getType().name());
                        ps.setBigDecimal(4, item.getAmount());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit(); // COMMIT TRANSACTION
            } catch (Exception e) {
                conn.rollback(); // ROLLBACK TRANSACTION ON ERROR
                throw new SQLException(e);
            }
        }
    }

    @Override
    public List<Department> getAllDepartments(int actorUserId) throws RemoteException {
        // Add security check for HR role here
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT id, name FROM public.departments ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Department dept = new Department();
                dept.setDeptId(rs.getInt("id"));
                dept.setDeptName(rs.getString("name"));
                departments.add(dept);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching departments.", e);
        }
        return departments;
    }

    @Override
    public Department createDepartment(int actorUserId, Department newDepartment) throws RemoteException {
        // Add security check for HR role here
        String sql = "INSERT INTO public.departments (name) VALUES (?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newDepartment.getDeptName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                newDepartment.setDeptId(rs.getInt(1));
                return newDepartment;
            }
        } catch (SQLException e) {
            // Handle unique constraint violation (duplicate name)
            if (e.getSQLState().equals("23505")) {
                throw new RemoteException("A department with that name already exists.");
            }
            throw new RemoteException("Error creating department.", e);
        }
        return null;
    }

    @Override
    public JobTitle createJobTitle(int actorUserId, JobTitle newJobTitle) throws RemoteException {
        // Add security check for HR role here
        String sql = "INSERT INTO public.job_titles (dept_id, title, level) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newJobTitle.getDeptId());
            ps.setString(2, newJobTitle.getTitle());
            ps.setString(3, newJobTitle.getLevel());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                newJobTitle.setId(rs.getInt(1));
                return newJobTitle;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new RemoteException("A job title with that name and level already exists in that department.");
            }
            throw new RemoteException("Error creating job title.", e);
        }
        return null;
    }



    @Override
    public List<PayslipSummary> getAllPayslips(int actorUserId) throws RemoteException {
        // Add security check for HR role here
        List<PayslipSummary> summaries = new ArrayList<>();
        String sql = "SELECT p.id, p.user_id, u.f_name, u.l_name, p.pay_period_start_date, p.net_pay " +
                "FROM public.payslip p " +
                "JOIN public.\"user\" u ON p.user_id = u.id " +
                "ORDER BY p.pay_period_start_date DESC, u.l_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PayslipSummary summary = new PayslipSummary();
                summary.setPayslipId(rs.getInt("id"));
                summary.setUserId(rs.getInt("user_id"));
                summary.setUserFullName(rs.getString("f_name") + " " + rs.getString("l_name"));
                summary.setPayPeriodStartDate(rs.getDate("pay_period_start_date").toLocalDate());
                summary.setNetPay(rs.getBigDecimal("net_pay"));
                summaries.add(summary);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching payslip summaries", e);
        }
        return summaries;
    }


    @Override
    public String runMonthlyPayrollForTarget(int actorUserId, int year, int month, int targetUserId) throws RemoteException {
        // 1. Security Check for HR role
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: You do not have HR privileges to run payroll.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        // 2. Setup pay period and fetch eligible employees
        LocalDate payPeriodStart = LocalDate.of(year, month, 1);
        List<User> employeesToPay = new ArrayList<>();

        // Start with the base SQL query
        StringBuilder fetchUsersSql = new StringBuilder(
                "SELECT id, username, f_name, l_name, job_title_id, emp_type_id FROM public.\"user\" WHERE status = 'ACTIVE'"
        );

        // SECURELY add the condition if we are targeting a single user
        if (targetUserId != 0) {
            fetchUsersSql.append(" AND id = ?");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(fetchUsersSql.toString())) {

            // Set the parameter ONLY if we're fetching a single user
            if (targetUserId != 0) {
                ps.setInt(1, targetUserId);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setJobTitleId(rs.getInt("job_title_id"));
                u.setEmpTypeId(rs.getInt("emp_type_id"));
                employeesToPay.add(u);
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not fetch list of employees.", e);
        }

        // 3. Loop through the list (which is either ALL users or just ONE user) and process payroll
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (User employee : employeesToPay) {
            try (Connection conn = DatabaseManager.getConnection()) {
                // Check if a payslip already exists for this user and period to prevent duplicates
                String checkSql = "SELECT id FROM payslip WHERE user_id = ? AND pay_period_start_date = ?";
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setInt(1, employee.getId());
                checkPs.setDate(2, java.sql.Date.valueOf(payPeriodStart));
                if (checkPs.executeQuery().next()) {
                    System.out.printf("Skipping %s: Payslip for this period already exists.\n", employee.getUsername());
                    skippedCount++;
                    continue; // Move to the next employee
                }

                // If it doesn't exist, process it using our existing helper method
                processPayrollForEmployee(employee, payPeriodStart, payPeriodStart.withDayOfMonth(payPeriodStart.lengthOfMonth()));
                System.out.printf("Successfully processed payroll for: %s\n", employee.getUsername());
                successCount++;
            } catch (Exception e) {
                System.err.printf("!!! FAILED to process payroll for %s: %s\n", employee.getUsername(), e.getMessage());
                failureCount++;
            }
        }

        // 4. Return a final summary
        String target = targetUserId == 0 ? "all employees" : "user ID " + targetUserId;
        String summary = String.format("Payroll run for %d-%02d for %s complete. Processed: %d, Skipped: %d, Failed: %d.",
                year, month, target, successCount, skippedCount, failureCount);
        System.out.println(summary);
        return summary;
    }

    @Override
    public List<Bonus> getAllPendingBonuses(int actorUserId) throws RemoteException {
        // Security Check for HR Role
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: You do not have HR privileges.");
            }
        } catch (SQLException e) { throw new RemoteException("Could not verify user permissions.", e); }

        List<Bonus> pendingList = new ArrayList<>();
        String sql = "SELECT b.id, b.user_id, u.f_name, u.l_name, b.pay_period_start_date, b.name, b.amount " +
                "FROM public.bonuses b JOIN public.\"user\" u ON b.user_id = u.id " +
                "WHERE b.is_approved = false ORDER BY b.pay_period_start_date, u.l_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Bonus bonus = new Bonus();
                bonus.setId(rs.getInt("id"));
                bonus.setEmployeeName(rs.getString("f_name") + " " + rs.getString("l_name"));
                bonus.setPayPeriodStartDate(rs.getDate("pay_period_start_date").toLocalDate());
                bonus.setName(rs.getString("name"));
                bonus.setAmount(rs.getBigDecimal("amount"));
                pendingList.add(bonus);
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching pending bonuses.", e);
        }
        return pendingList;
    }

    // =================================================================
    //  MANAGER Administrator Methods
    // =================================================================
    @Override
    public Bonus createBonusesToEmployee(int actorUserId, Bonus newBonus) throws RemoteException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Security Check 1: Ensure the user is a Manager
            if (!checkUserRole(actorUserId, Role.MANAGER)) {
                throw new SecurityException("Access Denied: You do not have Manager privileges.");
            }

            // Security Check 2: Ensure the target employee is in the manager's department
            if (!isUserInManagerDept(conn, actorUserId, newBonus.getUserId())) {
                throw new SecurityException("Access Denied: You can only assign bonuses to employees in your own department.");
            }

            // If security checks pass, insert the bonus
            String sql = "INSERT INTO public.bonuses (user_id, pay_period_start_date, name, type, amount, is_approved, approved_by_id) " +
                    "VALUES (?, ?, ?, CAST(? AS pay_item_type_enum), ?, ?, ?) RETURNING id";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newBonus.getUserId());
                ps.setDate(2, java.sql.Date.valueOf(newBonus.getPayPeriodStartDate()));
                ps.setString(3, newBonus.getName());
                ps.setString(4, "EARNING"); // Bonuses are always an EARNING
                ps.setBigDecimal(5, newBonus.getAmount());
                ps.setBoolean(6, true); // Automatically approved by the assigning manager
                ps.setInt(7, actorUserId);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    newBonus.setId(rs.getInt(1));
                    return newBonus;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error while assigning bonus.", e);
        }
        return null;
    }

    // Add this new private helper for the security check
    private boolean isUserInManagerDept(Connection conn, int managerId, int employeeId) throws SQLException {
        int managerDept = getDepartmentIdForUser(conn, managerId);
        int employeeDept = getDepartmentIdForUser(conn, employeeId);
        return managerDept != -1 && managerDept == employeeDept;
    }

    @Override
    public List<User> getMyDepartmentEmployees(int actorUserId) throws RemoteException {
        // 1. Security Check: Ensure the user is a Manager
        try {
            if (!checkUserRole(actorUserId, Role.MANAGER)) {
                throw new SecurityException("Access Denied: You do not have Manager privileges.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        List<User> departmentEmployees = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection()) {
            // 2. First, find out which department the manager (actorUserId) belongs to.
            int departmentId = -1;
            String findDeptSql = "SELECT j.dept_id FROM public.\"user\" u " +
                    "JOIN public.job_titles j ON u.job_title_id = j.id WHERE u.id = ?";

            try (PreparedStatement ps = conn.prepareStatement(findDeptSql)) {
                ps.setInt(1, actorUserId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    departmentId = rs.getInt("dept_id");
                } else {
                    throw new RemoteException("Could not find department for manager ID: " + actorUserId);
                }
            }

            // 3. Now, fetch all employees who belong to that departmentId.
            String fetchEmployeesSql = "SELECT u.id, u.username, u.f_name, u.l_name, j.title, j.level " +
                    "FROM public.\"user\" u " +
                    "JOIN public.job_titles j ON u.job_title_id = j.id " +
                    "WHERE j.dept_id = ? ORDER BY u.id ASC";

            try (PreparedStatement ps = conn.prepareStatement(fetchEmployeesSql)) {
                ps.setInt(1, departmentId);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFName(rs.getString("f_name"));
                    user.setLName(rs.getString("l_name"));
                    user.setJobTitle(rs.getString("title") + " (" + rs.getString("level") + ")");
                    departmentEmployees.add(user);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred while fetching the department report.", e);
        }

        return departmentEmployees;
    }

    @Override
    public PayrollSummaryReport getPayrollSummaryReport(int actorUserId, int year, int month) throws RemoteException {
        try {
            if (!checkUserRole(actorUserId, Role.HR)) {
                throw new SecurityException("Access Denied: You do not have HR privileges.");
            }
        } catch (SQLException e) {
            throw new RemoteException("Could not verify user permissions.", e);
        }

        PayrollSummaryReport report = new PayrollSummaryReport();
        report.setPayPeriod(LocalDate.of(year, month, 1));

        String summarySql = "SELECT COUNT(id) AS employees_paid, SUM(gross_earnings) AS total_gross, " +
                "SUM(total_deductions) AS total_deduct, SUM(net_pay) AS total_net " +
                "FROM public.payslip " +
                "WHERE date_part('year', pay_period_start_date) = ? AND date_part('month', pay_period_start_date) = ?";

        String breakdownSql = "SELECT d.name AS department_name, SUM(p.net_pay) AS department_net_pay " +
                "FROM public.payslip p " +
                "JOIN public.\"user\" u ON p.user_id = u.id " +
                "JOIN public.job_titles j ON u.job_title_id = j.id " +
                "JOIN public.departments d ON j.dept_id = d.id " +
                "WHERE date_part('year', p.pay_period_start_date) = ? AND date_part('month', p.pay_period_start_date) = ? " +
                "GROUP BY d.name ORDER BY d.name";

        try (Connection conn = DatabaseManager.getConnection()) {
            // Execute first query for overall totals
            try (PreparedStatement ps = conn.prepareStatement(summarySql)) {
                ps.setInt(1, year);
                ps.setInt(2, month);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    report.setNumberOfEmployeesPaid(rs.getInt("employees_paid"));
                    report.setTotalGrossEarnings(rs.getBigDecimal("total_gross"));
                    report.setTotalDeductions(rs.getBigDecimal("total_deduct"));
                    report.setTotalNetPay(rs.getBigDecimal("total_net"));

                    BigDecimal totalGross = report.getTotalGrossEarnings();
                    if (totalGross != null) {
                        // Estimate employer EPF contribution at 13%
                        BigDecimal estimatedContributions = totalGross.multiply(new BigDecimal("0.13"));
                        report.setEstimatedEmployerContributions(estimatedContributions);
                        // Total Payout = Gross Pay + Employer Contributions
                        report.setTotalCompanyPayout(totalGross.add(estimatedContributions));
                    }
                }
            }

            // Execute second query for department breakdown
            Map<String, BigDecimal> breakdownMap = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(breakdownSql)) {
                ps.setInt(1, year);
                ps.setInt(2, month);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    breakdownMap.put(rs.getString("department_name"), rs.getBigDecimal("department_net_pay"));
                }
            }
            report.setNetPayByDepartment(breakdownMap);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Error generating payroll summary report.", e);
        }

        return report;
    }

    @Override
    public List<Bonus> getUnapprovedBonusesForMyDepartment(int actorUserId) throws RemoteException {
        // Security Check for Manager Role
        try {
            if (!checkUserRole(actorUserId, Role.MANAGER)) {
                throw new SecurityException("Access Denied: You do not have Manager privileges.");
            }
        } catch (SQLException e) { throw new RemoteException("Could not verify user permissions.", e); }

        List<Bonus> bonusList = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            // First, find the manager's department ID
            int departmentId = getDepartmentIdForUser(conn, actorUserId);

            // Now, find all unapproved bonuses for users in that department
            String sql = "SELECT b.id, b.user_id, u.f_name, u.l_name, b.pay_period_start_date, b.name, b.type, b.amount " +
                    "FROM public.bonuses b " +
                    "JOIN public.\"user\" u ON b.user_id = u.id " +
                    "JOIN public.job_titles j ON u.job_title_id = j.id " +
                    "WHERE j.dept_id = ? AND b.is_approved = false";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, departmentId);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    Bonus bonus = new Bonus();
                    bonus.setId(rs.getInt("id"));
                    bonus.setUserId(rs.getInt("user_id"));
                    bonus.setEmployeeName(rs.getString("f_name") + " " + rs.getString("l_name"));
                    bonus.setPayPeriodStartDate(rs.getDate("pay_period_start_date").toLocalDate());
                    bonus.setName(rs.getString("name"));
                    bonus.setType(PayItemType.valueOf(rs.getString("type")));
                    bonus.setAmount(rs.getBigDecimal("amount"));
                    bonusList.add(bonus);
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Error fetching unapproved bonuses.", e);
        }
        return bonusList;
    }

    @Override
    public boolean approveBonus(int actorUserId, int bonusId) throws RemoteException {
        // Security Check for Manager Role
        try {
            if (!checkUserRole(actorUserId, Role.MANAGER)) {
                throw new SecurityException("Access Denied: You do not have Manager privileges.");
            }
        } catch (SQLException e) { throw new RemoteException("Could not verify user permissions.", e); }

        // Security Check: Ensure manager is not approving bonuses outside their department (important!)
        // (This check would be added in a production system)

        String sql = "UPDATE public.bonuses SET is_approved = true, approved_by_id = ? WHERE id = ? AND is_approved = false";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actorUserId);
            ps.setInt(2, bonusId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RemoteException("Error approving bonus.", e);
        }
    }

    // It's good practice to refactor repeated logic into a helper method.
    // We can create a helper to get a user's department ID.
    private int getDepartmentIdForUser(Connection conn, int userId) throws SQLException {
        String findDeptSql = "SELECT j.dept_id FROM public.\"user\" u JOIN public.job_titles j ON u.job_title_id = j.id WHERE u.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(findDeptSql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("dept_id");
            }
        }
        return -1; // User not found or has no department
    }


}
