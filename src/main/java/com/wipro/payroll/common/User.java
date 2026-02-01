package com.wipro.payroll.common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int deptId;
    private String departmentName;
    private int jobTitleId;
    private String jobTitle;
    private int empTypeId;
    private String username;
    private String fName;
    private String lName;
    private String email;
    private String phone;
    private String ic;
    private UserStatus status;
    private Role role;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getDeptId() { return deptId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public void setDeptId(int deptId) { this.deptId = deptId; }
    public int getJobTitleId() { return jobTitleId; }
    public void setJobTitleId(int jobTitleId) { this.jobTitleId = jobTitleId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public int getEmpTypeId() { return empTypeId; }
    public void setEmpTypeId(int empTypeId) { this.empTypeId = empTypeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFName() { return fName; }
    public void setFName(String fName) { this.fName = fName; }
    public String getLName() { return lName; }
    public void setLName(String lName) { this.lName = lName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIc() { return ic; }
    public void setIc(String ic) { this.ic = ic; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}