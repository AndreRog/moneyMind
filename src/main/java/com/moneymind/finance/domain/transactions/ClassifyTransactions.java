package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.ports.TransactionClassifier;
import com.moneymind.finance.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClassifyTransactions {

    private final Logger LOG = Logger.getLogger(ClassifyTransactions.class);

    private final TransactionClassifier transactionClassifier;

    private final TransactionRepository transactionRepository;

    // Use a cached thread pool or fixed thread pool for async task
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ClassifyTransactions(TransactionClassifier transactionClassifier, TransactionRepository transactionRepository) {
        this.transactionClassifier = transactionClassifier;
        this.transactionRepository = transactionRepository;
    }

    public void execute() {
        try{
            SearchResponse<FinancialRecord> search = this.transactionRepository.search(null, null, null, null, null, null, 80000, null,null);
            List<FinancialRecord> classify = transactionClassifier.classify(search.list());
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
        LOG.info("We are going to classify Stuff");
    }

    // TODO: instead of classifying all do it in batches and should be done async ... this is not a blocking operation
    // We can create a job/task, a first iteration should the ClassificationEngine that is a dumb impl or even a separate
    // module
//    public CompletableFuture<Void> execute() {

//
//        return CompletableFuture.runAsync(() -> {
//            //find all uncategorized transactions
//
//            // TransactionClassifier allows me to use multiple implementations one which can now be an internal DUMB impl
//        }, executor);
//    }
}
