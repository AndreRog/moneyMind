package com.moneymind.classifier.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(String description, String category, BigDecimal amount, LocalDate date) {
}
