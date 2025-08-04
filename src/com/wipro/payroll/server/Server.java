package com.wipro.payroll.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) {
        try {
//            C3011 - 192.168.100.5
//            Mobile Hostpot -
            System.setProperty("java.rmi.server.hostname", "192.168.100.5");

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
