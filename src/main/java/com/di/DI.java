package com.di;

import com.adapters.out.postgres.TransactionStore;
import com.app.domain.banks.ListBanks;
import com.app.domain.transactions.ImportTransactions;
import com.app.domain.transactions.ListTransactions;
import com.app.parsers.TransactionsParserFactory;
import com.app.ports.TransactionRepository;
import com.app.ports.TransactionsParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.jooq.DSLContext;

public class DI {

    @Inject
    DSLContext dataSource;


    @ApplicationScoped
    @Produces
    TransactionRepository transactionRepository(final DSLContext dataSource){
        return new TransactionStore(dataSource);
    }



    @ApplicationScoped
    @Produces
    TransactionsParserFactory transactionsParserFactory(@Any Instance<TransactionsParser> availableParsers) {
        return new TransactionsParserFactory(availableParsers);
    }

    @ApplicationScoped
    @Produces
    ImportTransactions importTransactions(final TransactionsParserFactory transactionsParserFactory,
                                          final TransactionRepository transactionStore) {
        return new ImportTransactions(transactionsParserFactory, transactionStore);
    }

    @ApplicationScoped
    @Produces
    ListTransactions listBankTransactions(final TransactionRepository transactionRepository) {
        return new ListTransactions(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    ListBanks listBanks(final TransactionsParserFactory transactionsParserFactory) {
        return new ListBanks(transactionsParserFactory);
    }

}
