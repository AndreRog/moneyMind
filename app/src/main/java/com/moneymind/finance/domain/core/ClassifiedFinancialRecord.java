package com.moneymind.finance.domain.core;

import java.math.BigDecimal;

public record ClassifiedFinancialRecord(String category, BigDecimal classificationConfidence, FinancialRecord record){
}
