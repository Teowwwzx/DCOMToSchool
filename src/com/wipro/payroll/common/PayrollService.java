package com.wipro.payroll.common;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PayrollService extends Remote {

    User login(String username, String password) throws RemoteException;
    String registerUser(User newUser, String password) throws RemoteException;

    Payslip getMyLatestPayslip(int userId) throws RemoteException;

    UserBankDetails getMyBankDetails(int userId) throws RemoteException;
    boolean updateMyBankDetails(int userId, UserBankDetails details) throws RemoteException;

    String runMonthlyPayroll(int adminUserId) throws RemoteException;
    List<User> getAllUsers(int adminUserId) throws RemoteException;

    // Add these to PayrollService.java

    /**
     * Retrieves a list of all job titles from the database.
     * Requires HR privileges.
     * @param adminUserId The ID of the HR user making the request.
     * @return A list of JobTitle objects.
     */
    List<JobTitle> getAllJobTitles(int adminUserId) throws RemoteException;

    /**
     * Updates the amount for a specific item in a pay template.
     * Requires HR privileges.
     * @param adminUserId The ID of the HR user making the request.
     * @param payTemplateItemId The ID of the pay_template_items record to update.
     * @param newAmount The new salary or allowance amount.
     * @return true if the update was successful.
     */
    boolean updatePayTemplateItem(int adminUserId, int payTemplateItemId, BigDecimal newAmount) throws RemoteException;

    List<PayTemplate> getPayTemplatesForJobTitle(int id, int jobTitleId) throws RemoteException;
}
