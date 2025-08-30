package com.moneymind.finance.domain.core;

import com.moneymind.classifier.WekaRandomForestClassifier;

import java.math.BigDecimal;

public record ClassifiedFinancialRecord(String category, BigDecimal classificationConfidence, FinancialRecord record){
}
