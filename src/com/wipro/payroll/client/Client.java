package com.wipro.payroll.client;

import com.wipro.payroll.common.*;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Client {

    private static PayrollService payrollService;
    private static Scanner scanner = new Scanner(System.in);
    private static User loggedInUser = null;

    public static void main(String[] args) {
        try {
            String serverAddress = "192.168.100.5";

            Registry registry = LocateRegistry.getRegistry("serverAddress", 1099);
//            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            payrollService = (PayrollService) registry.lookup("PayrollService");
            System.out.println("✅ Successfully connected to the Payroll RMI Service.");

            while (true) {
                if (loggedInUser == null) {
                    showMainMenu();
                } else {
                    routeUserBasedOnRole();
                }
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
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    private static void handleLogin() {
        try {
            System.out.println("\n--- System Login ---");
            System.out.print("Enter Username: ");
            String username = scanner.nextLine();
            System.out.print("Enter Password: ");
            String password = scanner.nextLine();

            loggedInUser = payrollService.login(username, password);

            if (loggedInUser != null) {
                System.out.println("\n✅ Login successful! Welcome, " + loggedInUser.getFName() + ". Your role is: " + loggedInUser.getRole());
                routeUserBasedOnRole();
            } else {
                System.out.println("❌ Login failed. Invalid username or password.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred during login: " + e.getMessage());
        }
    }

    private static void routeUserBasedOnRole() {
        if (loggedInUser == null) return; // Should not happen, but a good safeguard

        if (hasRole("HR")) {
            showHrMenu();
        } else if (hasRole("MANAGER")) {
            showManagerMenu();
        } else if (hasRole("EMPLOYEE")) {
            showEmployeeMenu(false);
        } else {
            System.out.println("Your assigned role is not recognized. Please contact an administrator.");
            handleLogout();
        }
    }


    // =================================================================
    //  ROLE-SPECIFIC MENUS
    // =================================================================

    private static void showEmployeeMenu(boolean isSwitchedView) {
        while (loggedInUser != null && (hasRole("EMPLOYEE") || isSwitchedView)) {
            System.out.println("\n--- Employee Portal ---");
            System.out.println("1. View My Profile");
            System.out.println("2. View My Latest Payslip");
            System.out.println("3. Update My Bank Details");

            if (isSwitchedView) {
                System.out.println("9. Return to Previous Menu");
            } else {
                System.out.println("9. Logout");
            }

            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleViewMyProfile();
                    break;
                case "2":
                    handleViewMyLatestPayslip();
                    break;
                case "3":
                    handleUpdateBankDetails();
                    break;
                case "9":
                    if (isSwitchedView) {
                        System.out.println("Returning to previous menu...");
                        return;
                    } else {
                        handleLogout();
                    }
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }


    // =================================================================
    //  HR Administrator Methods
    // =================================================================

    private static void showHrMenu() {
        while (loggedInUser != null && hasRole("HR")) {
            System.out.println("\n--- HR Administrator Portal ---");

            System.out.println("\n-- Employee Management --");
            System.out.println("1. View All Employees");
            System.out.println("2. Create New Employee");
            System.out.println("3. Edit Employee Details");

            System.out.println("\n-- Payroll & Compensation --");
            System.out.println("4. Manage Pay Template");
            System.out.println("5. Payroll Operations");
            System.out.println("6. View Payroll History");
            System.out.println("7. Generate Monthly Summary Report");
            System.out.println("8. View Pending Approvals Report");

            System.out.println("\n");
            System.out.println("9. Switch to My Employee View");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleReadAllUsers();
                    break;
                case "2":
                    handleCreateUser();
                    break;
                case "3":
                    handleEditUser();
                    break;
                case "4":
                    handleManageCompensation();
                    break;
                case "5":
                    handlePayrollOperations();
                    break;
                case "6":
                    handleViewPayrollHistory();
                    break;
                case "7":
                    handleGenerateSummaryReport();
                    break;
                case "8":
                    handleViewPendingApprovals();
                    break;
                case "9":
                    System.out.println("Switching to Employee View...");
                    showEmployeeMenu(true);
                    break;
                case "0":
                    handleLogout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void handlePayrollOperations() {
        try {
            List<Bonus> pendingBonuses = payrollService.getAllPendingBonuses(loggedInUser.getId());

            if (!pendingBonuses.isEmpty()) {
                String redColor = "\u001B[31m";
                String resetColor = "\u001B[0m";

                System.out.println("\n" + redColor + "================== WARNING ==================" + resetColor);
                System.out.printf(redColor + "There are %d bonus approvals still pending." + resetColor + "\n", pendingBonuses.size());
                System.out.println(redColor + "Running payroll now may result in underpayment for some employees." + resetColor);
                System.out.println("==========================================");
                System.out.print("Are you absolutely sure you want to proceed? (yes/no): ");

                if (!scanner.nextLine().equalsIgnoreCase("yes")) {
                    System.out.println("Payroll operation cancelled. Please clear pending approvals first.");
                    return; // Exit the function to prevent the payroll run
                }
            }

            // STEP 3: If there are no pending items, or if the user overrode the warning, proceed.
            System.out.println("\n--- Payroll Operations ---");
            System.out.println("1. Run Full Payroll Cycle for ALL Employees");
            System.out.println("2. Run Payroll for a SINGLE Employee");
            System.out.println("9. Return");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                runPayrollFor(0); // Use 0 to signify "ALL"
            } else if (choice.equals("2")) {
                System.out.print("Enter the User ID of the employee to process: ");
                int targetUserId = Integer.parseInt(scanner.nextLine());
                runPayrollFor(targetUserId);
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number.");
        } catch (RemoteException e) {
            System.err.println("❌ Error checking for pending approvals: " + e.getMessage());
        }
    }

    private static void handleViewPayrollHistory() {
        // This method first calls the list handler...
        handleListAllPayslips();

        // ...then asks the user if they want to see details.
        System.out.print("\nWould you like to view the full details for a specific payslip? (y/n): ");
        String choice = scanner.nextLine();

        if (choice.equalsIgnoreCase("y")) {
            // If yes, it calls the detailed view handler.
            handleViewEmployeePayslip();
        }
    }

    private static void handleViewPendingApprovals() {
        try {
            System.out.println("\n--- Report: All Pending Bonus Approvals ---");
            List<Bonus> pendingBonuses = payrollService.getAllPendingBonuses(loggedInUser.getId());

            if (pendingBonuses.isEmpty()) {
                System.out.println("✅ Great! There are no pending bonus approvals in the system.");
                return;
            }

            System.out.println("WARNING: The following items must be approved by their managers before payroll is run:");
            System.out.println("------------------------------------------------------------------------------------------");
            System.out.printf("| %-8s | %-12s | %-20s | %-30s |%n", "Bonus ID", "Pay Period", "Employee Name", "Description");
            System.out.println("------------------------------------------------------------------------------------------");
            for (Bonus bonus : pendingBonuses) {
                System.out.printf("| %-8d | %-12s | %-20s | %-30s |%n",
                        bonus.getId(),
                        bonus.getPayPeriodStartDate().toString().substring(0, 7),
                        bonus.getEmployeeName(),
                        bonus.getName());
            }
            System.out.println("------------------------------------------------------------------------------------------");

        } catch (RemoteException e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }


    // =================================================================
    //  MANAGER Administrator Methods
    // =================================================================

    private static void showManagerMenu() {
        while (loggedInUser != null && hasRole("MANAGER")) {
            System.out.println("\n--- Manager Portal ---");
            System.out.println("1. View My Department Report");
            System.out.println("2. Approve Timesheets / Bonuses");
            System.out.println("8. Switch to My Employee View");
            System.out.println("9. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleViewDepartmentReport();
                    break;
                case "2":
                    handleApproveBonuses(); // UNCOMMENT
                    break;
                case "8":
                    System.out.println("Switching to Employee View...");
                    showEmployeeMenu(true);
                    break;
                case "9":
                    handleLogout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void handleViewDepartmentReport() {
        try {
            System.out.println("\nFetching your department report...");
            List<User> employees = payrollService.getMyDepartmentEmployees(loggedInUser.getId());

            if (employees.isEmpty()) {
                System.out.println("No employees found in your department.");
                return;
            }

            System.out.println("\n--- Report for Your Department ---");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("| %-4s | %-15s | %-20s | %-25s |%n", "ID", "Username", "Full Name", "Job Title");
            System.out.println("--------------------------------------------------------------------------");

            for (User user : employees) {
                System.out.printf("| %-4d | %-15s | %-20s | %-25s |%n",
                        user.getId(),
                        user.getUsername(),
                        user.getFName() + " " + user.getLName(),
                        user.getJobTitle());
            }
            System.out.println("--------------------------------------------------------------------------");
            System.out.println(employees.size() + " employee(s) in your department.");

        } catch (RemoteException e) {
            System.err.println("❌ An error occurred while fetching your report: " + e.getMessage());
        }
    }

    private static void handleApproveBonuses() {
        try {
            System.out.println("\n--- Approve Pending Bonuses ---");

            // 1. Fetch the list of unapproved bonuses
            List<Bonus> pendingBonuses = payrollService.getUnapprovedBonusesForMyDepartment(loggedInUser.getId());

            if (pendingBonuses.isEmpty()) {
                System.out.println("There are no pending bonuses in your department to approve.");
                return;
            }

            // 2. Display the list in a table
            System.out.println("The following bonuses are pending your approval:");
            System.out.println("--------------------------------------------------------------------------------------");
            System.out.printf("| %-8s | %-20s | %-30s | %15s |%n", "Bonus ID", "Employee Name", "Description", "Amount (RM)");
            System.out.println("--------------------------------------------------------------------------------------");
            for (Bonus bonus : pendingBonuses) {
                System.out.printf("| %-8d | %-20s | %-30s | %15.2f |%n",
                        bonus.getId(),
                        bonus.getEmployeeName(),
                        bonus.getName(),
                        bonus.getAmount());
            }
            System.out.println("--------------------------------------------------------------------------------------");

            // 3. Prompt the manager for action
            System.out.print("Enter the Bonus ID to approve (or press Enter to cancel): ");
            String input = scanner.nextLine();

            if (input.isBlank()) {
                System.out.println("Approval process cancelled.");
                return;
            }

            int bonusIdToApprove = Integer.parseInt(input);

            // 4. Call the approval method on the server
            boolean success = payrollService.approveBonus(loggedInUser.getId(), bonusIdToApprove);

            if (success) {
                System.out.println("✅ Bonus ID " + bonusIdToApprove + " has been successfully approved.");
            } else {
                System.out.println("❌ Failed to approve bonus. The ID may be incorrect or it was already approved.");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }

    // =================================================================
    //  FEATURE HANDLERS & HELPERS
    // =================================================================

    private static void handleViewMyLatestPayslip() {
        try {
            System.out.println("\nFetching your latest payslip...");
            Payslip payslip = payrollService.getMyLatestPayslip(loggedInUser.getId());

            if (payslip != null) {
                printPayslip(payslip); // Use a helper to format the output
            } else {
                System.out.println("No payslips found for your account.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ Error fetching payslip: " + e.getMessage());
        }
    }

    private static void handleUpdateBankDetails() {
        try {
            // 1. Show current details
            System.out.println("\n--- Update My Bank Details ---");
            UserBankDetails currentDetails = payrollService.getMyBankDetails(loggedInUser.getId());
            if (currentDetails != null) {
                System.out.println("Current Bank Name: " + currentDetails.getBankName());
                System.out.println("Current Account No: " + currentDetails.getAccountNumber());
            } else {
                System.out.println("You have no bank details on file.");
            }

            // 2. Prompt for new details
            System.out.println("\nPlease enter your new details:");
            System.out.print("Enter Bank Name: ");
            String bankName = scanner.nextLine();
            System.out.print("Enter Account Number (digits only): ");
            String accNo = scanner.nextLine();
            System.out.print("Enter Account Holder Name (as per bank records): ");
            String accName = scanner.nextLine();

            if(bankName.isBlank() || accNo.isBlank() || accName.isBlank()) {
                System.out.println("All fields are required. Update cancelled.");
                return;
            }

            // 3. Send update to server
            UserBankDetails newDetails = new UserBankDetails();
            newDetails.setUserId(loggedInUser.getId());
            newDetails.setBankName(bankName);
            newDetails.setAccountNumber(accNo);
            newDetails.setAccountHolderName(accName);

            boolean success = payrollService.updateMyBankDetails(loggedInUser.getId(), newDetails);
            if (success) {
                System.out.println("✅ Bank details updated successfully.");
            } else {
                System.out.println("❌ Failed to update bank details.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ Error updating bank details: " + e.getMessage());
        }
    }

    // Add this helper method to format the payslip nicely
    private static void printPayslip(Payslip payslip) {
        System.out.println("\n==================================================");
        System.out.println("                  EARNINGS STATEMENT                ");
        System.out.println("==================================================");
        System.out.println(" Employee: " + loggedInUser.getFName() + " " + loggedInUser.getLName());
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

    private static void handleViewMyProfile() {
        try {
            System.out.println("\nFetching your profile...");
            UserProfile profile = payrollService.getMyProfile(loggedInUser.getId());

            if (profile != null) {
                System.out.println("\n================ MY PROFILE ================");
                System.out.println("Name: " + profile.getFirstName() + " " + profile.getLastName());
                System.out.println("Username: " + profile.getUsername());
                System.out.println("Email: " + profile.getEmail());
                System.out.println("Phone: " + profile.getPhone());
                System.out.println("------------------------------------------");
                System.out.println("Department: " + profile.getDepartmentName());
                System.out.println("Job Title: " + profile.getJobTitle());
                System.out.println("Employment: " + profile.getEmploymentType());
                System.out.println("------------------------------------------");

                UserBankDetails bank = profile.getBankDetails();
                if (bank != null) {
                    System.out.println("Bank Name: " + bank.getBankName());
                    System.out.println("Account No: " + bank.getAccountNumber());
                } else {
                    System.out.println("Bank Details: Not set.");
                }

                System.out.println("==========================================");
            } else {
                System.out.println("Could not retrieve your profile.");
            }
        } catch (RemoteException e) {
            System.err.println("❌ Error fetching profile: " + e.getMessage());
        }
    }

    // HR Functions
    private static void handleCreateUser() {
        try {
            System.out.println("\n--- Create New Employee ---");

            // --- Step 1: Fetch and display lists for user-friendly selection ---
            System.out.println("Loading available job titles...");
            List<JobTitle> jobTitles = payrollService.getAllJobTitles(loggedInUser.getId());
            for (JobTitle job : jobTitles) {
                System.out.printf("  ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel());
            }

            System.out.println("\nLoading available employment types...");
            List<EmpType> empTypes = payrollService.getAllEmpTypes(loggedInUser.getId());
            for (EmpType type : empTypes) {
                System.out.printf("  ID: %d, Type: %s\n", type.getId(), type.getName());
            }

            User newUser = new User();
            System.out.println("Please provide the following details for the new employee:");

            System.out.print("First Name: ");
            newUser.setFName(scanner.nextLine());
            System.out.print("Last Name: ");
            newUser.setLName(scanner.nextLine());
            System.out.print("Username: ");
            newUser.setUsername(scanner.nextLine());
            System.out.print("Email: ");
            newUser.setEmail(scanner.nextLine());
            System.out.print("Phone Number: ");
            newUser.setPhone(scanner.nextLine());
            System.out.print("IC / Passport Number: ");
            newUser.setIc(scanner.nextLine());
            System.out.print("Job Title ID (press Enter to skip): ");

            String jobTitleIdStr = scanner.nextLine();
            if (!jobTitleIdStr.isBlank()) {
                newUser.setJobTitleId(Integer.parseInt(jobTitleIdStr));
            }

            System.out.print("Employment Type ID (press Enter to skip): ");
            String empTypeIdStr = scanner.nextLine();
            if (!empTypeIdStr.isBlank()) {
                newUser.setEmpTypeId(Integer.parseInt(empTypeIdStr));
            }
            System.out.print("Temporary Password: ");
            String tempPassword = scanner.nextLine();

            // Call the RMI method, passing the logged-in HR admin's ID for the security check
            String response = payrollService.createUser(loggedInUser.getId(), newUser, tempPassword);
            System.out.println("\n✅ Server Response: " + response);

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number for Job Title and Employment Type.");
        } catch (SecurityException se) {
            System.err.println("❌ SECURITY ERROR: " + se.getMessage());
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred while creating the user: " + e.getMessage());
        }
    }

    private static void handleReadAllUsers() {
        try {
            System.out.println("\nFetching all employee records...");
            List<User> users = payrollService.readAllUsers(loggedInUser.getId());

            if (users == null || users.isEmpty()) {
                System.out.println("No users found in the system.");
                return;
            }

            // Print a formatted table header
            System.out.println("-------------------------------------------------------------------------------------------------------------");
            System.out.printf("| %-4s | %-15s | %-20s | %-25s | %-25s |%n", "ID", "Username", "Full Name", "Job Title", "Department");
            System.out.println("-------------------------------------------------------------------------------------------------------------");

            // Print each user's details
            for (User user : users) {
                System.out.printf("| %-4d | %-15s | %-20s | %-25s | %-25s |%n",
                        user.getId(),
                        user.getUsername(),
                        user.getFName() + " " + user.getLName(),
                        user.getJobTitle(),
                        user.getDepartmentName());
            }
            System.out.println("-------------------------------------------------------------------------------------------------------------");
            System.out.println(users.size() + " user(s) found.");

        } catch (SecurityException se) {
            System.err.println("❌ SECURITY ERROR: " + se.getMessage());
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred while fetching users: " + e.getMessage());
        }
    }

    private static void handleEditUser() {
        try {
            System.out.println("\n--- Edit Employee Details ---");
            System.out.print("Enter the ID of the user you wish to edit: ");
            int targetUserId = Integer.parseInt(scanner.nextLine());

            // 1. Fetch the user's current details from the server
            User userToEdit = payrollService.readUserById(loggedInUser.getId(), targetUserId);
            if (userToEdit == null) {
                System.out.println("❌ Error: User with ID " + targetUserId + " not found.");
                return;
            }

            // 2. Display current details and prompt for new ones.
            // The user can press Enter to keep the current value.
            System.out.println("\nEnter new details. Press Enter to keep the current value.");

            System.out.printf("First Name [%s]: ", userToEdit.getFName());
            String newFName = scanner.nextLine();
            if (!newFName.isBlank()) userToEdit.setFName(newFName);

            System.out.printf("Last Name [%s]: ", userToEdit.getLName());
            String newLName = scanner.nextLine();
            if (!newLName.isBlank()) userToEdit.setLName(newLName);

            System.out.printf("Email [%s]: ", userToEdit.getEmail());
            String newEmail = scanner.nextLine();
            if (!newEmail.isBlank()) userToEdit.setEmail(newEmail);

            System.out.printf("Phone [%s]: ", userToEdit.getPhone());
            String newPhone = scanner.nextLine();
            if (!newPhone.isBlank()) userToEdit.setPhone(newPhone);

            System.out.printf("IC/Passport [%s]: ", userToEdit.getIc());
            String newIc = scanner.nextLine();
            if (!newIc.isBlank()) userToEdit.setIc(newIc);

            // --- Handle Job Title and Emp Type with lists ---
            System.out.println("\n--- Assign Job Title ---");
            System.out.printf("Current Job Title: %d - %s\n", userToEdit.getJobTitleId(), userToEdit.getJobTitle());
            List<JobTitle> jobTitles = payrollService.getAllJobTitles(loggedInUser.getId());
            jobTitles.forEach(job -> System.out.printf("  ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel()));
            System.out.print("Enter new Job Title ID (press Enter to keep current): ");
            String newJobIdStr = scanner.nextLine();
            if (!newJobIdStr.isBlank()) userToEdit.setJobTitleId(Integer.parseInt(newJobIdStr));

            System.out.println("\n--- Assign Employment Type ---");
            System.out.printf("Current Employment Type ID: %d\n", userToEdit.getEmpTypeId());
            List<EmpType> empTypes = payrollService.getAllEmpTypes(loggedInUser.getId());
            empTypes.forEach(type -> System.out.printf("  ID: %d, Type: %s\n", type.getId(), type.getName()));
            System.out.print("Enter new Employment Type ID (press Enter to keep current): ");
            String newEmpTypeIdStr = scanner.nextLine();
            if (!newEmpTypeIdStr.isBlank()) userToEdit.setEmpTypeId(Integer.parseInt(newEmpTypeIdStr));

            // 3. Call the update method on the server
            boolean success = payrollService.updateUser(loggedInUser.getId(), userToEdit);

            // 4. Display the result
            if (success) {
                System.out.println("\n✅ User details for '" + userToEdit.getUsername() + "' updated successfully.");
            } else {
                System.out.println("\n❌ Failed to update user details.");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred during the update process: " + e.getMessage());
        }
    }

    private static void handleManageCompensation() {
        try {
            System.out.println("\n--- Manage Compensation Rules ---");
            System.out.println("Loading available job titles...");
            List<JobTitle> jobTitles = payrollService.getAllJobTitles(loggedInUser.getId());
            jobTitles.forEach(job -> System.out.printf("  ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel()));

            System.out.print("\nEnter the ID of the job title to manage: ");
            int jobTitleId = Integer.parseInt(scanner.nextLine());

            // This loop allows the user to perform multiple actions on one package
            while (true) {
                List<PayTemplate> templates = payrollService.getPayTemplatesForJobTitle(loggedInUser.getId(), jobTitleId);
                System.out.println("\nCurrent Compensation Package for Job ID " + jobTitleId + ":");
                templates.forEach(item -> System.out.printf("  Item ID: %-4d | Type: %-10s | Desc: %-25s | Amount: RM %.2f\n",
                        item.getId(), item.getType(), item.getDescription(), item.getAmount()));

                System.out.println("\nWhat would you like to do?");
                System.out.println("1. Update an existing item's amount");
                System.out.println("2. Add a new item to this package");
                System.out.println("3. Delete an item from this package");
                System.out.println("9. Return to HR Menu");
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine();

                try {
                    switch (choice) {
                        case "1": // UPDATE
                            System.out.print("Enter the Item ID to update: ");
                            int updateId = Integer.parseInt(scanner.nextLine());
                            System.out.print("Enter the new Amount (e.g., 650.00): ");
                            BigDecimal newAmount = new BigDecimal(scanner.nextLine());
                            if (payrollService.updatePayTemplateItem(loggedInUser.getId(), updateId, newAmount)) {
                                System.out.println("✅ Item updated successfully.");
                            } else { System.out.println("❌ Update failed. Check ID."); }
                            break;
                        case "2": // ADD (CREATE)
                            System.out.println("\n--- Add New Compensation Item ---");
                            PayTemplate newItem = new PayTemplate();
                            newItem.setJobTitleId(jobTitleId); // Link to the current job title
                            System.out.print("Description (e.g., Car Allowance): ");
                            newItem.setDescription(scanner.nextLine());
                            System.out.print("Type (EARNING or DEDUCTION): ");
                            newItem.setType(PayItemType.valueOf(scanner.nextLine().toUpperCase()));
                            System.out.print("Amount (e.g., 400.00): ");
                            newItem.setAmount(new BigDecimal(scanner.nextLine()));
                            if (payrollService.addPayTemplateItem(loggedInUser.getId(), newItem) != null) {
                                System.out.println("✅ New item added successfully.");
                            } else { System.out.println("❌ Failed to add item."); }
                            break;
                        case "3": // DELETE
                            System.out.print("Enter the Item ID to delete: ");
                            int deleteId = Integer.parseInt(scanner.nextLine());
                            if (payrollService.deletePayTemplateItem(loggedInUser.getId(), deleteId)) {
                                System.out.println("✅ Item deleted successfully.");
                            } else { System.out.println("❌ Delete failed. Check ID."); }
                            break;
                        case "9":
                            return; // Exit the loop and return to the HR Menu
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (NumberFormatException e) {
                    // This catch block prevents the crash!
                    System.err.println("❌ Invalid input. Please enter a valid number for IDs and amounts.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }

    private static void handleRunPayroll() {
        try {
            System.out.println("\n--- Run Monthly Payroll ---");
            System.out.print("Enter the Year for the payroll run (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the Month for the payroll run (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());

            // Add a strong confirmation warning
            System.out.println("\nWARNING: You are about to run the payroll for " + year + "-" + month + ".");
            System.out.println("This will generate payslips for all active employees and cannot be easily undone.");
            System.out.print("Type 'YES' to confirm and proceed: ");
            String confirmation = scanner.nextLine();

            if (confirmation.equals("YES")) {
                System.out.println("Processing... please wait. This may take a moment.");
                String result = payrollService.runMonthlyPayroll(loggedInUser.getId(), year, month);
                System.out.println("\n✅ Server Response: " + result);
            } else {
                System.out.println("Payroll run cancelled.");
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid year or month. Please enter numbers only.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred while running payroll: " + e.getMessage());
        }
    }

    private static void handleViewEmployeePayslip() {
        try {
            System.out.println("\n--- View Employee Payslip ---");
            System.out.print("Enter the User ID of the employee: ");
            int targetUserId = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the Year of the payslip (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the Month of the payslip (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());

            System.out.printf("Fetching payslip for user ID %d for %d-%02d...\n", targetUserId, year, month);

            Payslip payslip = payrollService.getPayslipForUser(loggedInUser.getId(), targetUserId, year, month);

            if (payslip != null) {
                User targetUser = payrollService.readUserById(loggedInUser.getId(), targetUserId);
                if (targetUser != null) {
                    printPayslip(payslip, targetUser);
                } else {
                    System.out.println("Could not find user details for ID " + targetUserId);
                }
            } else {
                System.out.println("No payslip found for that user for the specified period.");
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid input. Please enter numbers only.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }

    private static void printPayslip(Payslip payslip, User user) {
        System.out.println("\n==================================================");
        System.out.println("                  EARNINGS STATEMENT                ");
        System.out.println("==================================================");
        System.out.println(" Employee: " + user.getFName() + " " + user.getLName() + " (ID: " + user.getId() + ")");
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

    private static void handleListAllPayslips() {
        try {
            System.out.println("\nFetching all generated payslips...");
            List<PayslipSummary> summaries = payrollService.getAllPayslips(loggedInUser.getId());

            if (summaries.isEmpty()) {
                System.out.println("No payslips have been generated yet.");
                return;
            }

            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf("| %-10s | %-8s | %-20s | %-12s | %-15s |%n", "Payslip ID", "User ID", "Employee Name", "Pay Period", "Net Pay (RM)");
            System.out.println("-------------------------------------------------------------------------------");

            for (PayslipSummary summary : summaries) {
                System.out.printf("| %-10d | %-8d | %-20s | %-12s | %15.2f |%n",
                        summary.getPayslipId(),
                        summary.getUserId(),
                        summary.getUserFullName(),
                        summary.getPayPeriodStartDate().toString().substring(0, 7), // Show YYYY-MM
                        summary.getNetPay());
            }
            System.out.println("-------------------------------------------------------------------------------");

        } catch (RemoteException e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }

    private static void runPayrollFor(int targetUserId) {
        try {
            // This logic is from our old handleRunPayroll method
            System.out.print("Enter the Year for the payroll run (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the Month for the payroll run (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());

            System.out.print("Type 'YES' to confirm: ");
            if (scanner.nextLine().equals("YES")) {
                System.out.println("Processing...");
                // We need a new RMI method that can accept a targetUserId
                String result = payrollService.runMonthlyPayrollForTarget(loggedInUser.getId(), year, month, targetUserId);
                System.out.println("\n✅ Server Response: " + result);
            } else {
                System.out.println("Payroll run cancelled.");
            }
        } catch (Exception e) {
            System.err.println("❌ An error occurred: " + e.getMessage());
        }
    }

    private static void handleGenerateSummaryReport() {
        try {
            System.out.println("\n--- Monthly Payroll Summary Report ---");
            System.out.print("Enter the Year for the report (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the Month for the report (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());

            System.out.println("\nGenerating report... please wait.");
            PayrollSummaryReport report = payrollService.getPayrollSummaryReport(loggedInUser.getId(), year, month);

            if (report == null || report.getNumberOfEmployeesPaid() == 0) {
                System.out.println("No payroll data found for the specified period.");
                return;
            }

            System.out.println("\n=======================================================");
            System.out.printf("      Payroll Summary Report for %d-%02d\n", year, month);
            System.out.println("=======================================================");
            System.out.printf(" Employees Paid      : %d\n", report.getNumberOfEmployeesPaid());
            System.out.printf(" Total Gross Earnings: RM %,.2f\n", report.getTotalGrossEarnings());
            System.out.printf(" Total Deductions    : RM %,.2f\n", report.getTotalDeductions());
            System.out.printf(" Total Net Pay       : RM %,.2f\n", report.getTotalNetPay());
            System.out.println("-------------------------------------------------------");
            System.out.println(" Net Pay by Department:");
            if (report.getNetPayByDepartment().isEmpty()) {
                System.out.println("  No department breakdown available.");
            } else {
                for (Map.Entry<String, BigDecimal> entry : report.getNetPayByDepartment().entrySet()) {
                    System.out.printf("  - %-20s: RM %,.2f\n", entry.getKey(), entry.getValue());
                }
            }
            System.out.println("=======================================================");

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid year or month. Please enter numbers only.");
        } catch (RemoteException e) {
            System.err.println("❌ An error occurred while generating the report: " + e.getMessage());
        }
    }


    private static void handleLogout() {
        System.out.println("Logging out user: " + loggedInUser.getUsername());
        loggedInUser = null; // This will cause the main loop to go back to showMainMenu()
    }

    private static boolean hasRole(String roleName) {
        if (loggedInUser == null || loggedInUser.getRole() == null) {
            return false;
        }
        return loggedInUser.getRole().name().equalsIgnoreCase(roleName);
    }
}
