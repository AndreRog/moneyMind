package com.moneymind.classfication;

import com.moneymind.classfication.adapters.out.postgres.TransactionsStore;
import com.moneymind.classfication.application.RandomForestClassifier;
import com.moneymind.classfication.ports.TrainingDataService;
import com.moneymind.finance.ports.TransactionClassifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import org.jooq.DSLContext;

public class DI {


    /**
     * For now the injection of this is on the money mind layer we should put here.
     */
//    @ApplicationScoped
//    @Produces
//    TrainingDataService producesTxStore(final DSLContext dataSource) {
//        return new TransactionsStore(dataSource);
//    }
//
//    @ApplicationScoped
//    @Produces
//    TransactionClassifier produceRandomForestClassifier(final TrainingDataService trainingDataService) throws Exception {
//        return new RandomForestClassifier(trainingDataService);
//    }
//
}
