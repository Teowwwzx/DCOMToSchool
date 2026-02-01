package com.wipro.payroll.common;

import java.io.Serializable;

public class Department implements Serializable {
    private static final long serialVersionUID = 1L;

    private int deptId;
    private String deptName;
    private String description;

    public Department() {}

    public int getDeptId() {
        return deptId;
    }
    public void setDeptId(int deptId) {
        this.deptId = deptId;
    }
    public String getDeptName() {
        return deptName;
    }
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
