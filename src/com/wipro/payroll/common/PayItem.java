package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int payslipId;
    private String name;
    private String description;
    private PayItemType type; // Using the PayItemType enum you already created
    private BigDecimal amount; // Using BigDecimal for money is crucial
    private String remark;
    private LocalDateTime createdAt;

    public PayItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPayslipId() {
        return payslipId;
    }

    public void setPayslipId(int payslipId) {
        this.payslipId = payslipId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PayItemType getType() {
        return type;
    }

    public void setType(PayItemType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
