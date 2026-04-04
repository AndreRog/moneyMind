package com.moneymind.classifier.domain;

import java.math.BigDecimal;

public record Transaction(String description, String category, BigDecimal amount) {
}
