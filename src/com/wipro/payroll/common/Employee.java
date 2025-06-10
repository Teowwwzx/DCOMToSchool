package com.wipro.payroll.common;

import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private int employeeId;
    private String username;
    private String firstName;
    private String lastName;
    private String icPassportNumber;

    public Employee() {}

    // Getters and Setters for all fields (IntelliJ can generate these for you: Alt + Insert)
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
}
