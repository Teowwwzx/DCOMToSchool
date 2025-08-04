//package com.wipro.payroll.client; // Or server, the package doesn't matter for this test
//
//// Import all of your data model classes
//import com.wipro.payroll.common.*;
//
//public class ModelSanityCheck {
//
//    public static void main(String[] args) {
//        System.out.println("--- Running Sanity Check on Data Models ---");
//
//        try {
//            // We will try to create a 'new' instance of every single model class.
//            // If any of them are missing a no-argument constructor, this will fail.
//
//            System.out.println("Checking BASIC models...");
//            new Department();
//            new Role();
//            new User();
//            // We don't need to test enums
//
//            System.out.println("Checking JOB models...");
//            new EmploymentType();
//            new JobTitle();
//            new PayTemplate();
//
//            System.out.println("Checking PAYSLIP models...");
//            new Payslip();
//            new PayItem();
//
//            System.out.println("Checking Non-recurring payment models...");
//            new Bonus();
////            new LeaveRecord();
//            new Attendance();
//
//            System.out.println("Checking UTILITY models...");
//            new UserBankDetails();
//            new UserAddress();
//
//            System.out.println("\n✅ SUCCESS: All data model classes have a valid no-argument constructor.");
//
//        } catch (Exception e) {
//            System.err.println("\n❌ FAILURE: An error occurred while creating a model instance.");
//            System.err.println("This likely means a class is missing a public no-argument constructor.");
//            e.printStackTrace();
//        }
//    }
//}