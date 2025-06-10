package com.wipro.payroll.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;

import com.wipro.payroll.common.PayrollService;
import com.wipro.payroll.common.Employee;
import org.mindrot.jbcrypt.BCrypt;

public class PayrollServiceImpl extends UnicastRemoteObject implements PayrollService {

    private Connection dbConnection;

    public PayrollServiceImpl() throws RemoteException {
        super();
        // Establish database connection
        try {
            String dbUrl = "jdbc:postgresql://localhost:5432/postgres";
            String dbUser = "postgres"; // Your PostgreSQL username
            String dbPassword = "Tzx@0301000301!"; // The password you set during installation
            dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Successfully connected to the PostgreSQL database.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            // If DB connection fails, the server can't do its job, so we might exit or handle it gracefully.
            throw new RemoteException("com.wipro.payroll.server.Server could not connect to the database.", e);
        }
    }

    @Override
    public String registerEmployee(String firstName, String lastName, String icNumber, String username, String password) throws RemoteException {
        System.out.println("Received registration request for username: " + username);

        // 1. Check if the username already exists (CRITICAL REQUIREMENT)
        try (PreparedStatement checkUserStmt = dbConnection.prepareStatement("SELECT COUNT(*) FROM employees WHERE username = ?")) {
            checkUserStmt.setString(1, username);
            ResultSet rs = checkUserStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Registration failed: Username '" + username + "' already exists.");
                return "Error: Username already exists. Please choose a different one.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("com.wipro.payroll.server.Server error while checking username.", e);
        }

        // 2. Hash the password for secure storage
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // 3. Insert the new employee into the database
        String sql = "INSERT INTO employees (first_name, last_name, ic_passport_number, username, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertStmt = dbConnection.prepareStatement(sql)) {
            insertStmt.setString(1, firstName);
            insertStmt.setString(2, lastName);
            insertStmt.setString(3, icNumber);
            insertStmt.setString(4, username);
            insertStmt.setString(5, hashedPassword);

            int rowsAffected = insertStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Successfully registered user: " + username);
                return "Registration successful! Welcome, " + firstName + ".";
            } else {
                return "Error: Registration failed. Please try again.";
            }
        } catch (SQLException e) {
            // This could happen if the IC number is not unique, for example.
            e.printStackTrace();
            throw new RemoteException("com.wipro.payroll.server.Server error during registration.", e);
        }
    }

    @Override
    public Employee loginEmployee(String username, String password) throws RemoteException {
        System.out.println("[SERVER] Received login request for user: " + username);

        // SQL query to get the employee details and their hashed password
        String sql = "SELECT employee_id, username, first_name, last_name, ic_passport_number, password_hash FROM employees WHERE username = ?";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();D

            // Check if a user with that username was found
            if (rs.next()) {
                // User exists, now retrieve the hashed password from the database
                String storedHashedPassword = rs.getString("password_hash");

                // Use BCrypt to check if the provided password matches the stored hash
                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    // Password is correct! Create an Employee object to return to the client.
                    System.out.println("[SERVER] Login successful for: " + username);
                    Employee employee = new Employee();
                    employee.setEmployeeId(rs.getInt("employee_id"));
                    employee.setUsername(rs.getString("username"));
                    employee.setFirstName(rs.getString("first_name"));
                    employee.setLastName(rs.getString("last_name"));
                    // Set other fields as needed...
                    return employee; // Return the full employee object on success
                } else {
                    // Password is incorrect
                    System.out.println("[SERVER] Login failed: Incorrect password for " + username);
                    return null; // Return null if password doesn't match
                }
            } else {
                // User does not exist
                System.out.println("[SERVER] Login failed: User '" + username + "' not found.");
                return null; // Return null if username doesn't exist
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Server database error during login.", e);
        }
    }
}
