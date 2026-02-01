package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Bonus implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String employeeName;
    private LocalDate payPeriodStartDate;
    private String name;
    private PayItemType type;
    private BigDecimal amount;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getPayPeriodStartDate() {
        return payPeriodStartDate;
    }

    public void setPayPeriodStartDate(LocalDate payPeriodStartDate) {
        this.payPeriodStartDate = payPeriodStartDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}