package com.moneymind.finance.adapters.out.txClassifier;

import com.moneymind.classfication.application.PartialTransaction;
import com.moneymind.classfication.application.RandomForestClassifier;
import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.ports.TransactionClassifier;
import com.moneymind.finance.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;

public class ClassificationEngine implements TransactionClassifier {

    private final Logger LOG = Logger.getLogger(ClassificationEngine.class);

    private TransactionClassifier randomForestClassifier;
    private TransactionRepository transactionRepository;

    public ClassificationEngine(final RandomForestClassifier randomForestClassifier,
                                final TransactionRepository transactionRepository) {
        this.randomForestClassifier = randomForestClassifier;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<FinancialRecord> classify() {
        SearchResponse<FinancialRecord> search = this.transactionRepository.search(null, null, null, null, null, null,
                80000, null, null);
        for(FinancialRecord record: search.list()) {
            try {
                randomForestClassifier.classifyTransaction(new PartialTransaction( record.getDescription(), record.getAmount(), record.getCategory()));
            } catch (Exception e) {
                LOG.error("Something went wrong classifying shit");
            }
        }
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

    @Override
    public String classifyTransaction(PartialTransaction partialTransaction) throws Exception {
        return "";
    }
}
