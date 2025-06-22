package com.wipro.payroll.common;

import java.io.Serializable;

public class Department implements Serializable {
    private static final long serialVersionUID = 1L;

    private int departmentId;
    private String departmentName;
    private String description;

    public Department() {}

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
