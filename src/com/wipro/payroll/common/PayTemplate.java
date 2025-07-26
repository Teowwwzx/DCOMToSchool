package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;

public class PayTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int job_title_id;
    private String description;

    public PayTemplate() {}

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getJob_title_id() { return job_title_id; }

    public void setJob_title_id(int job_title_id) { this.job_title_id = job_title_id; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public PayItemType getType() { return type; }

    public void setType(PayItemType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    private PayItemType type; // Using your enum
    private BigDecimal amount;
}
