package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.BankRegistry;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.ports.TransactionsParser;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.List;

public class ImportTransactions {
    private final Logger LOG = Logger.getLogger(ImportTransactions.class);
    private final BankRegistry bankRegistry;
    private final TransactionRepository transactionRepository;
    private final TransactionClassifier transactionClassifier;

    public ImportTransactions(final BankRegistry bankRegistry,
                              final TransactionRepository transactionRepository,
                              final TransactionClassifier transactionClassifier) {
        this.bankRegistry = bankRegistry;
        this.transactionRepository = transactionRepository;
        this.transactionClassifier = transactionClassifier;
    }

    public List<ClassifiedFinancialRecord> execute(final String type, final InputStream file) throws Exception {
        final TransactionsParser parser = bankRegistry.getParser(type);
        final List<FinancialRecord> financialRecords = parser.parse(file);
        final List<FinancialRecord> insertedRecords = transactionRepository.insertTransactions(financialRecords);
        return transactionClassifier.classify(insertedRecords);
    }
}
