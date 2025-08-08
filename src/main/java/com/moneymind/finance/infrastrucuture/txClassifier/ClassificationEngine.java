package com.moneymind.finance.adapters.out.txClassifier;

import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.ports.TransactionClassifier;
import com.moneymind.finance.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;

public class ClassificationEngine implements TransactionClassifier {

    private final Logger LOG = Logger.getLogger(ClassificationEngine.class);

    private TransactionClassifier transactionClassifier;
    private TransactionRepository transactionRepository;

    public ClassificationEngine(final ClassificationEngine transactionClassifier,
                                final TransactionRepository transactionRepository) {
        this.transactionClassifier = transactionClassifier;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<FinancialRecord> classify() {

        return List.of();
    }


    @Override
    public FinancialRecord classify(FinancialRecord financialRecord) {
        return null;
    }

    @Override
    public List<FinancialRecord> classify(List<FinancialRecord> financialRecords) {


        return null;
    }

}
