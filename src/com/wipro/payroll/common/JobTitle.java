package com.wipro.payroll.common;

import java.io.Serializable;

public class JobTitle implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int deptId;
    private String title;
    private String level;
    private String description;

    public JobTitle() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getDeptId() { return deptId; }
    public void setDeptId(int department_id) { this.deptId = department_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
