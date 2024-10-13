package com.app.ports;

import com.app.domain.core.FinancialRecord;

import java.util.List;

public interface TransactionRepository {

    FinancialRecord insertTransaction(final FinancialRecord financialRecord);

    void insertTransactions(final List<FinancialRecord> financialRecords);

    List<FinancialRecord> list();
}
