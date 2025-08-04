package com.wipro.payroll.common;

import java.io.Serializable;
import java.util.List;

/**
 * A Data Transfer Object (DTO) that holds all the aggregated information
 * for a user's profile screen. This allows us to fetch everything in one RMI call.
 */
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    // Basic user info
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phone;

    // Joined info
    private String departmentName;
    private String jobTitle;
    private String employmentType;

    // Related objects
    private UserBankDetails bankDetails;
    private List<Payslip> payslipHistory; // Assuming Payslip.java exists

    // --- Getters and Setters for all fields ---

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public UserBankDetails getBankDetails() { return bankDetails; }
    public void setBankDetails(UserBankDetails bankDetails) { this.bankDetails = bankDetails; }
    public List<Payslip> getPayslipHistory() { return payslipHistory; }
    public void setPayslipHistory(List<Payslip> payslipHistory) { this.payslipHistory = payslipHistory; }
}