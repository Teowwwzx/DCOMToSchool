package com.wipro.payroll.server;

import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A utility class to seed the database with initial sample data from Java.
 * This is an alternative to running a .sql script and is useful for tasks
 * like password hashing that cannot be done in plain SQL.
 */
public class DataSeeder {

    public static void main(String[] args) {
        // MODIFIED: Use the connection pool from DatabaseManager
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("✅ Database connection successful.");

            // 1. Refresh the database to a clean state
            refreshDatabase(conn);

            // 2. Run the seeding methods in order of dependency
            seedLookupTables(conn); // Departments and Employment Types
            seedJobTitles(conn);
            seedPayTemplates(conn);
            seedUsersAndDetails(conn); // Users, Bank Details, and Variable Data
            seedBonuses(conn);
            seedAttendance(conn);
            syncIdSequence(conn, "user");

            System.out.println("\n✅✅✅ Database seeding complete! ✅✅✅");

        } catch (SQLException e) {
            System.err.println("❌ Database operation failed. Check your connection or SQL statements.");
            e.printStackTrace();
        }
    }

    private static void refreshDatabase(Connection conn) throws SQLException {
        System.out.println("⚠️  Refreshing database... ALL DATA WILL BE DELETED.");
        // TRUNCATE is fast. RESTART IDENTITY resets the auto-incrementing IDs. CASCADE wipes dependent tables.
        String sql = "TRUNCATE public.departments, public.emp_types, public.job_titles, public.\"user\" RESTART IDENTITY CASCADE;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
            System.out.println("✅ Database tables wiped and ID counters reset.");
        }
    }

    private static void seedLookupTables(Connection conn) throws SQLException {
        System.out.println("Seeding lookup tables (departments, emp_types)...");
        // MODIFIED: Departments updated for an IT company
        executeSql(conn, "INSERT INTO \"departments\" (id, name) VALUES (1, 'Engineering'), (2, 'Product'), (3, 'Human Resources') ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;");
        executeSql(conn, "INSERT INTO \"emp_types\" (id, name) VALUES (1, 'Full-Time'), (2, 'Contract') ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;");
    }

    private static void seedJobTitles(Connection conn) throws SQLException {
        System.out.println("Seeding job titles...");
        // MODIFIED: Job titles updated for an IT company
        executeSql(conn, "INSERT INTO \"job_titles\" (id, dept_id, title, level) VALUES (1, 1, 'Engineering Manager', 'Lead'), (2, 3, 'HR Business Partner', 'Senior'), (3, 1, 'Software Engineer', 'Senior'), (4, 1, 'QA Engineer', 'Junior') ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title;");
    }

    private static void seedPayTemplates(Connection conn) throws SQLException {
        System.out.println("Seeding realistic, hierarchical compensation templates for Malaysia...");

        executeSql(conn, "TRUNCATE public.pay_templates RESTART IDENTITY CASCADE;");

        String sql = "INSERT INTO public.pay_templates (job_title_id, emp_type_id, description, type, amount) VALUES (?, ?, ?, CAST(? AS pay_item_type_enum), ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // --- Level 1: Global Default Rules (apply to all employees in Malaysia) ---
            ps.setObject(1, null); ps.setObject(2, null); ps.setString(3, "EPF Employee (11%)"); ps.setString(4, "DEDUCTION"); ps.setBigDecimal(5, new BigDecimal("0.11")); // Stored as percentage rate
            ps.addBatch();
            ps.setObject(1, null); ps.setObject(2, null); ps.setString(3, "SOCSO Contribution"); ps.setString(4, "DEDUCTION"); ps.setBigDecimal(5, new BigDecimal("24.75")); // Placeholder amount for mid-range salary
            ps.addBatch();
            ps.setObject(1, null); ps.setObject(2, null); ps.setString(3, "EIS Contribution (0.2%)"); ps.setString(4, "DEDUCTION"); ps.setBigDecimal(5, new BigDecimal("0.002")); // Stored as percentage rate
            ps.addBatch();

            // --- Level 2: Employment Type Rules ---
            // For Full-Time staff (emp_type_id = 1)
            ps.setObject(1, null); ps.setInt(2, 1); ps.setString(3, "Annual Leave Entitlement"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("14.00")); // 14 days
            ps.addBatch();
            // For Contract staff (emp_type_id = 2)
            ps.setObject(1, null); ps.setInt(2, 2); ps.setString(3, "Annual Leave Entitlement"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("0.00")); // 0 days
            ps.addBatch();

            // --- Level 3: Job Title Rules (with more realistic KL IT salaries) ---
            // For Engineering Manager (job_title_id = 1)
            ps.setInt(1, 1); ps.setObject(2, null); ps.setString(3, "Base Salary"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("16000.00"));
            ps.addBatch();
            ps.setInt(1, 1); ps.setObject(2, null); ps.setString(3, "Management Bonus"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("2500.00"));
            ps.addBatch();

            // For HR Business Partner (job_title_id = 2)
            ps.setInt(1, 2); ps.setObject(2, null); ps.setString(3, "Base Salary"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("9000.00"));
            ps.addBatch();

            // For Senior Software Engineer (job_title_id = 3)
            ps.setInt(1, 3); ps.setObject(2, null); ps.setString(3, "Base Salary"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("8500.00"));
            ps.addBatch();
            ps.setInt(1, 3); ps.setObject(2, null); ps.setString(3, "Tech Allowance"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("500.00"));
            ps.addBatch();

            // For Junior QA Engineer (job_title_id = 4)
            ps.setInt(1, 4); ps.setObject(2, null); ps.setString(3, "Base Salary"); ps.setString(4, "EARNING"); ps.setBigDecimal(5, new BigDecimal("4000.00"));
            ps.addBatch();

            ps.executeBatch();
        }
    }

    private static void seedUsersAndDetails(Connection conn) throws SQLException {
        System.out.println("Seeding users (with SHA-256 passwords) and their details...");

        String sql = "INSERT INTO \"user\" (id, job_title_id, emp_type_id, username, f_name, l_name, email, phone, ic, pwd_hash, status, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS user_status_enum), CAST(? AS role_enum))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // MODIFIED: Password for all users is "123123123"
            String hashedPassword = hashPassword("123123123");

            // User 1: The Manager
            stmt.setInt(1, 1); stmt.setInt(2, 1); stmt.setInt(3, 1); stmt.setString(4, "mng"); stmt.setString(5, "tan"); stmt.setString(6, "wh"); stmt.setString(7, "mng@gmail.com"); stmt.setString(8, "011-11111111"); stmt.setString(9, "030101-10-1111"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE"); stmt.setString(12, "MANAGER");
            stmt.addBatch();

            // User 2: The HR person
            stmt.setInt(1, 2); stmt.setInt(2, 2); stmt.setInt(3, 1); stmt.setString(4, "hr"); stmt.setString(5, "tan"); stmt.setString(6, "zx"); stmt.setString(7, "hr@gmail.com"); stmt.setString(8, "012-2222222"); stmt.setString(9, "030202-14-2222"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE"); stmt.setString(12, "HR");
            stmt.addBatch();

            // User 3: Employee 1 (Software Engineer)
            stmt.setInt(1, 3); stmt.setInt(2, 3); stmt.setInt(3, 1); stmt.setString(4, "emp1"); stmt.setString(5, "teow"); stmt.setString(6, "zx"); stmt.setString(7, "emp1@gmail.com"); stmt.setString(8, "013-3333333"); stmt.setString(9, "030303-01-3333"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE"); stmt.setString(12, "EMPLOYEE");
            stmt.addBatch();

            // User 4: Employee 2 (QA Engineer)
            stmt.setInt(1, 4); stmt.setInt(2, 4); stmt.setInt(3, 1); stmt.setString(4, "emp2"); stmt.setString(5, "siew"); stmt.setString(6, "lx"); stmt.setString(7, "emp2@gmail.com"); stmt.setString(8, "014-4444444"); stmt.setString(9, "020404-10-4444"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE"); stmt.setString(12, "EMPLOYEE");
            stmt.addBatch();

            stmt.executeBatch();
        }

        // Seed bank details for the new users
        executeSql(conn, "INSERT INTO \"user_bank_details\" (user_id, bank_name, acc_no, acc_name) VALUES (1, 'HSBC', '300123456789', 'Maria Chen'), (2, 'Maybank', '112233445566', 'Henry Tan'), (3, 'CIMB', '778899001122', 'Eddie Ng');");
    }

    // Helper method to execute simple SQL statements
    private static void executeSql(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * MODIFIED: Added SHA-256 hashing method to match login logic.
     */
    private static String hashPassword(String password) {
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

    private static void seedBonuses(Connection conn) throws SQLException {
        System.out.println("Seeding sample bonuses...");
        String sql = "INSERT INTO public.bonuses (user_id, pay_period_start_date, name, type, amount, is_approved, approved_by_id) " +
                "VALUES (?, ?, ?, CAST(? AS pay_item_type_enum), ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Example: emp1 (user 3) gets a performance bonus for July, approved by the manager (user 1)
            ps.setInt(1, 3);
            ps.setDate(2, java.sql.Date.valueOf("2025-07-01"));
            ps.setString(3, "Q2 High Performance Bonus");
            ps.setString(4, "EARNING");
            ps.setBigDecimal(5, new BigDecimal("1000.00"));
            ps.setBoolean(6, true);
            ps.setInt(7, 1); // Approved by 'mng'
            ps.addBatch();

            ps.executeBatch();
        }
    }

    private static void seedAttendance(Connection conn) throws SQLException {
        System.out.println("Seeding monthly attendance summaries...");
        String sql = "INSERT INTO public.attendances (user_id, pay_period_start_date, days_worked, unpaid_leave_days, overtime_hours) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Example: For July, emp1 (user 3) worked 21 days, took 1 unpaid day, and had 8 hours OT.
            ps.setInt(1, 3);
            ps.setDate(2, java.sql.Date.valueOf("2025-07-01"));
            ps.setInt(3, 21); // The total days worked in the month
            ps.setInt(4, 1);
            ps.setBigDecimal(5, new BigDecimal("8.00"));

            ps.addBatch();
            ps.executeBatch();
        }
    }

    private static void syncIdSequence(Connection conn, String tableName) throws SQLException {
        String sql = String.format(
                "SELECT setval(pg_get_serial_sequence('public.\"%s\"', 'id'), COALESCE((SELECT MAX(id) FROM public.\"%s\"), 1))",
                tableName, tableName
        );
        System.out.println("✅ Syncing ID sequence for table: " + tableName);

        // FIX: We must use executeQuery() for a SELECT statement, not executeUpdate().
        // We won't use our general 'executeSql' helper here since it's a different type of command.
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeQuery();
        }
    }


}