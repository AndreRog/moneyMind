package com.moneymind.finance.domain.transactions;

import com.moneymind.classifier.ports.TrainingDataService;
import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.infrastrucuture.ports.TransactionClassifier;
import com.moneymind.finance.infrastrucuture.web.http.dto.ClassificationRequest;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClassifyTransactions {

    private final Logger LOG = Logger.getLogger(ClassifyTransactions.class);

    private final TransactionClassifier transactionClassifier;

    private final TransactionRepository transactionRepository;

    // Use a cached thread pool or fixed thread pool for async task
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final TrainingDataService trainingDataService;

    public ClassifyTransactions(TransactionClassifier transactionClassifier, TransactionRepository transactionRepository,
                                final TrainingDataService trainingDataService) {
        this.transactionClassifier = transactionClassifier;
        this.transactionRepository = transactionRepository;
        this.trainingDataService = trainingDataService;
    }

    public PagedResult<ClassifiedFinancialRecord> execute(ClassificationRequest classificationRequest) {
        try{
            PagedResult<FinancialRecord> search = this.transactionRepository.fetchNoCategoriesTransaction(100, classificationRequest.cursor());
            List<ClassifiedFinancialRecord> classify = transactionClassifier.classify(search.list());
            return  new PagedResult<>(classify, search.limit(), search.cursor());
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
