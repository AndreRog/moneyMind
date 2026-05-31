package com.moneymind.finance.domain.core;

import java.math.BigDecimal;

public record AggregatedResult(String period, String groupKey, BigDecimal total) {}
