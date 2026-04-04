package com.moneymind.finance.domain.core;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class FinancialRecord {
    private String id;
    private final String bankName;
    private final OffsetDateTime date;
    private final String description;
    private final BigDecimal amount;
    private final BigDecimal finalBalance;
    private String category; // TODO: convert to category

    public FinancialRecord(String id, String bankName, OffsetDateTime date, String description, BigDecimal amount, BigDecimal finalBalance, String category) {
        this.id = id;
        this.bankName = bankName;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.finalBalance = finalBalance;
        this.category = category;
    }

    public FinancialRecord(String bankName, OffsetDateTime date, String description, BigDecimal amount, BigDecimal finalBalance) {
        this.bankName = bankName;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.finalBalance = finalBalance;
        this.category = String.valueOf(Category.UNCATEGORIZED);  // default
    }

    public FinancialRecord(String category, BigDecimal amount) {
        this.bankName = null;
        this.date = null;
        this.description = null;
        this.amount = amount;
        this.finalBalance = null;
        this.category = category;
    }

    public FinancialRecord(String period, String category, BigDecimal amount, OffsetDateTime maxDate) {
        this.id = null;
        this.bankName = null;
        this.date = maxDate;
        this.description = period;
        this.amount = amount;
        this.finalBalance = null;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getBankName() {
        return bankName;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getFinalBalance() {
        return finalBalance;
    }

    public void assignCategory(String category) {
        this.category = category;
    }


}
