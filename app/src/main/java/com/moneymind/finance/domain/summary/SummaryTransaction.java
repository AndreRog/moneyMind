package com.moneymind.finance.domain.summary;

import com.moneymind.finance.domain.core.CategoryType;

import java.math.BigDecimal;

public record SummaryTransaction(
        BigDecimal amount,
        String categoryName,
        String subcategoryName,
        CategoryType categoryType
) {}
