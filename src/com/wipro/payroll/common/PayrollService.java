package com.wipro.payroll.common;

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
}
