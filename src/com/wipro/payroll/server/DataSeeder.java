package com.wipro.payroll.server;

import org.mindrot.jbcrypt.BCrypt;
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

    // --- Configuration ---
    // IMPORTANT: Use the same credentials as your PayrollServiceImpl
    private static final String DB_URL = "jdbc:postgresql://ep-withered-flower-a811hc2n-pooler.eastus2.azure.neon.tech:5432/neondb?sslmode=require&channel_binding=require";
    private static final String DB_USER = "neondb_owner";
    private static final String DB_PASSWORD = "npg_4A1VduWMcaYT";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✅ Database connection successful.");

            // Run the seeding methods in order of dependency
            seedBasicTables(conn);
            emp_types(conn);
            job_titles(conn);
            pay_templates(conn);
            seedUsers(conn);
            seedUserRoles(conn);
            seedUserDetails(conn);
            seedVariableData(conn);

            System.out.println("\n✅✅✅ Database seeding complete! ✅✅✅");

        } catch (SQLException e) {
            System.err.println("❌ Database operation failed. Check your connection or SQL statements.");
            e.printStackTrace();
        }
    }

    private static void seedBasicTables(Connection conn) throws SQLException {
        System.out.println("Seeding basic lookup tables (departments, roles)...");
        // Using ON CONFLICT DO NOTHING is a safe way to prevent errors if the data already exists.
        executeSql(conn, "INSERT INTO \"departments\" (id, name, description) VALUES (1, 'IT', 'Information Technology'), (2, 'Logistics', 'Shipping and Warehouse'), (3, 'Human Resources', 'Manages company personnel') ON CONFLICT (id) DO NOTHING;");
        executeSql(conn, "INSERT INTO \"role\" (id, name) VALUES (1, 'EMPLOYEE'), (2, 'HR'), (3, 'MANAGER') ON CONFLICT (id) DO NOTHING;");
    }

    private static void emp_types(Connection conn) throws SQLException {
        System.out.println("Seeding job structure tables (emp_types)...");
        executeSql(conn, "INSERT INTO \"emp_types\" (id, name) VALUES (1, 'Full-Time'), (2, 'Part-Time'), (3, 'Contract') ON CONFLICT (id) DO NOTHING;");
    }

    private static void job_titles(Connection conn) throws SQLException {
        System.out.println("Seeding job structure tables (job_titles)...");
        executeSql(conn, "INSERT INTO \"job_titles\" (id, dept_id, title, level) VALUES (1, 1, 'Data Analyst', 'Junior'), (2, 1, 'Data Analyst', 'Senior'), (3, 3, 'HR Administrator', 'Senior'), (4, 2, 'Logistics Manager', 'Lead'), (5, 2, 'Warehouse Assistant', 'Junior') ON CONFLICT (id) DO NOTHING;");
    }

    private static void pay_templates(Connection conn) throws SQLException {
        System.out.println("Seeding job structure tables (pay_templates)...");
        executeSql(conn, "INSERT INTO \"pay_templates\" (job_title_id, description, type, amount) VALUES " +
                "(1, 'Base Salary', 'EARNING', 4500.00), " + // <-- ADD THIS LINE
                "(2, 'Base Salary', 'EARNING', 8000.00), " +
                "(2, 'Tech Allowance', 'EARNING', 500.00), " +
                "(3, 'Base Salary', 'EARNING', 7500.00), " +
                "(4, 'Base Salary', 'EARNING', 9000.00), " +
                "(4, 'Manager Allowance', 'EARNING', 1500.00), " +
                "(5, 'Hourly Rate', 'EARNING', 20.00) ON CONFLICT DO NOTHING;");
    }

    private static void seedUsers(Connection conn) throws SQLException {
        System.out.println("Seeding users (with hashed passwords)...");
        String sql = "INSERT INTO \"user\" (id, job_title_id, emp_type_id, username, f_name, l_name, email, phone, ic, pwd_hash, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS user_status_enum)) ON CONFLICT (id) DO NOTHING;";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Password for everyone is "password123"
            String hashedPassword = BCrypt.hashpw("123123123", BCrypt.gensalt(12));

            // User 1: Alice (Senior Data Analyst)
            stmt.setInt(1, 1); stmt.setInt(2, 2); stmt.setInt(3, 1); stmt.setString(4, "it"); stmt.setString(5, "Alice"); stmt.setString(6, "Tan"); stmt.setString(7, "it@gmail.com"); stmt.setString(8, "012-3456789"); stmt.setString(9, "900101-10-1234"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE");
            stmt.addBatch();

            // User 2: Bob (Logistics Manager)
            stmt.setInt(1, 2); stmt.setInt(2, 4); stmt.setInt(3, 1); stmt.setString(4, "mng"); stmt.setString(5, "Bob"); stmt.setString(6, "Lee"); stmt.setString(7, "mng@gmail.com"); stmt.setString(8, "013-4567890"); stmt.setString(9, "850202-14-2345"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE");
            stmt.addBatch();

            // User 3: Charlie (HR Admin)
            stmt.setInt(1, 3); stmt.setInt(2, 3); stmt.setInt(3, 1); stmt.setString(4, "hr"); stmt.setString(5, "Charlie"); stmt.setString(6, "Lim"); stmt.setString(7, "hr@gmail.com"); stmt.setString(8, "880303-01-3456"); stmt.setString(9, "880303-01-3456"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE");
            stmt.addBatch();

            // User 4: Pat (Part-timer)
            stmt.setInt(1, 4); stmt.setInt(2, 5); stmt.setInt(3, 2); stmt.setString(4, "log"); stmt.setString(5, "Pat"); stmt.setString(6, "Wong"); stmt.setString(7, "log@gmail.com"); stmt.setString(8, "016-6789012"); stmt.setString(9, "950404-10-4567"); stmt.setString(10, hashedPassword); stmt.setString(11, "ACTIVE");
            stmt.addBatch();

            stmt.executeBatch();
        }
    }

    private static void seedUserRoles(Connection conn) throws SQLException {
        System.out.println("Seeding user roles...");
        executeSql(conn, "INSERT INTO \"user_role\" (user_id, role_id) VALUES (1, 1), (2, 1), (2, 3), (3, 1), (3, 2), (4, 1) ON CONFLICT DO NOTHING;");
    }

    private static void seedUserDetails(Connection conn) throws SQLException {
        System.out.println("Seeding user details (bank, address)...");
        executeSql(conn, "INSERT INTO \"user_bank_details\" (user_id, bank_name, acc_no, acc_name) VALUES (1, 'Maybank', '114812345678', 'Alice Tan') ON CONFLICT (user_id) DO NOTHING;");
        executeSql(conn, "INSERT INTO \"user_address\" (user_id, street_line_1, city, state, postcode, country) VALUES (1, '123 Jalan Teknologi', 'Bukit Jalil', 'Kuala Lumpur', '57000', 'Malaysia') ON CONFLICT DO NOTHING;");
    }

    private static void seedVariableData(Connection conn) throws SQLException {
        System.out.println("Seeding variable data (bonuses, attendances)...");
        executeSql(conn, "INSERT INTO \"bonuses\" (user_id, pay_period_start_date, name, type, amount, is_approved, approved_by_id) VALUES (1, '2025-07-01', 'Q2 Performance Bonus', 'EARNING', 1000.00, true, 2) ON CONFLICT DO NOTHING;");

        String sql = "INSERT INTO \"attendances\" (user_id, clock_in, clock_out, is_approved, approved_by_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Pat worked 21 hours in July
            stmt.setInt(1, 4); stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2025, 7, 2, 9, 0))); stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2025, 7, 2, 17, 0))); stmt.setBoolean(4, true); stmt.setInt(5, 2);
            stmt.addBatch();
            stmt.setInt(1, 4); stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2025, 7, 3, 9, 0))); stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2025, 7, 3, 13, 0))); stmt.setBoolean(4, true); stmt.setInt(5, 2);
            stmt.addBatch();
            stmt.setInt(1, 4); stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2025, 7, 4, 9, 0))); stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2025, 7, 4, 18, 0))); stmt.setBoolean(4, true); stmt.setInt(5, 2);
            stmt.addBatch();
            stmt.executeBatch();
        }
    }

    // Helper method to execute simple SQL statements
    private static void executeSql(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
}