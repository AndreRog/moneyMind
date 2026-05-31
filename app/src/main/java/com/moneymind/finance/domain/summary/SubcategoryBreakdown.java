package com.moneymind.finance.domain.summary;

import com.moneymind.finance.domain.core.CategoryType;

import java.math.BigDecimal;

public record SubcategoryBreakdown(
        String subcategoryName,
        String categoryName,
        CategoryType type,
        BigDecimal total
) {}
