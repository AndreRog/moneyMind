package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.ClassificationQuery;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;

public class ClassifyTransactions {

    private final Logger LOG = Logger.getLogger(ClassifyTransactions.class);

    private final TransactionClassifier transactionClassifier;
    private final TransactionRepository transactionRepository;

    public ClassifyTransactions(TransactionClassifier transactionClassifier, TransactionRepository transactionRepository) {
        this.transactionClassifier = transactionClassifier;
        this.transactionRepository = transactionRepository;
    }

    public PagedResult<ClassifiedFinancialRecord> execute(ClassificationQuery query) {
        try {
            PagedResult<FinancialRecord> search = this.transactionRepository.fetchNoCategoriesTransaction(100, query.cursor());
            List<ClassifiedFinancialRecord> classify = transactionClassifier.classify(search.list());
            return new PagedResult<>(classify, search.limit(), search.cursor());
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
