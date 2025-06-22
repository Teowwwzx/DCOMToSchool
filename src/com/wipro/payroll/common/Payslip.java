package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Payslip implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private LocalDate payPeriodStartDate; // Using LocalDate for dates without time
    private LocalDate payPeriodEndDate;
    private String remark;
    private LocalDateTime createdAt;

    // A payslip object should contain its list of line items.
    // This makes it a complete, self-contained object to send to the client.
    private List<PayItem> payItems = new ArrayList<>();

    // We can also include calculated fields that don't exist in the DB
    // These are calculated by the server before sending to the client.
    private BigDecimal grossEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;


    public Payslip() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDate getPayPeriodStartDate() { return payPeriodStartDate; }
    public void setPayPeriodStartDate(LocalDate payPeriodStartDate) { this.payPeriodStartDate = payPeriodStartDate; }
    public LocalDate getPayPeriodEndDate() { return payPeriodEndDate; }
    public void setPayPeriodEndDate(LocalDate payPeriodEndDate) { this.payPeriodEndDate = payPeriodEndDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<PayItem> getPayItems() { return payItems; }
    public void setPayItems(List<PayItem> payItems) { this.payItems = payItems; }
    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
}
