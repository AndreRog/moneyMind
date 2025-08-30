package com.moneymind.classifier.infrastructure.postgres;

import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.TrainingDataService;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public class TransactionRepository implements TrainingDataService {

    private DSLContext dataSource;

    public TransactionRepository(final DSLContext dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> getTrainingSet() {
        Result<Record> fetch = dataSource.select().from("public.bank_transaction as bt").where("bt.category is not null").fetch();

        List<Transaction> list = fetch.stream().map(record ->
                new Transaction(
                        record.get("description", String.class),
                        record.get("category", String.class),
                        record.get("value", BigDecimal.class)
                )).toList();
        return list;
    }
}
