package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;

import java.util.List;
import java.util.UUID;

public class UpdateTransactions {

    private final TransactionRepository transactionRepository;

    public UpdateTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public FinancialRecord execute(final String id, final String category) {
        final UUID uuid = UUID.fromString(id);

        FinancialRecord transaction = transactionRepository.getById(uuid);

        if(transaction == null) {
            return null;
        }

        this.transactionRepository.update(uuid, category);
        transaction.assignCategory(category);

        return transaction;
    }
}
