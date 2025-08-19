package com.wipro.payroll.common;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PayrollService extends Remote {

    // =================================================================
    //  Authentication & Session
    // =================================================================
    User login(String username, String password) throws RemoteException;
    boolean verifyCurrentUserPassword(int actorUserId, String password) throws RemoteException;
    Payslip getPayslipById(int actorUserId, int payslipId) throws RemoteException;


    // --- Employee Self-Service ---
    UserProfile getMyProfile(int userId) throws RemoteException;
    Payslip getMyLatestPayslip(int userId) throws RemoteException;
    UserBankDetails getMyBankDetails(int userId) throws RemoteException;
    boolean updateMyBankDetails(int userId, UserBankDetails details) throws RemoteException;

    // =================================================================
    //  HR Administrator Methods
    // =================================================================
    String createUser(int userId, User newUser, String password) throws RemoteException;
    List<User> readAllUsers(int userId) throws RemoteException;
    boolean updateUser(int userId, User userToUpdate) throws RemoteException;
    User readUserById(int userId, int targetUserId) throws RemoteException;
//    boolean deleteUser(int userId, int userIdToDelete) throws RemoteException;

//    List<UserBankDetails> readAllBankDetails(int userId) throws RemoteException;
    String runMonthlyPayrollForTarget(int actorUserId, int year, int month, int targetUserId) throws RemoteException;
    String runMonthlyPayroll(int actorUserId, int year, int month) throws RemoteException;
    Payslip getPayslipForUser(int actorUserId, int targetUserId, int year, int month) throws RemoteException;
    List<PayslipSummary> getAllPayslips(int actorUserId) throws RemoteException;


    // --- Payroll & Compensation (HR Role) ---
    List<PayTemplate> getPayTemplatesForJobTitle(int actorUserId, int jobTitleId) throws RemoteException;
    boolean updatePayTemplateItem(int actorUserId, int payTemplateItemId, BigDecimal newAmount) throws RemoteException;
    List<String> getExistingPayrollPeriods(int actorUserId) throws RemoteException;


    List<JobTitle> getAllJobTitles(int userId) throws RemoteException;
    List<EmpType> getAllEmpTypes(int userId) throws RemoteException;

    List<Bonus> getAllPendingBonuses(int actorUserId) throws RemoteException;



    //    Pay Template
//    List<PayTemplate> readPayTemplatesForJobTitle(int userId, int jobTitleId) throws RemoteException;
//    boolean updatePayTemplateItem(int userId, int payTemplateItemId, BigDecimal newAmount) throws RemoteException;
    PayTemplate addPayTemplateItem(int userId, PayTemplate newItem) throws RemoteException;
    boolean deletePayTemplateItem(int userId, int payTemplateItemId) throws RemoteException;

    // =================================================================
    //  MANAGER Administrator Methods
    // =================================================================
    List<User> getMyDepartmentEmployees(int actorUserId) throws RemoteException;
    PayrollSummaryReport getPayrollSummaryReport(int actorUserId, int year, int month) throws RemoteException;
    List<Bonus> getUnapprovedBonusesForMyDepartment(int actorUserId) throws RemoteException;
    boolean approveBonus(int actorUserId, int bonusId) throws RemoteException;

    String getPayrollSummaryAsCsv(int id, int year, int month) throws RemoteException;
}
