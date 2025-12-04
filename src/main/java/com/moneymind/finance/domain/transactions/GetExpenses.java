package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class GetExpenses {
    private final Logger LOG = Logger.getLogger(GetExpenses .class);
    private final TransactionRepository transactionRepository;

    public GetExpenses (final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public PagedResult<FinancialRecord> execute(TransactionSearchQuery query){

        final TransactionSearchQuery expensesQuery = TransactionSearchQuery.builder()
                .from(query.from())
                .to(query.to())
                .limit(query.limit())
                .cursor(query.cursor())
                .excludeCategories(List.of("TRANSFER BETWEEN ACCOUNTS"))
                .build();
        return this.transactionRepository.search(expensesQuery);
    }
}
