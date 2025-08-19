package com.wipro.payroll.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) {
        final String[] KNOWN_IPS = {
                "192.168.100.5",  // Index 0
                "192.168.100.35",  // Index 1
                "192.168.100.35"
        };

        String hostname = "localhost"; // The default for local development

        if (args.length > 0) {
            try {
                int index = Integer.parseInt(args[0]);
                // Check if the provided index is valid for our array
                if (index >= 0 && index < KNOWN_IPS.length) {
                    hostname = KNOWN_IPS[index];
                    System.out.println("✅ INFO: Selected pre-defined IP at index " + index + ": " + hostname);
                } else {
                    System.err.println("⚠️ WARN: Index " + index + " is out of bounds. Defaulting to localhost.");
                }
            } catch (NumberFormatException e) {
                // If the argument isn't a number, we can just use it directly as a custom IP
                hostname = args[0];
                System.out.println("✅ INFO: Using custom IP from argument: " + hostname);
            }
        } else {
            System.out.println("✅ INFO: No index provided. Defaulting to localhost.");
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
            System.out.println("   Listening on IP " + System.getProperty("java.rmi.server.hostname") + " on port 1099...");

            System.out.println("\n[Server is active. Press the red 'Stop' button in IntelliJ to shut down]");
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("com.wipro.payroll.server.Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
