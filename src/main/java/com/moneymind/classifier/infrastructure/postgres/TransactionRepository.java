package com.moneymind.classifier.infrastructure.postgres;

import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.TrainingDataService;
import org.jooq.DSLContext;

import java.util.List;

public class TransactionRepository implements TrainingDataService {

    private DSLContext dataSource;

    public TransactionRepository(final DSLContext dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> getTrainingSet() {
        return List.of();
    }
}
