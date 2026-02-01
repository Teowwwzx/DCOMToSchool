package com.wipro.payroll.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) {

        String hostname;

        if (args.length > 0) {
            // If yes, use the provided IP. This is for network or cloud deployment.
            hostname = args[0];
            System.out.println("✅ INFO: Using hostname provided from command line: " + hostname);
        } else {
            // If no, default to "localhost". This is for easy local development.
            hostname = "localhost";
            System.out.println("✅ INFO: No hostname provided. Defaulting to 'localhost' for local development.");
        }

        try {
            System.setProperty("java.rmi.server.hostname", hostname);

            // 1. Create an instance of our implementation
            PayrollServiceImpl payrollService = new PayrollServiceImpl();

            // 2. Create the RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // 3. Bind our remote object to the registry with a name
            registry.bind("PayrollService", payrollService);

            System.out.println("✅ Payroll RMI Server is running and ready.");
            System.out.println("   Listening on IP " + hostname + " on port 1099...");
            System.out.println("\n[Server is active. Press the red 'Stop' button in IntelliJ to shut down]");

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("com.wipro.payroll.server.Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
