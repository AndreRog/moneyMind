package com.moneymind.classifier.di;

import com.moneymind.classifier.WekaRandomForestClassifier;
import com.moneymind.classifier.infrastructure.postgres.TransactionRepository;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.classifier.ports.TrainingDataService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.jooq.DSLContext;

public class DI {

    @Inject
    DSLContext dataSource;


    @ApplicationScoped
    @Produces
    TrainingDataService trainingDataService(final DSLContext dataSource) {
        return new TransactionRepository(dataSource);
    }

    @ApplicationScoped
    @Produces
    Classifier produceClassifier(final TrainingDataService trainingDataService) throws Exception {
        return new WekaRandomForestClassifier(trainingDataService);
    }
}
