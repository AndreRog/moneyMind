package com.app.ports;

import com.app.domain.SearchResponse;
import com.app.domain.core.FinancialRecord;

import java.util.List;

public interface TransactionRepository {

    FinancialRecord insertTransaction(final FinancialRecord financialRecord);

    void insertTransactions(final List<FinancialRecord> financialRecords);

    SearchResponse<FinancialRecord> search(String id, String category, String bank, String from,
                                           String to, int limit, String cursor);
}
