package com.moneymind.finance.domain.summary;

import java.math.BigDecimal;
import java.util.List;

public record Summary(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal savings,
        List<CategoryBreakdown> categories,
        List<SubcategoryBreakdown> subcategories
) {}
