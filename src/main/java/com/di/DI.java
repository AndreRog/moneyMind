package com.di;

import com.adapters.out.postgres.TransactionStore;
import com.adapters.out.txClassifier.ClassificationEngine;
import com.app.domain.banks.ListBanks;
import com.app.domain.transactions.ClassifyTransactions;
import com.app.domain.transactions.ImportTransactions;
import com.app.domain.transactions.SearchTransactions;
import com.app.domain.transactions.UpdateTransactions;
import com.app.parsers.TransactionsParserFactory;
import com.app.ports.TransactionClassifier;
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
    SearchTransactions listBankTransactions(final TransactionRepository transactionRepository) {
        return new SearchTransactions(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    UpdateTransactions updateTransactions(final TransactionRepository transactionRepository) {
        return new UpdateTransactions(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    ListBanks listBanks(final TransactionsParserFactory transactionsParserFactory) {
        return new ListBanks(transactionsParserFactory);
    }

    @ApplicationScoped
    @Produces
    TransactionClassifier transactionClassifier( ) {
        return new ClassificationEngine();
    }

    @ApplicationScoped
    @Produces
    ClassifyTransactions classifyTransactions( final TransactionClassifier transactionClassifier,
                                               final TransactionRepository transactionRepository ) {
        return new ClassifyTransactions(transactionClassifier, transactionRepository);
    }

}
