package com.moneymind.finance.infrastrucuture.txClassifier;

import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.Classifier;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.ports.TransactionClassifier;
import com.moneymind.finance.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;

public class ClassificationEngine implements Classifier {

    private final Logger LOG = Logger.getLogger(ClassificationEngine.class);

    private TransactionClassifier transactionClassifier;
    private TransactionRepository transactionRepository;

    public ClassificationEngine(final TransactionClassifier transactionClassifier,
                                final TransactionRepository transactionRepository) {
        this.transactionClassifier = transactionClassifier;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Category classify(FinancialRecord financialRecord) {
        return null;
    }

    @Override
    public List<FinancialRecord> classify(List<FinancialRecord> financialRecords) {


        return null;
    }

}
