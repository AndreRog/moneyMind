package com.moneymind.classfication.application;

import java.math.BigDecimal;

public record PartialTransaction(String description, BigDecimal amount, String category) {
}
