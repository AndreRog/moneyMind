package com.moneymind.domain.transactions;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.AggregatedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.SearchResult;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

class SearchTransactionsTest {

    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);

    SearchTransactions searchTransactions;

    @BeforeEach
    void beforeEach() {
        this.searchTransactions = new SearchTransactions(transactionRepository);
    }

    @Test
    void plainQueryRoutesToRecords() {
        TransactionSearchQuery query = TransactionSearchQuery.builder().limit(10).build();
        PagedResult<FinancialRecord> pagedResult = new PagedResult<>(List.of(), 10, null);
        Mockito.when(transactionRepository.search(query)).thenReturn(pagedResult);

        SearchResult result = searchTransactions.execute(query);

        Assertions.assertInstanceOf(SearchResult.Records.class, result);
        Assertions.assertSame(pagedResult, ((SearchResult.Records) result).result());
    }

    @Test
    void aggregateByPeriodQueryRoutesToAggregated() {
        TransactionSearchQuery query = TransactionSearchQuery.builder().aggregateByPeriod("MONTH").limit(10).build();
        PagedResult<AggregatedResult> pagedResult = new PagedResult<>(
                List.of(new AggregatedResult("2024-01", null, BigDecimal.TEN)), 10, null);
        Mockito.when(transactionRepository.searchAggregated(query)).thenReturn(pagedResult);

        SearchResult result = searchTransactions.execute(query);

        Assertions.assertInstanceOf(SearchResult.Aggregated.class, result);
        Assertions.assertSame(pagedResult, ((SearchResult.Aggregated) result).result());
    }

    @Test
    void aggregateByColumnQueryRoutesToAggregated() {
        TransactionSearchQuery query = TransactionSearchQuery.builder().aggregateByColumn("category").limit(10).build();
        PagedResult<AggregatedResult> pagedResult = new PagedResult<>(
                List.of(new AggregatedResult(null, "FOOD", BigDecimal.valueOf(50))), 10, null);
        Mockito.when(transactionRepository.searchAggregated(query)).thenReturn(pagedResult);

        SearchResult result = searchTransactions.execute(query);

        Assertions.assertInstanceOf(SearchResult.Aggregated.class, result);
    }

    @Test
    void aggregateByBothPeriodAndColumnRoutesToAggregated() {
        TransactionSearchQuery query = TransactionSearchQuery.builder()
                .aggregateByPeriod("YEAR").aggregateByColumn("category").limit(10).build();
        PagedResult<AggregatedResult> pagedResult = new PagedResult<>(List.of(), 10, null);
        Mockito.when(transactionRepository.searchAggregated(query)).thenReturn(pagedResult);

        SearchResult result = searchTransactions.execute(query);

        Assertions.assertInstanceOf(SearchResult.Aggregated.class, result);
        Mockito.verify(transactionRepository).searchAggregated(query);
        Mockito.verify(transactionRepository, Mockito.never()).search(Mockito.any());
    }
}
