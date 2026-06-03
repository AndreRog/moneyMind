package com.moneymind.classifier.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure, deterministic feature extraction for the auto-classifier (ADR-0001).
 *
 * <p>Turns a {@link Transaction} into a {@link Features} view that the Weka model trains and
 * predicts on: tokenized description (TF-IDF input) + a bucketed amount range + a day-of-month
 * period. It deliberately keeps no exact amount and no exact date so the shared global model
 * never embeds precise financial detail (GDPR-defensible).
 *
 * <p>No DB, no framework, no mutable state — safe to instantiate freely and to unit-test directly.
 */
public final class FeatureExtractor {

    public static final String UNKNOWN = "unknown";

    public static final String EARLY = "early";
    public static final String MID = "mid";
    public static final String LATE = "late";

    /** Day-of-month boundaries: 1..10 early, 11..20 mid, 21..31 late. */
    private static final int EARLY_LAST_DAY = 10;
    private static final int MID_LAST_DAY = 20;

    /** Half-open [lower, upper) buckets keyed off the magnitude (absolute value) of the amount. */
    private static final BigDecimal[] BUCKET_UPPER_BOUNDS = {
            new BigDecimal("10"),
            new BigDecimal("50"),
            new BigDecimal("100"),
            new BigDecimal("500"),
            new BigDecimal("1000"),
            new BigDecimal("5000"),
    };
    private static final String[] BUCKET_LABELS = {
            "0-10", "10-50", "50-100", "100-500", "500-1000", "1000-5000", "5000+",
    };

    /** Nominal values for the amount-bucket Weka attribute (includes {@link #UNKNOWN}). */
    public static final List<String> AMOUNT_BUCKETS = List.of(
            "0-10", "10-50", "50-100", "100-500", "500-1000", "1000-5000", "5000+", UNKNOWN);

    /** Nominal values for the period Weka attribute (includes {@link #UNKNOWN}). */
    public static final List<String> PERIODS = List.of(EARLY, MID, LATE, UNKNOWN);

    private static final int MIN_TOKEN_LENGTH = 3;

    public Features extract(Transaction transaction) {
        return new Features(
                tokens(transaction.description()),
                amountBucket(transaction.amount()),
                period(transaction.date()));
    }

    /**
     * Lowercase, strip non-alphanumerics, keep tokens longer than two characters. Null/empty/fully
     * masked descriptions yield an empty set (the amount + period features still carry signal).
     */
    public Set<String> tokens(String description) {
        Set<String> tokens = new LinkedHashSet<>();
        if (description == null || description.isBlank()) {
            return tokens;
        }
        String[] candidates = description.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");
        for (String candidate : candidates) {
            if (candidate.length() >= MIN_TOKEN_LENGTH) {
                tokens.add(candidate);
            }
        }
        return tokens;
    }

    /**
     * Maps the amount's magnitude to a half-open {@code [lower, upper)} range label; an edge value
     * lands in the upper bucket (e.g. 500.00 → "500-1000"). Null → {@link #UNKNOWN}.
     */
    public String amountBucket(BigDecimal amount) {
        if (amount == null) {
            return UNKNOWN;
        }
        BigDecimal magnitude = amount.abs();
        for (int i = 0; i < BUCKET_UPPER_BOUNDS.length; i++) {
            if (magnitude.compareTo(BUCKET_UPPER_BOUNDS[i]) < 0) {
                return BUCKET_LABELS[i];
            }
        }
        return BUCKET_LABELS[BUCKET_LABELS.length - 1];
    }

    /** Day-of-month → early/mid/late. Null date → {@link #UNKNOWN}. */
    public String period(LocalDate date) {
        if (date == null) {
            return UNKNOWN;
        }
        int day = date.getDayOfMonth();
        if (day <= EARLY_LAST_DAY) {
            return EARLY;
        }
        if (day <= MID_LAST_DAY) {
            return MID;
        }
        return LATE;
    }
}
