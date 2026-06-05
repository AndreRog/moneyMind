package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.core.SearchResult;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;


public class SearchTransactions {

    private final TransactionRepository transactionRepository;

    public SearchTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public SearchResult execute(TransactionSearchQuery query) {
        boolean isAggregating = (query.aggregateByPeriod() != null && !query.aggregateByPeriod().isEmpty())
                || (query.aggregateByColumn() != null && !query.aggregateByColumn().isEmpty());

        if (isAggregating) {
            return new SearchResult.Aggregated(transactionRepository.searchAggregated(query));
        }
        return new SearchResult.Records(transactionRepository.search(query));
    }
}
