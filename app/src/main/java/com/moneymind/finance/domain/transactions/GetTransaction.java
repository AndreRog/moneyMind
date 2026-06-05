package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;

import java.util.UUID;

public class GetTransaction {

    private final TransactionRepository transactionRepository;

    public GetTransaction(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public FinancialRecord execute(String id) {
        try {
            return transactionRepository.getById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
