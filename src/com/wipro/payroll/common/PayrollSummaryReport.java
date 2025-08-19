package com.wipro.payroll.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class PayrollSummaryReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate payPeriod;
    private int numberOfEmployeesPaid;
    private BigDecimal totalGrossEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPay;
    private Map<String, BigDecimal> netPayByDepartment; // For a department-level breakdown
    private BigDecimal estimatedEmployerContributions;
    private BigDecimal totalCompanyPayout;

    public LocalDate getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(LocalDate payPeriod) {
        this.payPeriod = payPeriod;
    }

    public int getNumberOfEmployeesPaid() {
        return numberOfEmployeesPaid;
    }

    public void setNumberOfEmployeesPaid(int numberOfEmployeesPaid) {
        this.numberOfEmployeesPaid = numberOfEmployeesPaid;
    }

    public BigDecimal getTotalGrossEarnings() {
        return totalGrossEarnings;
    }

    public void setTotalGrossEarnings(BigDecimal totalGrossEarnings) {
        this.totalGrossEarnings = totalGrossEarnings;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getTotalNetPay() {
        return totalNetPay;
    }

    public void setTotalNetPay(BigDecimal totalNetPay) {
        this.totalNetPay = totalNetPay;
    }

    public Map<String, BigDecimal> getNetPayByDepartment() {
        return netPayByDepartment;
    }

    public void setNetPayByDepartment(Map<String, BigDecimal> netPayByDepartment) {
        this.netPayByDepartment = netPayByDepartment;
    }

    public BigDecimal getEstimatedEmployerContributions() {
        return estimatedEmployerContributions;
    }

    public void setEstimatedEmployerContributions(BigDecimal estimatedEmployerContributions) {
        this.estimatedEmployerContributions = estimatedEmployerContributions;
    }

    public BigDecimal getTotalCompanyPayout() {
        return totalCompanyPayout;
    }

    public void setTotalCompanyPayout(BigDecimal totalCompanyPayout) {
        this.totalCompanyPayout = totalCompanyPayout;
    }

    // Add all getters and setters
}