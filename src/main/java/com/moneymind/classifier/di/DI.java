package com.moneymind.classifier.di;

import com.moneymind.classifier.infrastructure.postgres.TransactionRepository;
import com.moneymind.classifier.ports.TrainingDataService;
import com.moneymind.finance.infrastrucuture.postgres.TransactionStore;
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
}
