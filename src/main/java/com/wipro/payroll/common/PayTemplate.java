package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;

public class PayTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int jobTitleId;
    private int empTypeId;
    private String description;
    private PayItemType type;
    private BigDecimal amount;

    public PayTemplate() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getJobTitleId() { return jobTitleId; }
    public void setJobTitleId(int jobTitleId) { this.jobTitleId = jobTitleId; }
    public int getEmpTypeId() { return empTypeId; }
    public void setEmpTypeId(int empTypeId) { this.empTypeId = empTypeId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PayItemType getType() { return type; }
    public void setType(PayItemType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
