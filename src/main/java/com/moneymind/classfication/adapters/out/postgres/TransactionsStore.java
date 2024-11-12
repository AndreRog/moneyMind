package com.moneymind.classfication.adapters.out.postgres;

import com.moneymind.classfication.application.PartialTransaction;
import com.moneymind.classfication.ports.TrainingDataService;
import com.moneymind.finance.adapters.out.postgres.TransactionStore;
import org.jooq.DSLContext;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TransactionsStore implements TrainingDataService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionStore.class);
    private static final String PLSQL_DUPLICATED_RECORD_ERROR_CODE = "23505";

    final DSLContext dataSource;

    public TransactionsStore(final DSLContext dataSource) {
        this.dataSource = dataSource;
    }

    PartialTransaction toPartialTransaction(BankTransactionRecord bankTransactionRecord) {
        return new PartialTransaction(
                bankTransactionRecord.getDescription(),
                bankTransactionRecord.getBalance(),
                bankTransactionRecord.getCategory()
        );
    }

    @Override
    public List<PartialTransaction> getTrainingSet() {
        final List<BankTransactionRecord> records = this.dataSource
                .select(DSL.asterisk())
                .from(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.CATEGORY.isNotNull())
                .or(BankTransaction.BANK_TRANSACTION.CATEGORY.ne(""))
                .fetchInto(BankTransactionRecord.class);

        return records.stream().map(this::toPartialTransaction).toList();
    }
}
