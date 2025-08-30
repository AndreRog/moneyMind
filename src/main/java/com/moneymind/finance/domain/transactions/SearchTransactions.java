package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;
import org.jboss.logging.Logger;


public class SearchTransactions {
    private final Logger LOG = Logger.getLogger(SearchTransactions.class);
    private final TransactionRepository transactionRepository;

    public SearchTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public PagedResult<FinancialRecord> execute(String transactionId,
                                                String category,
                                                String dimension,
                                                String bank,
                                                String from,
                                                String to,
                                                int limit,
                                                String cursor,
                                                String sort
    ){
        return this.transactionRepository.search(transactionId, category, dimension, bank,from, to, limit, cursor, sort);
    }
}
