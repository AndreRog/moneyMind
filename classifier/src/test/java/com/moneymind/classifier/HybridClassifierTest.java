package com.moneymind.classifier;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.ClassificationRules;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests (no mocks, no DB) for {@link HybridClassifier}, covering:
 * - rule match wins regardless of ML result
 * - high-confidence ML result is forwarded
 * - low-confidence ML falls back based on amount sign (credit→INCOME, debit→MISCELLANEOUS)
 * - ML exception falls back based on amount sign
 * - low-confidence ML never returns an EXCLUDED-type category (TRANSFERS)
 */
class HybridClassifierTest {

    private Classifier stubMl(ClassificationResult result) {
        return new Classifier() {
            @Override
            public ClassificationResult classify(Transaction t) {
                return result;
            }

            @Override
            public List<ClassificationResult> classify(List<Transaction> ts) {
                return ts.stream().map(t -> result).toList();
            }
        };
    }

    private Classifier failingMl() {
        return new Classifier() {
            @Override
            public ClassificationResult classify(Transaction t) throws Exception {
                throw new IllegalStateException("Insufficient training data");
            }

            @Override
            public List<ClassificationResult> classify(List<Transaction> ts) throws Exception {
                throw new IllegalStateException("Insufficient training data");
            }
        };
    }

    private Transaction debit(String description, String amount) {
        return new Transaction(description, null, new BigDecimal("-" + amount), LocalDate.now());
    }

    private Transaction credit(String description, String amount) {
        return new Transaction(description, null, new BigDecimal(amount), LocalDate.now());
    }

    private Transaction noAmount(String description) {
        return new Transaction(description, null, null, null);
    }

    // --- rule wins ---

    @Test
    void ruleMatch_returnsRuleCategory_ignoringML() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of(new ClassificationRules.Rule("CONTINENTE", "FOOD & DINING"))),
                stubMl(new ClassificationResult("TRANSFERS", 0.99, true))
        );

        ClassificationResult result = hybrid.classify(credit("CONTINENTE BELEM LISBOA", "63.15"));

        assertEquals("FOOD & DINING", result.predictedCategory());
        assertEquals(1.0, result.confidence());
        assertTrue(result.highConfidence());
    }

    // --- high-confidence ML ---

    @Test
    void highConfidenceML_isForwarded_whenNoRuleMatches() throws Exception {
        ClassificationResult mlResult = new ClassificationResult("HOUSING", 0.85, true);
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(mlResult)
        );

        ClassificationResult result = hybrid.classify(debit("SOME LANDLORD PAYMENT", "900.00"));

        assertEquals("HOUSING", result.predictedCategory());
        assertEquals(0.85, result.confidence(), 1e-9);
        assertTrue(result.highConfidence());
    }

    @Test
    void confidenceBoundary_exactThreshold_isForwarded() throws Exception {
        ClassificationResult atThreshold = new ClassificationResult("TRANSPORT", HybridClassifier.MIN_CONFIDENCE, true);
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(atThreshold)
        );

        ClassificationResult result = hybrid.classify(debit("SOME TRANSPORT MERCHANT", "50.00"));

        assertEquals("TRANSPORT", result.predictedCategory());
    }

    // --- low-confidence fallback: amount-sign aware ---

    @Test
    void lowConfidenceML_debitFallsBackToMiscellaneous() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(new ClassificationResult("HEALTH", 0.35, false))
        );

        ClassificationResult result = hybrid.classify(debit("UNKNOWN MERCHANT", "60.00"));

        assertEquals(HybridClassifier.MISCELLANEOUS, result.predictedCategory());
        assertFalse(result.highConfidence());
    }

    @Test
    void lowConfidenceML_creditFallsBackToIncome() throws Exception {
        // An unrecognised credit (positive amount, e.g. salary not matched by rules) must NOT
        // end up in MISCELLANEOUS (EXPENSE type) — that would make the expense total negative.
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(new ClassificationResult("MISCELLANEOUS", 0.2, false))
        );

        ClassificationResult result = hybrid.classify(credit("UNKNOWN CREDIT ENTRY", "2600.00"));

        assertEquals(ClassificationRules.CAT_INCOME, result.predictedCategory());
        assertFalse(result.highConfidence());
    }

    @Test
    void lowConfidenceML_nullAmountFallsBackToMiscellaneous() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(new ClassificationResult("HOUSING", 0.1, false))
        );

        ClassificationResult result = hybrid.classify(noAmount("UNKNOWN TRANSACTION"));

        assertEquals(HybridClassifier.MISCELLANEOUS, result.predictedCategory());
    }

    @Test
    void lowConfidenceML_neverReturnsExcludedCategory() throws Exception {
        // ML predicts TRANSFERS (EXCLUDED type) with low confidence — must be overridden
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of()),
                stubMl(new ClassificationResult("TRANSFERS", 0.45, false))
        );

        ClassificationResult result = hybrid.classify(debit("AMBIGUOUS TRANSACTION", "100.00"));

        assertEquals(HybridClassifier.MISCELLANEOUS, result.predictedCategory());
        assertFalse(result.highConfidence());
    }

    // --- ML exception fallback ---

    @Test
    void mlException_creditFallsBackToIncome() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(new ClassificationRules(List.of()), failingMl());

        ClassificationResult result = hybrid.classify(credit("SALARY DEPOSIT", "3000.00"));

        assertEquals(ClassificationRules.CAT_INCOME, result.predictedCategory());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void mlException_debitFallsBackToMiscellaneous() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(new ClassificationRules(List.of()), failingMl());

        ClassificationResult result = hybrid.classify(debit("UNKNOWN EXPENSE", "150.00"));

        assertEquals(HybridClassifier.MISCELLANEOUS, result.predictedCategory());
    }

    // --- batch classify ---

    @Test
    void batchClassify_appliesHybridLogicToAll() throws Exception {
        HybridClassifier hybrid = new HybridClassifier(
                new ClassificationRules(List.of(new ClassificationRules.Rule("CONTINENTE", "FOOD & DINING"))),
                stubMl(new ClassificationResult("TRANSFERS", 0.2, false))
        );

        List<ClassificationResult> results = hybrid.classify(List.of(
                credit("CONTINENTE BELEM", "63.15"),  // rule match
                debit("RANDOM MERCHANT", "50.00"),    // low confidence debit → MISCELLANEOUS
                credit("UNKNOWN CREDIT", "500.00")    // low confidence credit → INCOME
        ));

        assertEquals(3, results.size());
        assertEquals("FOOD & DINING", results.get(0).predictedCategory());
        assertEquals(HybridClassifier.MISCELLANEOUS, results.get(1).predictedCategory());
        assertEquals(ClassificationRules.CAT_INCOME, results.get(2).predictedCategory());
    }
}
