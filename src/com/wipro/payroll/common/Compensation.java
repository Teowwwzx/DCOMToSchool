package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Compensation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private PayType payType;
    private BigDecimal amount;
    private LocalDate effectiveDate;

    public Compensation() {}

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

    public PayType getPayType() {
        return payType;
    }

    public void setPayType(PayType payType) {
        this.payType = payType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
