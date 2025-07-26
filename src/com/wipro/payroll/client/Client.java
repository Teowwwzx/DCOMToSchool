package com.wipro.payroll.client;

import com.wipro.payroll.common.*;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class Client {

    private static PayrollService payrollService;
    private static Scanner scanner = new Scanner(System.in);
    private static User loggedInUser = null;

    public static void main(String[] args) {
        try {
            // 1. Connect to the RMI Service once at the start
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            payrollService = (PayrollService) registry.lookup("PayrollService");
            System.out.println("✅ Successfully connected to the Payroll RMI Service.");

            // 2. Start the main application loop
            while (true) {
                showMainMenu();
            }

        } catch (Exception e) {
            System.err.println("❌ FATAL: Could not connect to the RMI Server. Please ensure it is running.");
        }
    }

    private static void showMainMenu() {
        System.out.println("\n--- Welcome to Wipro Logistics Payroll System ---");
        System.out.println("1. Login");
        System.out.println("2. Exit");
        System.out.print("Choose an option: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                System.out.println("Thank you for using the system. Exiting...");
                System.exit(0); // Terminate the application
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    /**
     * Handles the user login process.
     */
    private static void handleLogin() {
        try {
            System.out.println("\n--- System Login ---");
            System.out.print("Enter Username: ");
            String username = scanner.nextLine();
            System.out.print("Enter Password: ");
            String password = scanner.nextLine();

            loggedInUser = payrollService.login(username, password);

            if (loggedInUser != null) {
                System.out.println("\n✅ Login successful! Welcome, " + loggedInUser.getFirstName() + ".");
                // Direct user to the appropriate menu based on their role
                routeUserBasedOnRole();
            } else {
                System.out.println("❌ Login failed. Invalid username or password.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred during login: " + e.getMessage());
        }
    }

    /**
     * Checks the logged-in user's roles and directs them to the correct menu.
     */
    private static void routeUserBasedOnRole() {
        if (hasRole(loggedInUser, "HR")) {
            showHrMenu();
        } else if (hasRole(loggedInUser, "MANAGER")) {
            showManagerMenu();
        } else if (hasRole(loggedInUser, "EMPLOYEE")) {
            showEmployeeMenu(false);
        } else {
            System.out.println("You do not have a role assigned. Please contact an administrator.");
            handleLogout();
        }
    }

    // =================================================================
    //  ROLE-SPECIFIC MENUS
    // =================================================================

    /**
     * Displays the menu for users with the EMPLOYEE role.
     */
    private static void showEmployeeMenu(boolean isSwitchedView) {
        while (loggedInUser != null) {
            System.out.println("\n--- Employee Portal ---");
            System.out.println("1. View My Latest Payslip");
            System.out.println("2. Update My Bank Details");
            if (isSwitchedView) {
                System.out.println("9. Return to HR Menu");
            } else {
                System.out.println("9. Logout");
            }
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    handleViewMyPayslip();
                    break;
                case "2":
                    handleUpdateBankDetails();
                    break;
                case "9":
                    if (isSwitchedView) {
                        System.out.println("Returning to HR menu...");
                        return; // FIX: Just return to the calling menu (showHrMenu)
                    } else {
                        handleLogout(); // Perform a full logout
                        return;
                    }
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /**
     * Displays the menu for users with the HR role.
     */
    private static void showHrMenu() {
        while (loggedInUser != null) {
            System.out.println("\n--- HR Administrator Portal ---");
            System.out.println("1. Run Monthly Payroll for All Employees");
            System.out.println("2. List All Users");
            System.out.println("3. Register New User");
            System.out.println("4. Manage Compensation Templates"); // <-- NEW OPTION
            System.out.println("9. Switch to Employee View");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    handleRunPayroll();
                    break;
                case "2":
                    handleListAllUsers();
                    break;
                case "3":
                    handleHrRegisterUser();
                    break;
                case "4":
                    handleManageCompensation();
                case "9":
                    showEmployeeMenu(true); // Pass true to show "Return to HR Menu"
                    break;
                case "0":
                    handleLogout();
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }


    private static void handleHrRegisterUser() {
        try {
            System.out.println("\n--- HR: Register New User ---");
            User newUser = new User();

            System.out.print("Enter First Name: ");
            newUser.setFirstName(scanner.nextLine());
            System.out.print("Enter Last Name: ");
            newUser.setLastName(scanner.nextLine());
            System.out.print("Enter Username: ");
            newUser.setUsername(scanner.nextLine());
            System.out.print("Enter Email: ");
            newUser.setEmail(scanner.nextLine());
            System.out.print("Enter IC/Passport Number: ");
            newUser.setIc(scanner.nextLine());
            System.out.print("Enter Phone Number: ");
            newUser.setPhoneNumber(scanner.nextLine());
            System.out.print("Enter Department ID (e.g., 1=General, 2=Logistics): ");
            newUser.setDepartmentId(Integer.parseInt(scanner.nextLine()));

            System.out.print("Enter a temporary password for the user: ");
            String password = scanner.nextLine();

            // Call the RMI method to register the user
            // Note: Our current registerUser RMI method is simple. A real one might take a User object.
            // For now, we will adapt to the existing RMI method.
            // To make this work, we need to update the PayrollService to accept a User object.
            // Let's assume we update it for a better design.
            String response = payrollService.registerUser(newUser, password);
            System.out.println("\n✅ Server Response: " + response);

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid Department ID. Please enter a number.");
        } catch (Exception e) {
            System.err.println("❌ An error occurred during registration: " + e.getMessage());
        }
    }


    /**
     * Displays the menu for users with the MANAGER role.
     */
    private static void showManagerMenu() {
        System.out.println("\n[Manager Portal: Feature Not Implemented Yet]");
        System.out.println("This is where you would call RMI methods to show reports for your department.");
        handleLogout(); // For now, just log out.
    }

    // =================================================================
    //  FEATURE HANDLERS
    // =================================================================

    private static void handleViewMyPayslip() {
        try {
            System.out.println("\nFetching your latest payslip...");
            Payslip payslip = payrollService.getMyLatestPayslip(loggedInUser.getId());

            if (payslip != null) {
                printPayslip(payslip);
            } else {
                System.out.println("No payslips found for your account.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ Error fetching payslip: " + e.getMessage());
        }
    }

    private static void handleUpdateBankDetails() {
        try {
            System.out.println("\n--- My Bank Details ---");
            UserBankDetails currentDetails = payrollService.getMyBankDetails(loggedInUser.getId());

            if (currentDetails != null) {
                System.out.println("Current Bank Name: " + currentDetails.getBankName());
                System.out.println("Current Account No: " + currentDetails.getAccountNumber());
                System.out.println("Current Account Name: " + currentDetails.getAccountHolderName());
            } else {
                System.out.println("You have no bank details on file.");
            }

            // 2. Ask the user if they want to update
            System.out.print("\nDo you want to add or update your details? (y/n): ");
            String choice = scanner.nextLine();

            if (!choice.equalsIgnoreCase("y")) {
                System.out.println("Update cancelled.");
                return;
            }

            System.out.println("\n--- Enter New Bank Details ---");
            System.out.print("Enter Bank Name (e.g., Maybank): ");
            String bankName = scanner.nextLine();
            System.out.print("Enter Account Number: ");
            String accNo;
            while (true) {
                System.out.print("Enter Account Number (digits only): ");
                accNo = scanner.nextLine();
                // Use a simple regular expression to check if it's all digits and within a length range
                if (accNo.matches("\\d{8,16}")) { // This means "8 to 16 digits"
                    break; // Valid format, exit the loop
                } else {
                    System.out.println("❌ Invalid format. Please enter 8 to 16 digits with no spaces or symbols.");
                }
            }

            System.out.print("Enter Account Holder Name: ");
            String accName = scanner.nextLine();

            UserBankDetails details = new UserBankDetails();
            details.setUserId(loggedInUser.getId());
            details.setBankName(bankName);
            details.setAccountNumber(accNo);
            details.setAccountHolderName(accName);

            boolean success = payrollService.updateMyBankDetails(loggedInUser.getId(), details);
            if (success) {
                System.out.println("✅ Bank details updated successfully.");
            } else {
                System.out.println("❌ Failed to update bank details.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ Error updating bank details: " + e.getMessage());
        }
    }

    private static void handleRunPayroll() {
        try {
            System.out.println("\nAre you sure you want to run the monthly payroll for all employees? This cannot be undone.");
            System.out.print("Type 'YES' to confirm: ");
            String confirmation = scanner.nextLine();
            if (confirmation.equals("YES")) {
                System.out.println("Processing... please wait.");
                String result = payrollService.runMonthlyPayroll(loggedInUser.getId());
                System.out.println("\n✅ Server Response: " + result);
            } else {
                System.out.println("Payroll run cancelled.");
            }
        } catch(SecurityException se) {
            System.err.println("❌ SECURITY ERROR: You do not have permission to perform this action.");
        } catch (RemoteException e) {
            System.err.println("❌ Error running payroll: " + e.getMessage());
        }
    }

    private static void handleListAllUsers() {
        try {
            System.out.println("\nFetching all users...");
            List<User> users = payrollService.getAllUsers(loggedInUser.getId());
            System.out.println("------------------------------------------------------------------");
            System.out.printf("%-5s | %-15s | %-20s | %-25s%n", "ID", "Username", "Full Name", "Email");
            System.out.println("------------------------------------------------------------------");
            for (User user : users) {
                System.out.printf("%-5d | %-15s | %-20s | %-25s%n",
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName() + " " + user.getLastName(),
                        user.getEmail());
            }
            System.out.println("------------------------------------------------------------------");

        } catch(SecurityException se) {
            System.err.println("❌ SECURITY ERROR: You do not have permission to perform this action.");
        } catch (RemoteException e) {
            System.err.println("❌ Error fetching users: " + e.getMessage());
        }
    }

    private static void handleLogout() {
        System.out.println("Logging out user: " + loggedInUser.getUsername());
        loggedInUser = null;
    }

    // =================================================================
    //  UTILITY HELPERS
    // =================================================================

    /**
     * Checks if the logged-in user has a specific role.
     */
    private static boolean hasRole(User user, String roleName) {
        for (Role role : user.getRoles()) {
            if (role.getName().equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints a payslip to the console in a nicely formatted way.
     */
    private static void printPayslip(Payslip payslip) {
        System.out.println("\n==================================================");
        System.out.println("          EARNINGS STATEMENT / PAYSLIP          ");
        System.out.println("==================================================");
        System.out.println(" Employee: " + loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        System.out.println(" Pay Period: " + payslip.getPayPeriodStartDate() + " to " + payslip.getPayPeriodEndDate());
        System.out.println("--------------------------------------------------");
        System.out.printf("%-30s %15s%n", "DESCRIPTION", "AMOUNT (RM)");
        System.out.println("--------------------------------------------------");

        System.out.println("\n  EARNINGS:");
        for (PayItem item : payslip.getPayItems()) {
            if (item.getType() == PayItemType.EARNING) {
                System.out.printf("  %-28s %15.2f%n", item.getName(), item.getAmount());
            }
        }
        System.out.printf("\n  %-28s %15.2f%n", "GROSS PAY:", payslip.getGrossEarnings());

        System.out.println("\n  DEDUCTIONS:");
        for (PayItem item : payslip.getPayItems()) {
            if (item.getType() == PayItemType.DEDUCTION) {
                System.out.printf("  %-28s %15.2f%n", item.getName(), item.getAmount());
            }
        }
        System.out.printf("\n  %-28s %15.2f%n", "TOTAL DEDUCTIONS:", payslip.getTotalDeductions());

        System.out.println("==================================================");
        System.out.printf("  %-28s %15.2f%n", "NET PAY:", payslip.getNetPay());
        System.out.println("==================================================");
    }

    // Add this new helper method to Client.java

    private static void handleManageCompensation() {
        if (!hasRole(loggedInUser, "HR")) {
            System.out.println("❌ You do not have permission to perform this action.");
            return;
        }

        try {
            System.out.println("\n--- Manage Compensation Templates ---");
            // Step 1: Display all job titles
            List<JobTitle> jobTitles = payrollService.getAllJobTitles(loggedInUser.getId());
            if (jobTitles.isEmpty()) {
                System.out.println("No job titles found.");
                return;
            }
            System.out.println("Available Job Titles:");
            for (JobTitle job : jobTitles) {
                System.out.printf("  ID: %d, Title: %s %s (Dept ID: %d)\n",
                        job.getId(), job.getLevel(), job.getTitle(), job.getDeptId());
            }

            // Step 2: Ask HR to select a job title
            System.out.print("\nEnter the ID of the job title to view/edit: ");
            int jobTitleId = Integer.parseInt(scanner.nextLine());

            // Step 3: Fetch and display the compensation components for that title
            List<PayTemplate> templates = payrollService.getPayTemplatesForJobTitle(loggedInUser.getId(), jobTitleId);
            if (templates.isEmpty()) {
                System.out.println("No compensation template found for this job title.");
                return;
            }

            System.out.println("\nCurrent Compensation Package:");
            for (PayTemplate item : templates) {
                System.out.printf("  Item ID: %d, Description: %s, Type: %s, Amount: RM %.2f\n",
                        item.getId(), item.getDescription(), item.getType(), item.getAmount());
            }

            // Step 4: Ask if they want to edit
            System.out.print("\nDo you want to edit an item? (y/n): ");
            if (!scanner.nextLine().equalsIgnoreCase("y")) {
                return;
            }

            System.out.print("Enter the Item ID to update: ");
            int templateItemId = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter the new Amount (e.g., 8500.00): ");
            BigDecimal newAmount = new BigDecimal(scanner.nextLine());

            // Step 5: Call the RMI method to perform the update
            boolean success = payrollService.updatePayTemplateItem(loggedInUser.getId(), templateItemId, newAmount);
            if (success) {
                System.out.println("✅ Successfully updated the compensation template.");
            } else {
                System.out.println("❌ Failed to update the item. Please check the ID.");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid input. Please enter a number.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }
}
