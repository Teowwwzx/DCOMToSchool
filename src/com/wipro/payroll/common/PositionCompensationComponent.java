package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;

public class PositionCompensationComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int employmentPositionId;
    private String description;
    private PayItemType type;
    private BigDecimal amount;

    public PositionCompensationComponent() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmploymentPositionId() { return employmentPositionId; }
    public void setEmploymentPositionId(int employmentPositionId) { this.employmentPositionId = employmentPositionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PayItemType getType() { return type; }
    public void setType(PayItemType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
