package com.wipro.payroll.client;

import com.wipro.payroll.common.PayrollService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            // 1. Get the RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // 2. Look up the remote object
            PayrollService payrollService = (PayrollService) registry.lookup("com.wipro.payroll.common.PayrollService");

            // 3. Get user input from the command line
            Scanner scanner = new Scanner(System.in);
            System.out.println("--- com.wipro.payroll.common.Employee Registration ---");
            System.out.print("Enter First Name: ");
            String firstName = scanner.nextLine();
            System.out.print("Enter Last Name: ");
            String lastName = scanner.nextLine();
            System.out.print("Enter IC/Passport Number: ");
            String icNumber = scanner.nextLine();
            System.out.print("Enter Username: ");
            String username = scanner.nextLine();
            System.out.print("Enter Password: ");
            String password = scanner.nextLine();

            // 4. Call the remote method
            String response = payrollService.registerEmployee(firstName, lastName, icNumber, username, password);

            // 5. Print the server's response
            System.out.println("\ncom.wipro.payroll.server.Server response: " + response);

            scanner.close();

        } catch (Exception e) {
            System.err.println("com.wipro.payroll.client.Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
