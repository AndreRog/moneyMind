package com.moneymind.finance.di;

import com.moneymind.classfication.adapters.out.postgres.TransactionsStore;
import com.moneymind.classfication.application.RandomForestClassifier;
import com.moneymind.classfication.ports.TrainingDataService;
import com.moneymind.finance.adapters.out.postgres.TransactionStore;
import com.moneymind.finance.domain.banks.ListBanks;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.domain.transactions.ImportTransactions;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import com.moneymind.finance.parsers.TransactionsParserFactory;
import com.moneymind.finance.ports.TransactionClassifier;
import com.moneymind.finance.ports.TransactionRepository;
import com.moneymind.finance.ports.TransactionsParser;
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
    ClassifyTransactions classifyTransactions(final TransactionClassifier transactionClassifier,
                                              final TransactionRepository transactionRepository ) {
        return new ClassifyTransactions(transactionClassifier, transactionRepository);
    }

    @ApplicationScoped
    @Produces
    TrainingDataService producesTxStore(final DSLContext dataSource) {
        return new TransactionsStore(dataSource);
    }

    @ApplicationScoped
    @Produces
    TransactionClassifier produceRandomForestClassifier(final TrainingDataService trainingDataService) throws Exception {
        return new RandomForestClassifier(trainingDataService);
    }

}
