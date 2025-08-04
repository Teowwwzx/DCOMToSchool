import java.sql.*;
import java.util.*;

public class PayrollCLI {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Simulate a login result:
        String role = "HR"; // or "HR"
        UUID employeeId = null;

        if (role.equalsIgnoreCase("HR")) {
            System.out.println("\n🔐 HR Payroll Page");
            hrMode(scanner);
        } else if (role.equalsIgnoreCase("Employee")) {
            System.out.println("\n👤 Employee Payroll Page");
            employeeMode(scanner);
        } else {
            System.out.println("❌ Unknown role. Access denied.");
        }
    }


    private static void hrMode(Scanner scanner) {
        while (true) {
            System.out.println("\n🔐 HR Payroll Page");
            System.out.println("[1] Set Payroll");
            System.out.println("[2] View Employee Payroll");
            System.out.println("[0] Return to Main Menu");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    setEmployeePayroll(scanner);
                    break;
                case "2":
                    viewEmployeePayroll(scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("❌ Invalid choice. Please select 1, 2, or 0.");
            }
        }
    }

    private static void setEmployeePayroll(Scanner scanner) {
        try (Connection conn = getConnection()) {
            System.out.println("\n📋 Employee List:");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT employee_id, first_name, last_name FROM employees");

            Map<Integer, UUID> empMap = new HashMap<>();
            int index = 1;

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("employee_id"));
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                System.out.println("[" + index + "] " + name + " (" + id + ")");
                empMap.put(index++, id);
            }

            UUID employeeId = null;
            while (true) {
                try {
                    System.out.print("\nSelect employee number (or 0 to return to HR menu): ");
                    int empChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (empChoice == 0) return;

                    if (empMap.containsKey(empChoice)) {
                        employeeId = empMap.get(empChoice);
                        break;
                    } else {
                        System.out.println("❌ Invalid selection. Please enter a valid number from the list.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Invalid input. Please enter a number.");
                }
            }

            double gross = getValidDouble(scanner, "Enter gross pay (RM): ", v -> v > 0, "❌ Gross pay must be a positive number.");
            double deductionRate = getValidDouble(scanner, "Enter deduction rate (e.g., 0.12): ", v -> v >= 0 && v <= 1, "❌ Must be between 0 and 1.");
            double taxRate = getValidDouble(scanner, "Enter tax rate (e.g., 0.08): ", v -> v >= 0 && v <= 1, "❌ Must be between 0 and 1.");

            double deductions = gross * deductionRate;
            double taxes = gross * taxRate;
            double net = gross - deductions - taxes;

            UUID runId = UUID.randomUUID();
            PreparedStatement runStmt = conn.prepareStatement(
                    "INSERT INTO payrollruns (payroll_run_id, payroll_period_start_date, payroll_period_end_date, pay_date, status, processing_date, created_at, updated_at) " +
                            "VALUES (?, CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, 'Open', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
            runStmt.setObject(1, runId);
            runStmt.executeUpdate();

            PreparedStatement payStmt = conn.prepareStatement(
                    "INSERT INTO payrolldetails (payroll_detail_id, payroll_run_id, employee_id, gross_pay, total_deductions, total_taxes, net_pay, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                            "ON CONFLICT (employee_id) DO UPDATE SET " +
                            "gross_pay = EXCLUDED.gross_pay, total_deductions = EXCLUDED.total_deductions, total_taxes = EXCLUDED.total_taxes, " +
                            "net_pay = EXCLUDED.net_pay, updated_at = CURRENT_TIMESTAMP"
            );

            payStmt.setObject(1, UUID.randomUUID());
            payStmt.setObject(2, runId);
            payStmt.setObject(3, employeeId);
            payStmt.setDouble(4, gross);
            payStmt.setDouble(5, deductions);
            payStmt.setDouble(6, taxes);
            payStmt.setDouble(7, net);
            payStmt.executeUpdate();

            System.out.printf("✅ Payroll saved. Net Pay: RM %.2f%n", net);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.print("\nDo you want to enter payroll for another employee? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            setEmployeePayroll(scanner);
        }
    }


    private static void viewEmployeePayroll(Scanner scanner) {
        try (Connection conn = getConnection()) {
            System.out.println("\n📋 Employee List:");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT employee_id, first_name, last_name FROM employees");

            Map<Integer, UUID> empMap = new HashMap<>();
            int index = 1;

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("employee_id"));
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                System.out.println("[" + index + "] " + name + " (" + id + ")");
                empMap.put(index++, id);
            }

            UUID empId = null;
            while (true) {
                try {
                    System.out.print("\nSelect employee number (or 0 to cancel): ");
                    int choice = Integer.parseInt(scanner.nextLine().trim());
                    if (choice == 0) return;

                    if (empMap.containsKey(choice)) {
                        empId = empMap.get(choice);
                        break;
                    } else {
                        System.out.println("❌ Invalid selection. Please enter a valid number.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Invalid input. Please enter a number.");
                }
            }

            PreparedStatement stmt2 = conn.prepareStatement(
                    "SELECT gross_pay, total_deductions, total_taxes, net_pay " +
                            "FROM payrolldetails WHERE employee_id = ? ORDER BY updated_at DESC LIMIT 1"
            );
            stmt2.setObject(1, empId);
            ResultSet rs2 = stmt2.executeQuery();

            if (rs2.next()) {
                System.out.println("\n📄 Latest Payroll for Employee:");
                System.out.printf("Gross Pay   : RM %.2f\n", rs2.getDouble("gross_pay"));
                System.out.printf("Deductions  : RM %.2f\n", rs2.getDouble("total_deductions"));
                System.out.printf("Taxes       : RM %.2f\n", rs2.getDouble("total_taxes"));
                System.out.printf("Net Pay     : RM %.2f\n", rs2.getDouble("net_pay"));
            } else {
                System.out.println("❌ No payroll record found for this employee.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.print("\nPress Enter to return...");
        scanner.nextLine();
    }



    private static double getValidDouble(Scanner scanner, String prompt, java.util.function.Predicate<Double> validator, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (validator.test(value)) {
                    return value;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a valid decimal number.");
            }
        }
    }


    private static void employeeMode(Scanner scanner) {
        while (true) {
            UUID empId = null;

            while (true) {
                System.out.print("Enter your employee ID (or 0 to return): ");
                String input = scanner.nextLine().trim();

                if (input.equals("0")) return;

                try {
                    empId = UUID.fromString(input);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("❌ Invalid UUID format. Please enter a valid UUID (e.g., 00000000-0000-0000-0000-000000000010).");
                }
            }

            try (Connection conn = getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT gross_pay, total_deductions, total_taxes, net_pay FROM payrolldetails " +
                                "WHERE employee_id = ? ORDER BY updated_at DESC LIMIT 1"
                );
                stmt.setObject(1, empId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    System.out.println("\n📄 Latest Payroll:");
                    System.out.printf("Gross Pay   : RM %.2f\n", rs.getDouble("gross_pay"));
                    System.out.printf("Deductions  : RM %.2f\n", rs.getDouble("total_deductions"));
                    System.out.printf("Taxes       : RM %.2f\n", rs.getDouble("total_taxes"));
                    System.out.printf("Net Pay     : RM %.2f\n", rs.getDouble("net_pay"));
                } else {
                    System.out.println("❌ No payroll record found for this employee.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.print("\nPress Enter to return to the main menu...");
            scanner.nextLine();
            break;
        }
    }


    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(
                "jdbc:postgresql://ep-withered-flower-a811hc2n-pooler.eastus2.azure.neon.tech:5432/neondb?sslmode=require&channel_binding=require ",
                "neondb_owner",
                "npg_4A1VduWMcaYT"
        );
    }
}
