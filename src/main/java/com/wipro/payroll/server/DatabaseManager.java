package com.wipro.payroll.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    // This static block runs once when the class is first loaded.
    // It configures and initializes the connection pool.
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://ep-withered-flower-a811hc2n-pooler.eastus2.azure.neon.tech:5432/neondb?sslmode=require");
        config.setUsername("neondb_owner");
        config.setPassword("npg_4A1VduWMcaYT");

        // --- Pool Configuration ---
        // Maximum number of connections in the pool
        config.setMaximumPoolSize(10);
        // Minimum number of idle connections
        config.setMinimumIdle(5);
        // How long to wait for a connection before timing out
        config.setConnectionTimeout(30000); // 30 seconds

        // Create the pool
        dataSource = new HikariDataSource(config);
        System.out.println("✅ Database Connection Pool initialized.");
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private DatabaseManager() {}

    /**
     * Gets a connection from the pool.
     * @return A database connection.
     * @throws SQLException if a connection cannot be obtained.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
