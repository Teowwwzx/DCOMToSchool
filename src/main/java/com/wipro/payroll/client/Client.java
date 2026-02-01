package com.wipro.payroll.client;

import com.wipro.payroll.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private static String[] SERVER_IPS = {
            "47.129.38.215",
//            "localhost",
    };
    private static final int RMI_PORT = 1099;
    private static final String SERVICE_NAME = "PayrollService";
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private static PayrollService payrollService = null;
    private static Scanner scanner = new Scanner(System.in);
    private static User loggedInUser = null;
    private static int currentServerIndex = -1;

    public static void main(String[] args) {
        if (args.length > 0) {
            SERVER_IPS = args;
            System.out.println("✅ INFO: Using custom server IPs from command line: " + Arrays.toString(SERVER_IPS));
        } else {
            System.out.println("✅ INFO: No IPs provided, using default server list: " + Arrays.toString(SERVER_IPS));
        }

        if (!establishConnection()) {
            System.err.println("❌ FATAL: Could not connect to any RMI Server at startup. Please ensure at least one server is running.");
            System.exit(1);
        }

        while (true) {
            if (loggedInUser == null) {
                showMainMenu();
            } else {
                routeUserBasedOnRole();
            }
        }
    }

    private static boolean establishConnection() {
        for (int i = 0; i < SERVER_IPS.length; i++) {
            currentServerIndex = (currentServerIndex + 1) % SERVER_IPS.length;
            String targetIp = SERVER_IPS[currentServerIndex];
            System.out.println("\n--- Attempting connection to " + targetIp + "...");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<PayrollService> lookupTask = () -> {
                Registry registry = LocateRegistry.getRegistry(targetIp, RMI_PORT);
                return (PayrollService) registry.lookup(SERVICE_NAME);
            };
            Future<PayrollService> future = executor.submit(lookupTask);

            try {
                payrollService = future.get(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                System.out.println("✅ Successfully connected to Payroll RMI Service via " + targetIp);
                return true; // Connection successful
            } catch (TimeoutException e) {
                System.err.println("❌ Connection to " + targetIp + " timed out.");
            } catch (Exception e) {
                System.err.println("❌ Failed to connect to " + targetIp + ".");
            } finally {
                executor.shutdownNow();
            }
        }
        System.err.println("\n❌ All server connections failed.");
        payrollService = null;
        return false;
    }

    private static <T> T executeWithResilience(Function<PayrollService, T> rmiFunction) {
        Callable<T> timedCall = () -> rmiFunction.apply(payrollService);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(timedCall);
            return future.get(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.err.println("\n⚠️  RMI call failed or timed out. Attempting to reconnect...");
            if (establishConnection()) {
                System.out.println("✅ Reconnected successfully. Retrying the operation...");
                executor.shutdownNow();
                executor = Executors.newSingleThreadExecutor();
                try {
                    Future<T> future = executor.submit(timedCall);
                    return future.get(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (Exception retryException) {
                    System.err.println("❌ Retry failed. The operation could not be completed.");
                    retryException.printStackTrace();
                    return null;
                }
            } else {
                System.err.println("❌ Reconnect failed. Could not find any live servers.");
                return null;
            }
        } finally {
            executor.shutdownNow();
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
        System.out.println("\n--- System Login ---");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        loggedInUser = executeWithResilience(service -> {
            try {
                return service.login(username, password);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        if (loggedInUser != null) {
            System.out.println("\n✅ Login successful! Welcome, " + loggedInUser.getFName() + ".");
        } else {
            System.out.println("❌ Login failed. Invalid credentials or server unavailable.");
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
    //  EMP MENU
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
    //  HR Menu
    // =================================================================

    private static void showHrMenu() {
        while (loggedInUser != null && hasRole("HR")) {
            System.out.println("\n--- HR Administrator Portal ---");

            System.out.println("\n-- Employee Management --");
            System.out.println("1. View All Employees");
            System.out.println("2. Create New Employee");
            System.out.println("3. Edit Employee Details");
            System.out.println("11. Delete Employee");

            System.out.println("\n-- Organization Management --");
            System.out.println("12. Manage Organization Department and Job Titles");

            System.out.println("\n-- Compensation Management --");
            System.out.println("4. Manage Pay Template");

            System.out.println("\n-- Payroll Management --");
            System.out.println("5. View All Payrolls");
            System.out.println("6. Create New Payroll");
            System.out.println("7. View All Bonus Details");

            System.out.println("\n-- Reports Management --");
            System.out.println("8. Create New Report");

            System.out.println("\n");
            System.out.println("9. Switch to My Employee View");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleViewUsers();
                    break;
                case "2":
                    handleCreateUser();
                    break;
                case "3":
                    handleEditUser();
                    break;
                case "4":
                    handleEditCompensation();
                    break;
                case "5":
                    handleViewPayrolls();
                    break;
                case "6":
                    handleCreatePayroll();
                    break;
                case "7":
                    handleViewBonuses();
                    break;
                case "8":
                    handleCreateReport();
//                    handleViewReports();
                    break;
                case "11":
                    handleDeleteUser();
                    break;
                case "12":
                    handleManageOrganization();
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

    private static void handleManageOrganization() {
        System.out.println("\n--- Manage Organization ---");
        System.out.println("1. Create New Department");
        System.out.println("2. Create New Job Title");
        System.out.println("9. Return to Main Menu");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                handleCreateDept();
                break;
            case "2":
                handleCreateJobTitle();
                break;
        }
    }

    private static void handleCreateJobTitle() {
        System.out.println("\n--- Create New Department ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)\n");

        /// 1. Fetch necessary lists
        List<Department> departments = executeWithResilience(s -> { try { return s.getAllDepartments(loggedInUser.getId()); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (departments == null) { System.err.println("❌ Could not load departments. Aborting."); return; }

        List<JobTitle> jobTitles = executeWithResilience(s -> { try { return s.getAllJobTitles(loggedInUser.getId()); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (jobTitles == null) { System.err.println("❌ Could not load job titles. Aborting."); return; }

        // 2. Get the details for the new job title
        while(true) {
            departments.forEach(dept -> System.out.printf("  ID: %d, Name: %s\n", dept.getDeptId(), dept.getDeptName()));
            Integer deptId = promptForInteger("\nEnter the Department ID for a new job title: ", false);
            if (deptId == null) return;

            String title = promptForInput("Enter the job title (e.g., 'Developer'): ", ValidationType.LETTERS_ONLY);
            if (title == null) return;

            String level = promptForInput("Enter the job level (e.g., 'Junior', 'Senior', 'Lead'): ", ValidationType.LETTERS_ONLY);
            if (level == null) return;

            // CLIENT-SIDE VALIDATION: Check for duplicates before calling the server
            final int finalDeptId = deptId;
            final String finalTitle = title;
            final String finalLevel = level;
            if (jobTitles.stream().anyMatch(jt -> jt.getDeptId() == finalDeptId && jt.getTitle().equalsIgnoreCase(finalTitle) && jt.getLevel().equalsIgnoreCase(finalLevel))) {
                System.err.println("❌ Error: That job title and level already exists in the selected department. Please try again.");
                continue; // Ask again from the beginning
            }

            JobTitle newJobTitle = new JobTitle();
            newJobTitle.setDeptId(deptId);
            newJobTitle.setTitle(title);
            newJobTitle.setLevel(level);

            // 3. Make the resilient call to create it
            JobTitle result = executeWithResilience(s -> { try { return s.createJobTitle(loggedInUser.getId(), newJobTitle); } catch (RemoteException e) { throw new RuntimeException(e); }});
            if (result != null) {
                System.out.println("✅ Job Title '" + result.getTitle() + "' created successfully with ID " + result.getId() + ".");
            } else {
                System.out.println("❌ Failed to create job title.");
            }
            break; // Exit the loop on success
        }
    }

    private static void handleCreateDept() {
        System.out.println("\n--- Create New Job Title ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)");

        // 1. Show existing list first to prevent duplicates
        List<Department> departments = executeWithResilience(s -> { try { return s.getAllDepartments(loggedInUser.getId()); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (departments == null) { System.err.println("❌ Could not load departments. Aborting."); return; }
        System.out.println("\nExisting Departments: " + departments.stream().map(Department::getDeptName).collect(Collectors.joining(", ")));

        // 2. Get details for the new department, with validation
        while (true) {
            String deptName = promptForInput("\nEnter new department name: ", ValidationType.LETTERS_ONLY);
            if (deptName == null) return; // User cancelled

            // CLIENT-SIDE VALIDATION: Check for duplicates before calling the server
            final String finalDeptName = deptName;
            if (departments.stream().anyMatch(d -> d.getDeptName().equalsIgnoreCase(finalDeptName))) {
                System.err.println("❌ Error: A department with the name '" + deptName + "' already exists. Please try a different name.");
                continue; // Ask again
            }

            Department newDepartment = new Department();
            newDepartment.setDeptName(deptName);

            // 3. Make the resilient call to create it
            Department result = executeWithResilience(s -> { try { return s.createDepartment(loggedInUser.getId(), newDepartment); } catch (RemoteException e) { throw new RuntimeException(e); }});
            if (result != null) {
                System.out.println("✅ Department '" + result.getDeptName() + "' created successfully with ID " + result.getDeptId() + ".");
            } else {
                System.out.println("❌ Failed to create department. The server may be unavailable.");
            }
            break; // Exit the loop on success
        }
    }

    // =================================================================
    //  MANAGER Menu
    // =================================================================
    private static void showManagerMenu() {
        while (loggedInUser != null && hasRole("MANAGER")) {
            System.out.println("\n--- Manager Dashboard ---");
            System.out.println("1. View My Department Report");
            System.out.println("2. View Pending Bonuses");
            System.out.println("3. Create Bonuses for Employees");
            System.out.println("8. Switch to Employee View");
            System.out.println("9. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleViewDepartmentReport();
                    break;
                case "2":
                    handleApproveBonuses();
                    break;
                case "3":
                    handleCreateBonuses();
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

    private static void handleCreateBonuses() {
        System.out.println("\n--- Assign Bonus to Employee ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)");

        // 1. Show the manager who is on their team for easy selection
        System.out.println("\nFetching your department's employees...");
        List<User> employees = executeWithResilience(s -> { try { return s.getMyDepartmentEmployees(loggedInUser.getId()); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (employees == null || employees.isEmpty()) {
            System.out.println("Could not load your department's employees.");
            return;
        }
        System.out.println("------------------------------------------");
        System.out.printf("| %-4s | %-20s |\n", "ID", "Name");
        System.out.println("------------------------------------------");
        employees.forEach(e -> System.out.printf("| %-4d | %-20s |\n", e.getId(), e.getFName() + " " + e.getLName()));
        System.out.println("------------------------------------------");

        // 2. Get the details for the bonus from the manager
        Integer targetUserId = promptForInteger("Enter the Employee ID to receive the bonus: ", false);
        if (targetUserId == null) return;

        // Client-side validation to ensure the ID is valid
        final int finalTargetUserId = targetUserId;
        if (employees.stream().noneMatch(e -> e.getId() == finalTargetUserId)) {
            System.err.println("❌ Error: That ID does not belong to an employee in your department.");
            return;
        }

        String bonusName = promptForInput("Enter the bonus description (e.g., 'Project Completion Bonus'): ", ValidationType.LETTERS_ONLY);
        if (bonusName == null) return;

        BigDecimal bonusAmount = promptForBigDecimal("Enter the bonus amount (e.g., 500.00): ");
        if (bonusAmount == null) return;

        Integer year = promptForInteger("Enter the Year of the pay period for this bonus: ", false);
        if (year == null) return;
        Integer month = promptForInteger("Enter the Month of the pay period for this bonus: ", false);
        if (month == null) return;

        // 3. Create the Bonus object and send it to the server
        Bonus newBonus = new Bonus();
        newBonus.setUserId(targetUserId);
        newBonus.setName(bonusName);
        newBonus.setAmount(bonusAmount);
        newBonus.setPayPeriodStartDate(LocalDate.of(year, month, 1));

        Bonus result = executeWithResilience(s -> { try { return s.createBonusesToEmployee(loggedInUser.getId(), newBonus); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (result != null) {
            System.out.println("✅ Bonus assigned successfully with ID " + result.getId() + ".");
        } else {
            System.out.println("❌ Failed to assign bonus. Please check the details and try again.");
        }
    }

    // =================================================================
    //  HR Methods
    // =================================================================
    // Employee Management
    private static void handleViewUsers() {
        // We pass a function that takes the 'service' object as input and calls the method.
        List<User> users = executeWithResilience(service -> {
            try {
                return service.readAllUsers(loggedInUser.getId());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

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
    }

    private static void handleCreateUser() {
        try {
            System.out.println("\n--- Create New Employee ---");
            System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)");

            User newUser = new User();

            String fName = promptForInput("\nFirst Name: ", ValidationType.LETTERS_ONLY); // true = letters only
            if (fName == null) return; // User cancelled
            newUser.setFName(fName);

            String lName = promptForInput("Last Name: ", ValidationType.LETTERS_ONLY); // true = letters only
            if (lName == null) return;
            newUser.setLName(lName);

            String username = promptForInput("Username: ", ValidationType.LETTERS_ONLY);
            if (username == null) return;
            newUser.setUsername(username);

            String email = promptForInput("Email: ", ValidationType.LETTERS_ONLY);
            if (email == null) return;
            newUser.setEmail(email);

            String phone = promptForInput("Phone Number (+60): ", ValidationType.PHONE_NUMBER);
            if (phone == null) return;
            newUser.setPhone(phone);

            Integer ic = promptForInteger("IC: ", true);
            if (ic == null) return;
            newUser.setIc(String.valueOf(ic));

            // --- Step 1: FAULT-TOLERANT fetch of lists ---
            List<JobTitle> jobTitles = executeWithResilience(service -> {
                System.out.println("\nLoading available job titles...");
                try {
                    return service.getAllJobTitles(loggedInUser.getId());
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });

            if (jobTitles == null) {
                System.err.println("❌ Could not load job titles. Aborting.");
                return;
            }
            jobTitles.forEach(job -> System.out.printf("ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel()));

            Integer jobTitleId = promptForInteger("\nJob Title ID: ", true);
            if (jobTitleId == null) return;
            newUser.setJobTitleId(jobTitleId);

            List<EmpType> empTypes = executeWithResilience(service -> {
                System.out.println("\nLoading available employment types...");
                try {
                    return service.getAllEmpTypes(loggedInUser.getId());
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            if (empTypes == null) {
                System.err.println("❌ Could not load employment types. Aborting.");
                return;
            }
            empTypes.forEach(type -> System.out.printf("ID: %d, Type: %s\n", type.getId(), type.getName()));

            Integer empTypeId = promptForInteger("\nEmployment Type ID: ", true);
            if (empTypeId == null) return;
            newUser.setEmpTypeId(empTypeId);

            String tempPassword = promptForInput("Temporary Password: ", ValidationType.ANY);
            if (tempPassword == null) return;

            // --- Step 3: FAULT-TOLERANT call to create the user ---
            String response = executeWithResilience(service -> {
                try {
                    return service.createUser(loggedInUser.getId(), newUser, tempPassword);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });

            if (response != null) {
                System.out.println("\n✅ Server Response: " + response);
            } else {
                System.out.println("\n❌ Operation failed. The user was not created.");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number.");
        }
    }

    private static void handleEditUser() {
        System.out.println("\n--- Edit Employee Details ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)");

        try {
            handleViewUsers();

            final Integer targetUserId = promptForInteger("Enter the ID of the user you wish to edit: ", false);
            if (targetUserId == null) return;

            // 1. RESILIENTLY fetch the user's current details
            User userToEdit = executeWithResilience(service -> {
                try {
                    return service.readUserById(loggedInUser.getId(), targetUserId);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            if (userToEdit == null) {
                System.out.println("❌ Error: User with ID " + targetUserId + " not found or server is unavailable.");
                return;
            }

            // 2. Prompt for new details using our VALIDATING helper
            String newFName = promptForUpdate("First Name", userToEdit.getFName(), true);
            if (newFName == null) return; // User cancelled
            userToEdit.setFName(newFName);

            String newLName = promptForUpdate("Last Name", userToEdit.getLName(), true);
            if (newLName == null) return;
            userToEdit.setLName(newLName);

            String newUsername = promptForUpdate("Username", userToEdit.getUsername(), true);
            if (newUsername == null) return;
            userToEdit.setUsername(newUsername);

            String newEmail = promptForUpdate("Email", userToEdit.getEmail(), false);
            if (newEmail == null) return;
            userToEdit.setEmail(newEmail);

            String newPhone = promptForUpdate("Phone Number", userToEdit.getPhone(), true);
            if (newPhone == null) return;
            userToEdit.setPhone(newPhone);

            String newIc = promptForUpdate("IC", userToEdit.getIc(), true);
            if (newIc == null) return;
            userToEdit.setIc(newIc);

            List<JobTitle> jobTitles = executeWithResilience(s -> {
                try {
                    return s.getAllJobTitles(loggedInUser.getId());
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            if (jobTitles != null) {
                System.out.println("\n--- Assign Job Title ---");
                System.out.printf("Current Job Title: %d - %s\n", userToEdit.getJobTitleId(), userToEdit.getJobTitle());
                jobTitles.forEach(job -> System.out.printf("ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel()));
                System.out.print("Enter new Job Title ID (press Enter to keep current): ");
                String newJobIdStr = scanner.nextLine();
                if (newJobIdStr.equalsIgnoreCase("cancel")) {
                    System.out.println("Operation cancelled.");
                    return;
                }
                if (!newJobIdStr.isBlank()) userToEdit.setJobTitleId(Integer.parseInt(newJobIdStr));
            }

            List<EmpType> empTypes = executeWithResilience(s -> {
                try {
                    return s.getAllEmpTypes(loggedInUser.getId());
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            if (empTypes != null) {
                System.out.println("\n--- Assign Employment Type ---");
                System.out.printf("Current Employment Type ID: %d\n", userToEdit.getEmpTypeId());
                empTypes.forEach(type -> System.out.printf("ID: %d, Type: %s\n", type.getId(), type.getName()));
                System.out.print("Enter new Employment Type ID (press Enter to keep current): ");
                String newEmpTypeIdStr = scanner.nextLine();
                if (newEmpTypeIdStr.equalsIgnoreCase("cancel")) {
                    System.out.println("Operation cancelled.");
                    return;
                }
                if (!newEmpTypeIdStr.isBlank()) userToEdit.setEmpTypeId(Integer.parseInt(newEmpTypeIdStr));
            }

            // 3. RESILIENTLY call the update method on the server
            Boolean successObject = executeWithResilience(service -> {
                try {
                    return service.updateUser(loggedInUser.getId(), userToEdit);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });

            // 4. Check if the result is NOT NULL before using its value
            if (successObject != null && successObject) { // Check for null AND then check for true
                System.out.println("\n✅ User details for '" + userToEdit.getUsername() + "' updated successfully.");
            } else {
                System.out.println("\n❌ Failed to update user details. The server may be unavailable or the operation failed.");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid ID. Please enter a number.");
        }
    }

    private static void handleDeleteUser() {
        System.out.println("\n--- Delete Employee ---");
        handleViewUsers(); // Show the list of users first

        final Integer userIdToDelete = promptForInteger("\nEnter the ID of the user to DELETE: ", false);
        if (userIdToDelete == null) return;

        // Fetch the user's name for the confirmation message
        User userToDelete = executeWithResilience(s -> { try { return s.readUserById(loggedInUser.getId(), userIdToDelete); } catch (RemoteException e) { throw new RuntimeException(e); }});
        if (userToDelete == null) {
            System.err.println("❌ User with ID " + userIdToDelete + " not found.");
            return;
        }

        // Add a strong confirmation
        System.out.println("\nWARNING: You are about to permanently delete the following user:");
        System.out.printf("  ID: %d, Name: %s %s\n", userToDelete.getId(), userToDelete.getFName(), userToDelete.getLName());
        System.out.println("\nThis action cannot be undone.");
        System.out.print("\nType the user's username ('" + userToDelete.getUsername() + "') to confirm: ");
        String confirmation = scanner.nextLine();

        if (confirmation.equals(userToDelete.getUsername())) {
            Boolean success = executeWithResilience(s -> { try { return s.deleteUser(loggedInUser.getId(), userIdToDelete); } catch (RemoteException e) { throw new RuntimeException(e); }});
            if (success != null && success) {
                System.out.println("✅ User successfully deleted.");
            } else {
                System.out.println("❌ User deletion failed.");
            }
        } else {
            System.out.println("Confirmation text did not match. Deletion cancelled.");
        }
    }

    // Compensation Management
    private static void handleEditCompensation() {
        System.out.println("\n--- Manage Compensation Rules ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)\n");

        // Step 1: Resiliently fetch and display job titles
        List<JobTitle> jobTitles = executeWithResilience(s -> {
            try {
                return s.getAllJobTitles(loggedInUser.getId());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        if (jobTitles == null) {
            System.err.println("❌ Could not load job titles. Aborting.");
            return;
        }
        jobTitles.forEach(job -> System.out.printf("ID: %d, Title: %s (%s)\n", job.getId(), job.getTitle(), job.getLevel()));

        // Step 2: Get a valid job title ID from the user
        final Integer jobTitleId = promptForInteger("\nEnter the ID of the job title to manage: ", false);
        if (jobTitleId == null) return;

        while (true) {
            List<PayTemplate> templates = executeWithResilience(s -> {
                try {
                    return s.getPayTemplatesForJobTitle(loggedInUser.getId(), jobTitleId);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
            if (templates == null) {
                System.err.println("❌ Could not load compensation package.");
                break;
            }

            System.out.println("\nCurrent Compensation Package for Job ID " + jobTitleId + ":");
            templates.forEach(item -> System.out.printf("  Item ID: %-4d | Type: %-10s | Desc: %-25s | Amount: RM %.2f\n",
                    item.getId(), item.getType(), item.getDescription(), item.getAmount()));

            System.out.println("\nWhat would you like to do?");
            System.out.println("1. Update an existing item's amount");
            System.out.println("2. Add a new item to this package");
            System.out.println("3. Delete an item from this package");
            System.out.println("\n9. Return to HR Menu");
            System.out.print("\nChoose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1": { // UPDATE
                    Integer updateId = promptForInteger("Enter the Item ID to update: ", false);
                    if (updateId == null) break;

                    final int finalUpdateId = updateId;
                    if (templates.stream().noneMatch(t -> t.getId() == finalUpdateId)) {
                        System.err.println("❌ Error: Item ID " + updateId + " is not a valid choice.");
                        break;
                    }

                    BigDecimal newAmount = promptForBigDecimal("Enter the new Amount (e.g., 650.00): ");
                    if (newAmount == null) break;

                    Boolean success = executeWithResilience(s -> {
                        try {
                            return s.updatePayTemplateItem(loggedInUser.getId(), finalUpdateId, newAmount);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    if (success != null && success) System.out.println("✅ Item updated successfully.");
                    else System.out.println("❌ Update failed.");
                    break;
                }
                case "2": { // ADD
                    System.out.println("\n--- Add New Compensation Item ---");
                    PayTemplate newItem = new PayTemplate();
                    newItem.setJobTitleId(jobTitleId);

                    String desc = promptForInput("Description (e.g., Car Allowance): ", ValidationType.LETTERS_ONLY);
                    if (desc == null) break;
                    newItem.setDescription(desc);

                    PayItemType type = promptForPayItemType("Type ");
                    if (type == null) break;
                    newItem.setType(type);

                    BigDecimal amount = promptForBigDecimal("Amount (e.g., 400.00): ");
                    if (amount == null) break;
                    newItem.setAmount(amount);

                    PayTemplate result = executeWithResilience(s -> {
                        try {
                            return s.addPayTemplateItem(loggedInUser.getId(), newItem);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    if (result != null) System.out.println("✅ New item added successfully.");
                    else System.out.println("❌ Failed to add item.");
                    break;
                }
                case "3": { // DELETE
                    Integer deleteId = promptForInteger("Enter the Item ID to delete: ", false);
                    if (deleteId == null) break;

                    final int finalDeleteId = deleteId;
                    if (templates.stream().noneMatch(t -> t.getId() == finalDeleteId)) {
                        System.err.println("❌ Error: Item ID " + deleteId + " is not a valid choice.");
                        break;
                    }

                    Boolean success = executeWithResilience(s -> {
                        try {
                            return s.deletePayTemplateItem(loggedInUser.getId(), finalDeleteId);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    if (success != null && success) System.out.println("✅ Item deleted successfully.");
                    else System.out.println("❌ Delete failed.");
                    break;
                }
                case "9":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void handleViewPayrolls() {
        System.out.println("\n--- View Payroll History ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)\n");

        // Step 1: Resiliently fetch and display the list of all payslip summaries.
        System.out.println("Fetching all generated payslips...");
        List<PayslipSummary> summaries = executeWithResilience(service -> {
            try { return service.getAllPayslips(loggedInUser.getId()); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (summaries == null) {
            System.err.println("❌ Could not retrieve payslip list. Server may be unavailable.");
            return;
        }
        if (summaries.isEmpty()) {
            System.out.println("No payslips have been generated yet.");
            return;
        }

        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("| %-10s | %-8s | %-20s | %-12s | %-15s |%n", "Payslip ID", "User ID", "Employee Name", "Pay Period", "Net Pay (RM)");
        System.out.println("-------------------------------------------------------------------------------");
        for (PayslipSummary summary : summaries) {
            System.out.printf("| %-10d | %-8d | %-20s | %-12s | %15.2f |%n",
                    summary.getPayslipId(), summary.getUserId(), summary.getUserFullName(),
                    summary.getPayPeriodStartDate().toString().substring(0, 7), summary.getNetPay());
        }
        System.out.println("-------------------------------------------------------------------------------");

        // Step 2: Prompt the user to select one by its ID.
        Integer payslipId = promptForInteger("\nEnter the Payslip ID to view its full details: ", false);
        if (payslipId == null) return; // User cancelled or entered invalid input

        // Step 3: Resiliently fetch the full payslip object by its ID.
        final int finalPayslipId = payslipId;
        Payslip payslip = executeWithResilience(service -> {
            try { return service.getPayslipById(loggedInUser.getId(), finalPayslipId); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (payslip == null) {
            System.out.println("❌ Error: Payslip with ID " + payslipId + " not found.");
            return;
        }

        // Step 4: Fetch the associated user's details and print the payslip.
        final int targetUserId = payslip.getUserId();
        User targetUser = executeWithResilience(service -> {
            try { return service.readUserById(loggedInUser.getId(), targetUserId); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (targetUser != null) {
            printPayslip(payslip, targetUser);
        }
    }

    private static void runPayrollFor(int targetUserId) {
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)\n");

        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        final Integer year = promptForIntegerWithDefault("Enter the Year for the payroll run", currentYear);
        if (year == null) return;

        final Integer month = promptForIntegerWithDefault("Enter the Month for the payroll run (1-12)", currentMonth);
        if (month == null) return;

        System.out.println("\nWARNING: This is a critical action and cannot be undone.");
        Integer confirmation = promptForInteger("Enter '1' to proceed, or '0' to cancel: ", false);

        if (confirmation != null && confirmation == 1) {

            // REVISED: Password verification now has a retry loop (max 3 attempts)
            boolean passwordVerified = false;
            for (int i = 0; i < 3; i++) {
                System.out.print("For security, please re-enter your password: ");
                String password = scanner.nextLine();

                Boolean passwordCorrect = executeWithResilience(service -> {
                    try { return service.verifyCurrentUserPassword(loggedInUser.getId(), password); }
                    catch (RemoteException e) { throw new RuntimeException(e); }
                });

                if (passwordCorrect != null && passwordCorrect) {
                    passwordVerified = true;
                    break; // Exit the loop on success
                } else {
                    System.err.printf("❌ Invalid password. You have %d attempt(s) remaining.\n", 2 - i);
                }
            }

            if (passwordVerified) {
                System.out.println("Password verified. Processing payroll...");
                String result = executeWithResilience(service -> {
                    try { return service.runMonthlyPayrollForTarget(loggedInUser.getId(), year, month, targetUserId); }
                    catch (RemoteException e) { throw new RuntimeException(e); }
                });

                if (result != null) {
                    System.out.println("\n✅ Server Response: " + result);
                } else {
                    System.out.println("\n❌ Payroll run failed. The server may be unavailable.");
                }
            } else {
                System.out.println("Too many incorrect password attempts. Payroll run cancelled.");
            }
        } else {
            System.out.println("Payroll run cancelled.");
        }
    }

    private static void handleCreatePayroll() {
        // RESILIENT CALL to check for pending items
        List<Bonus> pendingBonuses = executeWithResilience(service -> {
            try {
                return service.getAllPendingBonuses(loggedInUser.getId());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        // Check for null in case the server call failed
        if (pendingBonuses == null) {
            System.err.println("❌ Could not check for pending approvals. Server may be unavailable. Aborting.");
            return;
        }

        if (!pendingBonuses.isEmpty()) {
            String redColor = "\u001B[31m";
            String resetColor = "\u001B[0m";

            System.out.println("\n" + redColor + "================== WARNING ==================" + resetColor);
            System.out.printf(redColor + "There are %d bonus approvals still pending." + resetColor + "\n", pendingBonuses.size());
            System.out.println(redColor + "Running payroll now may result in underpayment for some employees." + resetColor);
            System.out.println("==========================================");

            Integer proceed = promptForInteger("Enter '1' to proceed or '0' to cancel: ", false);
            if (proceed == null || proceed != 1) {
                System.out.println("Payroll operation cancelled. Please clear pending approvals first.");
                return;
            }
        }

        System.out.println("\n--- Payroll Operations ---");
        System.out.println("1. Run Full Payroll Cycle for ALL Employees");
        System.out.println("2. Run Payroll for a SINGLE Employee");
        System.out.println("9. Return");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            runPayrollFor(0);
        } else if (choice.equals("2")) {
            Integer targetUserId = promptForInteger("Enter the User ID of the employee to process: ", true);
            if (targetUserId != null) {
                runPayrollFor(targetUserId);
            }
        }
    }

    private static void handleViewBonuses() {
        System.out.println("\n--- Report: All Pending Bonus Approvals ---");

        // RESILIENT CALL to the server
        List<Bonus> pendingBonuses = executeWithResilience(service -> {
            try {
                return service.getAllPendingBonuses(loggedInUser.getId());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        // Check for null in case the server call failed completely
        if (pendingBonuses == null) {
            System.err.println("❌ Could not retrieve pending approvals. Server may be unavailable.");
            return;
        }

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

    private static void handleCreateReport() {
        List<String> periods = executeWithResilience(service -> {
            try { return service.getExistingPayrollPeriods(loggedInUser.getId()); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });
        if (periods == null || periods.isEmpty()) {
            System.out.println("No payroll data found. Please run a payroll cycle first.");
            return;
        }
        System.out.println("Available report periods: " + String.join(", ", periods));

        // 2. Get user input for the desired period
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        final Integer year = promptForIntegerWithDefault("\nEnter the Year for the report", currentYear);
        if (year == null) return;
        final Integer month = promptForIntegerWithDefault("Enter the Month for the report (1-12)", currentMonth);
        if (month == null) return;

        // 3. Fetch the report data from the server ONE TIME
        System.out.println("\nGenerating report... please wait.");
        PayrollSummaryReport report = executeWithResilience(service -> {
            try { return service.getPayrollSummaryReport(loggedInUser.getId(), year, month); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (report == null || report.getNumberOfEmployeesPaid() == 0) {
            System.out.println("No payroll data found for the specified period.");
            return;
        }

        // 4. Display the report on screen using our helper
        displaySummaryReport(report, year, month);

        // 5. Ask the user if they want to export the report they just saw
        System.out.print("\nWould you like to export this report to a CSV file? (y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            exportSummaryReport(report, year, month);
        }
    }

    private static void displaySummaryReport(PayrollSummaryReport report, int year, int month) {
        System.out.println("\n=======================================================");
        System.out.printf("      Payroll Summary Report for %d-%02d\n", year, month);
        System.out.println("=======================================================");
        System.out.printf(" Employees Paid      : %d\n", report.getNumberOfEmployeesPaid());
        System.out.printf(" Total Gross Earnings: RM %,.2f\n", report.getTotalGrossEarnings());
        System.out.printf(" Total Deductions    : RM %,.2f\n", report.getTotalDeductions());
        System.out.printf(" Total Net Pay       : RM %,.2f\n", report.getTotalNetPay());
        System.out.println();
        System.out.printf(" Est. Employer Contrib.: RM %,.2f (e.g., EPF)\n", report.getEstimatedEmployerContributions());
        System.out.println("-------------------------------------------------------");
        System.out.printf(" TOTAL COMPANY PAYOUT  : RM %,.2f\n", report.getTotalCompanyPayout());
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
    }

    private static void exportSummaryReport(PayrollSummaryReport report, int year, int month) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Category,Value\n");
        csvBuilder.append("Pay Period,").append(String.format("%d-%02d", year, month)).append("\n");
        csvBuilder.append("Employees Paid,").append(report.getNumberOfEmployeesPaid()).append("\n");
        csvBuilder.append("Total Gross Earnings,").append(report.getTotalGrossEarnings()).append("\n");
        csvBuilder.append("Total Deductions,").append(report.getTotalDeductions()).append("\n");
        csvBuilder.append("Total Net Pay,").append(report.getTotalNetPay()).append("\n");
        csvBuilder.append("Estimated Employer Contributions,").append(report.getEstimatedEmployerContributions()).append("\n");
        csvBuilder.append("Total Company Payout,").append(report.getTotalCompanyPayout()).append("\n");
        csvBuilder.append("\nDepartment,Net Pay (RM)\n");
        for (Map.Entry<String, BigDecimal> entry : report.getNetPayByDepartment().entrySet()) {
            csvBuilder.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
        }

        try {
            String fileName = String.format("payroll_summary_%d-%02d.csv", year, month);
            java.io.FileWriter writer = new java.io.FileWriter(fileName);
            writer.write(csvBuilder.toString());
            writer.close();
            System.out.println("✅ Report successfully exported to file: " + fileName);
        } catch (java.io.IOException e) {
            System.err.println("❌ An error occurred while writing the file: " + e.getMessage());
        }
    }

    // =================================================================
    //  MANAGER Methods
    // =================================================================
    private static void handleViewDepartmentReport() {
        System.out.println("\nFetching your department report...");

        // RESILIENT CALL
        List<User> employees = executeWithResilience(service -> {
            try { return service.getMyDepartmentEmployees(loggedInUser.getId()); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (employees == null) {
            System.err.println("❌ Could not retrieve department report. Server may be unavailable.");
            return;
        }

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
    }

    private static void handleApproveBonuses() {
        System.out.println("\n--- Approve Pending Bonuses ---");
        System.out.println("(Type '0' OR ':e' for exit OR ':q' for quit at any time to return to the menu)\n");

        // 1. Resiliently fetch the list of unapproved bonuses
        List<Bonus> pendingBonuses = executeWithResilience(service -> {
            try { return service.getUnapprovedBonusesForMyDepartment(loggedInUser.getId()); }
            catch (RemoteException e) { throw new RuntimeException(e); }
        });

        if (pendingBonuses == null) {
            System.err.println("❌ Could not retrieve pending bonuses. Server may be unavailable.");
            return;
        }
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

        // 3. Prompt the manager for a clearer action
        System.out.print("Enter Bonus ID(s) to approve (e.g., '1', '1,3', or 'all'): ");
        String input = scanner.nextLine();
        if (input.equalsIgnoreCase("0") || input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.isBlank()) {
            System.out.println("Approval process cancelled.");
            return;
        }

        List<Integer> idsToApprove = new ArrayList<>();
        if (input.equalsIgnoreCase("all")) {
            pendingBonuses.forEach(b -> idsToApprove.add(b.getId()));
        } else {
            try {
                Arrays.stream(input.split(",")).forEach(idStr -> idsToApprove.add(Integer.parseInt(idStr.trim())));
            } catch (NumberFormatException e) {
                System.err.println("❌ Invalid format. Please enter numbers separated by commas, or 'all'.");
                return;
            }
        }

        // 4. Loop through the selected IDs and approve them one by one
        Set<Integer> validIds = pendingBonuses.stream().map(Bonus::getId).collect(Collectors.toSet());
        List<Integer> invalidIds = new ArrayList<>();
        for (int id : idsToApprove) {
            if (!validIds.contains(id)) {
                invalidIds.add(id);
            }
        }

        if (!invalidIds.isEmpty()) {
            System.err.println("❌ Error: The following Bonus IDs are not valid or not pending approval: " + invalidIds);
            System.out.println("Approval process cancelled.");
            return;
        }

        System.out.println("\nYou are about to approve the following " + idsToApprove.size() + " bonus(es):");
        for (Bonus bonus : pendingBonuses) {
            if (idsToApprove.contains(bonus.getId())) {
                System.out.printf("  - ID: %d, Employee: %s, Amount: RM %.2f\n", bonus.getId(), bonus.getEmployeeName(), bonus.getAmount());
            }
        }
        Integer confirmation = promptForInteger("\nEnter '1' to confirm, or '0' to cancel: ", false);
        if (confirmation == null || confirmation != 1) {
            System.out.println("Approval process cancelled.");
            return;
        }

        int successCount = 0;
        for (int bonusId : idsToApprove) {
            final int finalBonusId = bonusId;
            Boolean success = executeWithResilience(service -> {
                try { return service.approveBonus(loggedInUser.getId(), finalBonusId); }
                catch (RemoteException e) { throw new RuntimeException(e); }
            });
            if (success != null && success) {
                System.out.println("  - Bonus ID " + bonusId + " approved.");
                successCount++;
            } else {
                System.out.println("  - Failed to approve Bonus ID " + bonusId + ".");
            }
        }
        System.out.println("\n✅ Approval process complete. " + successCount + " of " + idsToApprove.size() + " bonuses approved.");
    }

    // =================================================================
    //  EMP Methods
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

            if (bankName.isBlank() || accNo.isBlank() || accName.isBlank()) {
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

    private static String promptForInput(String prompt, ValidationType type) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();

            // CHECK FOR CANCEL: The user can exit by typing 'cancel'
            if (input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.equalsIgnoreCase("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            if (input.isBlank()) {
                System.err.println("❌ Input cannot be empty.");
                continue;
            }

            if (type == ValidationType.LETTERS_ONLY && !input.matches("[a-zA-Z ]+")) {
                System.err.println("❌ Invalid input. Please use letters and spaces only.");
                continue;
            }

            if (type == ValidationType.PHONE_NUMBER && !input.matches("[0-9+\\-() ]+")) {
                System.err.println("❌ Invalid input. Phone numbers should only contain digits and symbols like '+ - ( )'.");
                continue;
            }

            return input; // Input is valid
        }
    }

    private static String promptForUpdate(String prompt, String currentValue, boolean lettersOnly) {
        while (true) {
            System.out.printf("%s [%s]: ", prompt, currentValue);
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.equalsIgnoreCase("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }
            if (input.isBlank()) {
                return currentValue; // User pressed Enter, so keep the old value
            }
            if (lettersOnly && !input.matches("[a-zA-Z ]+")) {
                System.err.println("❌ Invalid input. Please use letters and spaces only.");
                continue; // Ask again
            }
            return input; // Return the new, valid input
        }
    }

    private static Integer promptForInteger(String prompt, boolean optional) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.equalsIgnoreCase("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            if (optional && input.isBlank()) {
                System.err.println("❌ Input cannot be empty.");
                continue;
            }

            if (!input.matches("[0-9]+")) {
                System.err.println("❌ Invalid input. Please enter a whole number.");
                continue; // Re-prompt the user
            }

            return Integer.parseInt(input);
        }
    }

    private enum ValidationType {
        ANY,
        LETTERS_ONLY,
        PHONE_NUMBER
    }

    private static PayItemType promptForPayItemType(String prompt) {
        while (true) {
            // The prompt now shows the user the number options
            System.out.print(prompt + " ('1' = EARNING, '2' = DEDUCTION): ");
            String input = scanner.nextLine().toUpperCase();

            if (input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.equalsIgnoreCase("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            // Check for the new number-based inputs
            if (input.equals("1")) {
                return PayItemType.EARNING;
            }
            if (input.equals("2")) {
                return PayItemType.DEDUCTION;
            }

            // Also check for the original word-based inputs
            try {
                return PayItemType.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.err.println("❌ Invalid type. Please enter '1', '2', 'EARNING', or 'DEDUCTION'.");
            }
        }
    }

    private static BigDecimal promptForBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase(":q") || input.equalsIgnoreCase(":e") || input.equalsIgnoreCase("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.err.println("❌ Invalid input. Please enter a valid number (e.g., 500.00).");
            }
        }
    }

    private static Integer promptForIntegerWithDefault(String prompt, int defaultValue) {
        while (true) {
            // The prompt now displays the default value in brackets
            System.out.printf("%s [%d]: ", prompt, defaultValue);
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase(":q")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            // If the user just presses Enter, return the default value
            if (input.isBlank()) {
                return defaultValue;
            }

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.err.println("❌ Invalid input. Please enter a number.");
            }
        }
    }
}
