package com.moneymind.finance.domain.core;

import com.moneymind.finance.domain.PagedResult;

public sealed interface SearchResult permits SearchResult.Records, SearchResult.Aggregated {

    record Records(PagedResult<FinancialRecord> result) implements SearchResult {}

    record Aggregated(PagedResult<AggregatedResult> result) implements SearchResult {}
}
