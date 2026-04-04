package com.moneymind.finance.infrastructure.txClassifier;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ClassificationEngine implements TransactionClassifier {

    private final Logger LOG = Logger.getLogger(ClassificationEngine.class);

    private final Classifier classifier;
    private final TransactionRepository transactionRepository;

    public ClassificationEngine(final Classifier classifier,
                                final TransactionRepository transactionRepository) {
        this.classifier = classifier;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<FinancialRecord> classify() {
        TransactionSearchQuery query = TransactionSearchQuery.builder()
                .limit(80000)
                .build();

        PagedResult<FinancialRecord> search = transactionRepository.search(query);
        if (search.list() == null || search.list().isEmpty()) {
            return List.of();
        }

        try {
            List<FinancialRecord> list = search.list()
                    .stream()
                    .filter(financialRecord -> financialRecord.getCategory() != null || financialRecord.getCategory().isBlank())
                    .toList();

            for (FinancialRecord record : list) {
                classifier.classify(mapToTransaction(record));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return List.of();
    }

    @Override
    public FinancialRecord classify(FinancialRecord financialRecord) {
        return null;
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
        return new Transaction(financialRecord.getDescription(), financialRecord.getCategory(), financialRecord.getAmount());
    }
}
