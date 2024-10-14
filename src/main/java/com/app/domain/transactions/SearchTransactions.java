package com.app.domain.transactions;

import com.app.domain.SearchResponse;
import com.app.domain.core.FinancialRecord;
import com.app.ports.TransactionRepository;
import org.jboss.logging.Logger;


public class SearchTransactions {
    private final Logger LOG = Logger.getLogger(SearchTransactions.class);
    private final TransactionRepository transactionRepository;

    public SearchTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public SearchResponse<FinancialRecord> execute(String transactionId,
                                                   String category,
                                                   String bank,
                                                   String from,
                                                   String to,
                                                   int limit,
                                                   String cursor
    ){
        return this.transactionRepository.search(transactionId, category,bank,from,to, limit, cursor);
    }
}
