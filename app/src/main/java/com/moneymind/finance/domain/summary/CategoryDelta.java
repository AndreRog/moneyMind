package com.moneymind.finance.domain.summary;

import java.math.BigDecimal;

public record CategoryDelta(
        String categoryName,
        BigDecimal baseTotal,
        BigDecimal compareTotal,
        BigDecimal delta
) {}
