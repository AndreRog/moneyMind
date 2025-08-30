package com.moneymind.finance.domain.ports;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.FinancialRecord;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {

    FinancialRecord insertTransaction(final FinancialRecord financialRecord);

    void insertTransactions(final List<FinancialRecord> financialRecords);

    PagedResult<FinancialRecord> search(String id, String category, String dimension, String bank, String from,
                                        String to, int limit, String cursor, String sort);

    PagedResult<FinancialRecord> fetchNoCategoriesTransaction(int limit, String cursor);

    void update(final UUID id, final String category);

    FinancialRecord getById(final UUID id);
}
