package com.moneymind.finance.domain.transactions;

import com.moneymind.classifier.ports.TrainingDataService;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.infrastrucuture.ports.TransactionClassifier;
import com.moneymind.finance.infrastrucuture.txClassifier.ClassificationEngine;
import com.moneymind.finance.parsers.TransactionsParserFactory;
import com.moneymind.finance.ports.TransactionsParser;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImportTransactions {
    private final Logger LOG = Logger.getLogger(ImportTransactions.class);
    private final TransactionsParserFactory transactionsParserFactory;
    private final TransactionRepository transactionRepository;
    private final TransactionClassifier transactionClassifier;

    public ImportTransactions(final TransactionsParserFactory transactionsParserFactory,
                              final TransactionRepository transactionRepository,
                              final TransactionClassifier transactionClassifier
    ) {
        this.transactionsParserFactory = transactionsParserFactory;
        this.transactionRepository = transactionRepository;
        this.transactionClassifier = transactionClassifier;
    }


    public  List<ClassifiedFinancialRecord> execute(final String type, final InputStream file) throws Exception {

        final TransactionsParser parser = transactionsParserFactory.getParser(type);

        final List<FinancialRecord> financialRecords = parser.parse(file);

        final List<FinancialRecord> insertedRecords = transactionRepository.insertTransactions(financialRecords);
        return transactionClassifier.classify(insertedRecords);
    }
}
