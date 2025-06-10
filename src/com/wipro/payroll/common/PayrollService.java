package com.wipro.payroll.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PayrollService extends Remote {
    String registerEmployee(String firstName, String lastName, String icNumber, String username, String password) throws RemoteException;

    Employee loginEmployee(String username, String password) throws RemoteException;
}
