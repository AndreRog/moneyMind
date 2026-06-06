package com.moneymind.finance.infrastructure.txClassifier;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionClassifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ClassificationEngine implements TransactionClassifier {

    private final Classifier classifier;

    public ClassificationEngine(final Classifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public List<ClassifiedFinancialRecord> classify(List<FinancialRecord> financialRecords) {
        if (financialRecords == null || financialRecords.isEmpty()) {
            return List.of();
        }

        try {
            List<FinancialRecord> list = financialRecords
                    .stream()
                    .filter(financialRecord -> financialRecord.getCategory() == null || financialRecord.getCategory().isBlank() || "UNCATEGORIZED".equals(financialRecord.getCategory()))
                    .toList();
            List<ClassifiedFinancialRecord> classifiedFinancialRecords = new ArrayList<>();
            for (FinancialRecord record : list) {
                ClassificationResult result = classifier.classify(mapToTransaction(record));
                classifiedFinancialRecords.add(new ClassifiedFinancialRecord(result.predictedCategory(), new BigDecimal(result.confidence()), record));
            }
            return classifiedFinancialRecords;
        } catch (Exception e) {
            // TODO: Better exceptions
            throw new RuntimeException(e);
        }
    }

    private Transaction mapToTransaction(final FinancialRecord financialRecord) {
        return new Transaction(
                financialRecord.getDescription(),
                financialRecord.getCategory(),
                financialRecord.getAmount(),
                financialRecord.getDate() == null ? null : financialRecord.getDate().toLocalDate());
    }
}
