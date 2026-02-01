package com.wipro.payroll.server;

import com.wipro.payroll.server.DatabaseManager; // Import your manager
import java.sql.Connection;

/**
 * A simple class to test if the DatabaseManager can successfully
 * connect to the remote Neon database.
 */
public class TestDbConnection {

    public static void main(String[] args) {
        System.out.println("Attempting to connect to the database using DatabaseManager...");
        Connection conn = null;
        try {
            // 1. Get a connection from your manager class
            conn = DatabaseManager.getConnection();

            // 2. Check if the connection is valid
            if (conn != null && !conn.isClosed()) {
                System.out.println("--------------------------------------------------");
                System.out.println("✅ SUCCESS: Database connection established!");
                System.out.println("   Database: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("   Version: " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("--------------------------------------------------");
            } else {
                System.err.println("❌ FAILURE: Connection object was null or closed.");
            }

        } catch (Exception e) {
            System.err.println("❌ FAILURE: An exception occurred while trying to connect.");
            e.printStackTrace();
        } finally {
            // 3. Clean up the connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}