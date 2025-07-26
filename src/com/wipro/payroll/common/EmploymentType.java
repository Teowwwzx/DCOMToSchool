package com.wipro.payroll.common;

import java.io.Serializable;

public class EmploymentType implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String positionName;
    private String description;

    public EmploymentType() {}

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getPositionName() { return positionName; }

    public void setPositionName(String positionName) { this.positionName = positionName; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
}
