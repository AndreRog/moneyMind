package com.moneymind.classifier.domain;

import java.math.BigDecimal;

public record PartialTransaction(String description, BigDecimal amount) {
}