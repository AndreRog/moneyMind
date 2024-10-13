package com.app.domain.transactions;

import com.app.domain.core.FinancialRecord;
import com.app.ports.TransactionRepository;
import org.jboss.logging.Logger;

import java.util.List;


public class ListTransactions {
    private final Logger LOG = Logger.getLogger(ListTransactions.class);
    private final TransactionRepository transactionRepository;

    public ListTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<FinancialRecord> execute(){
        return this.transactionRepository.list();
    }
}
