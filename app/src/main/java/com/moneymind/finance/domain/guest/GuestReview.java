package com.moneymind.finance.domain.guest;

import java.util.List;

/**
 * Result of a guest import: the bank detected from the uploaded file plus one
 * {@link MonthlyReview} per month present in the file. Nothing here is persisted.
 */
public record GuestReview(String detectedBank, List<MonthlyReview> months) {}