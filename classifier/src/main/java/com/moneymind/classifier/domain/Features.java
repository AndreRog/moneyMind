package com.moneymind.classifier.domain;

import java.util.Set;

/**
 * The GDPR-defensible feature representation of a Transaction (per ADR-0001):
 * tokenized description (fed to TF-IDF), a bucketed amount range, and a
 * day-of-month period. It carries no exact amount and no exact date.
 */
public record Features(Set<String> tokens, String amountBucket, String period) {
}
