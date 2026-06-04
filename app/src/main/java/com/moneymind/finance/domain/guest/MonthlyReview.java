package com.moneymind.finance.domain.guest;

import java.math.BigDecimal;
import java.util.List;

/**
 * The instant monthly review for a single period: income/expense/savings totals
 * (Category Type drives the math, never the amount sign) plus the expense Category
 * breakdown. Excluded transactions are absent from every total; they appear only in
 * {@code excludedCount}. {@code countedTransactions} is the remainder that fed the totals.
 */
public record MonthlyReview(
        String period,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal savings,
        List<CategorySlice> categories,
        int countedTransactions,
        int excludedCount
) {}
