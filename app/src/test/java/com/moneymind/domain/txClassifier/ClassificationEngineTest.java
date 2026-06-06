package com.moneymind.domain.txClassifier;

import com.moneymind.FinancialRecordTestFactory;
import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.infrastructure.txClassifier.ClassificationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationEngineTest {

    private static final String PREDICTED = "FOOD";
    private static final double CONFIDENCE = 0.95;

    private Classifier stubClassifier;
    private ClassificationEngine classificationEngine;

    @BeforeEach
    void setUp() throws Exception {
        stubClassifier = new Classifier() {
            @Override
            public ClassificationResult classify(Transaction transaction) {
                return new ClassificationResult(PREDICTED, CONFIDENCE, true);
            }

            @Override
            public List<ClassificationResult> classify(List<Transaction> transactions) throws Exception {
                return transactions.stream()
                        .map(t -> new ClassificationResult(PREDICTED, CONFIDENCE, true))
                        .toList();
            }
        };
        classificationEngine = new ClassificationEngine(stubClassifier);
    }

    @Test
    void emptyInputYieldsEmptyList() throws Exception {
        List<ClassifiedFinancialRecord> result = classificationEngine.classify(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void nullInputYieldsEmptyList() throws Exception {
        List<ClassifiedFinancialRecord> result = classificationEngine.classify(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void uncategorisedRecordsAreClassified() throws Exception {
        FinancialRecord uncategorised = FinancialRecordTestFactory.createUncategorisedRecord();

        List<ClassifiedFinancialRecord> result = classificationEngine.classify(List.of(uncategorised));

        assertEquals(1, result.size());
        ClassifiedFinancialRecord classified = result.get(0);
        assertEquals(PREDICTED, classified.category());
        assertEquals(new BigDecimal(CONFIDENCE), classified.classificationConfidence());
        assertEquals(uncategorised, classified.record());
    }

    @Test
    void alreadyCategorisedRecordsAreFilteredOut() throws Exception {
        FinancialRecord categorised = FinancialRecordTestFactory.createMockFinancialRecord();
        // createMockFinancialRecord sets category to "RANDOM CAT" — already categorised

        List<ClassifiedFinancialRecord> result = classificationEngine.classify(List.of(categorised));

        assertTrue(result.isEmpty());
    }

    @Test
    void mixedListOnlyClassifiesUncategorised() throws Exception {
        FinancialRecord categorised = FinancialRecordTestFactory.createMockFinancialRecord();
        FinancialRecord uncategorised = FinancialRecordTestFactory.createUncategorisedRecord();

        List<ClassifiedFinancialRecord> result = classificationEngine.classify(List.of(categorised, uncategorised));

        assertEquals(1, result.size());
        assertEquals(uncategorised, result.get(0).record());
    }
}
