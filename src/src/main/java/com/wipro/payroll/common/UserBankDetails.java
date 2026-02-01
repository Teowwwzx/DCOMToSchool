package com.wipro.payroll.common;

import java.io.Serializable;

public class UserBankDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;

    public UserBankDetails() {}

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getBankName() {
        return bankName;
    }
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    public String getAccountHolderName() {
        return accountHolderName;
    }
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
}
