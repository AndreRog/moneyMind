package com.moneymind.classifier.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests (no mocks) for {@link FeatureExtractor}, covering the boundary behaviour the
 * classifier depends on: amount-bucket edges, day-of-month period edges, and masked/empty
 * descriptions (ADR-0001).
 */
class FeatureExtractorTest {

    private final FeatureExtractor extractor = new FeatureExtractor();

    @ParameterizedTest
    @CsvSource({
            // value, expected bucket — edges land in the UPPER half-open bucket
            "0,        0-10",
            "9.99,     0-10",
            "10,       10-50",
            "49.99,    10-50",
            "50,       50-100",
            "99.99,    50-100",
            "100,      100-500",
            "499.99,   100-500",
            "500,      500-1000",
            "999.99,   500-1000",
            "1000,     1000-5000",
            "4999.99,  1000-5000",
            "5000,     5000+",
            "12345.67, 5000+",
    })
    void amountBucket_mapsEdgesToCorrectBucket(String amount, String expected) {
        assertEquals(expected, extractor.amountBucket(new BigDecimal(amount)));
    }

    @Test
    void amountBucket_usesMagnitudeForNegativeAmounts() {
        assertEquals("500-1000", extractor.amountBucket(new BigDecimal("-500")));
        assertEquals("0-10", extractor.amountBucket(new BigDecimal("-9.99")));
    }

    @Test
    void amountBucket_nullIsUnknown() {
        assertEquals(FeatureExtractor.UNKNOWN, extractor.amountBucket(null));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  early",
            "10, early",  // early -> mid boundary
            "11, mid",
            "20, mid",    // mid -> late boundary
            "21, late",
            "28, late",
    })
    void period_mapsDayOfMonthEdgesToCorrectPeriod(int day, String expected) {
        assertEquals(expected, extractor.period(LocalDate.of(2026, 2, day)));
    }

    @Test
    void period_nullDateIsUnknown() {
        assertEquals(FeatureExtractor.UNKNOWN, extractor.period(null));
    }

    @Test
    void tokens_lowercasesStripsAndDropsShortTokens() {
        // "PT" (len 2) is dropped; "com" (len 3) is kept
        assertEquals(java.util.Set.of("netflix", "com", "lisboa"), extractor.tokens("NETFLIX.COM, Lisboa PT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*******", "12 9*", "  "})
    void tokens_maskedOrShortDescriptionsYieldEmptySet(String description) {
        assertTrue(extractor.tokens(description).isEmpty(),
                () -> "expected no usable tokens for: " + description);
    }

    @Test
    void tokens_nullAndEmptyDescriptionsAreEmpty() {
        assertTrue(extractor.tokens(null).isEmpty());
        assertTrue(extractor.tokens("").isEmpty());
    }

    @Test
    void extract_maskedDescriptionStillYieldsUsableAmountAndPeriodFeatures() {
        Transaction masked = new Transaction(
                "351 9******* transfer", "INCOME", new BigDecimal("1200.00"), LocalDate.of(2026, 1, 2));

        Features features = extractor.extract(masked);

        // Amount + period signal survives even though the description is largely masked
        assertEquals("1000-5000", features.amountBucket());
        assertEquals(FeatureExtractor.EARLY, features.period());
        assertTrue(features.tokens().contains("transfer"));
    }

    @Test
    void extract_fullyMaskedDescriptionDropsToAmountAndPeriodOnly() {
        Transaction masked = new Transaction(
                "*******", "EXPENSE", new BigDecimal("-9.99"), LocalDate.of(2026, 1, 25));

        Features features = extractor.extract(masked);

        assertTrue(features.tokens().isEmpty());
        assertEquals("0-10", features.amountBucket());
        assertEquals(FeatureExtractor.LATE, features.period());
    }
}
