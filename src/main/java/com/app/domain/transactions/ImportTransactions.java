package com.app.domain.transactions;

import com.app.domain.core.FinancialRecord;
import com.app.parsers.TransactionsParserFactory;
import com.app.ports.TransactionRepository;
import com.app.ports.TransactionsParser;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImportTransactions {
    private final Logger LOG = Logger.getLogger(ImportTransactions.class);
    private final TransactionsParserFactory transactionsParserFactory;
    private final TransactionRepository transactionRepository;

    public ImportTransactions(final TransactionsParserFactory transactionsParserFactory,
                              final TransactionRepository transactionRepository
    ) {
        this.transactionsParserFactory = transactionsParserFactory;
        this.transactionRepository = transactionRepository;
    }


    public void execute(final String type, final InputStream file) throws IOException {

        final TransactionsParser parser = transactionsParserFactory.getParser(type);

        final List<FinancialRecord> financialRecords = parser.parse(file);

        transactionRepository.insertTransactions(financialRecords);
    }
}
