package com.adapters.out.postgres;

import com.app.domain.core.FinancialRecord;
import com.app.exceptions.ExceptionCode;
import com.app.exceptions.StoreException;
import com.app.ports.TransactionRepository;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionStore implements TransactionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransactionStore.class);
    private static final String PLSQL_DUPLICATED_RECORD_ERROR_CODE = "23505";

    final DSLContext dataSource;

    public TransactionStore(final DSLContext dataSource) {
        this.dataSource = dataSource;
    }

    public BankTransactionRecord toRecord(FinancialRecord record) {
        final BankTransactionRecord bankTransactionRecord = dataSource.newRecord(BankTransaction.BANK_TRANSACTION);
        bankTransactionRecord.setValue(record.getAmount());
        bankTransactionRecord.setDescription(record.getDescription());
        bankTransactionRecord.setDate(record.getDate());
        bankTransactionRecord.setBalance(record.getFinalBalance());
        bankTransactionRecord.setBankName(record.getBankName());

        return bankTransactionRecord;
    }

    public FinancialRecord toModel(BankTransactionRecord record) {
        return new FinancialRecord(
                record.getBankName(),
                record.getDate(),
                record.getDescription(),
                BigDecimal.valueOf(record.getValue().doubleValue()),
                BigDecimal.valueOf(record.getBalance().doubleValue())
        );
    }
    @Override
    public FinancialRecord insertTransaction(FinancialRecord financialRecord) {
        final BankTransactionRecord bankTransactionRecord = toRecord(financialRecord);
        bankTransactionRecord.changed(BankTransaction.BANK_TRANSACTION.ID, false);
        bankTransactionRecord.store();

        return toModel(bankTransactionRecord);
    }

    @Override
    public void insertTransactions(List<FinancialRecord> financialRecords) {
        try {
            final List<BankTransactionRecord> transactionRecords = financialRecords.stream().map(this::toRecord).toList();

            final int[] inserted = dataSource.batchInsert(transactionRecords).execute();

            if (inserted.length != financialRecords.size()) {
                logger.error("Error occurred on batch financialRecords insert");
            }
        } catch (IntegrityConstraintViolationException ex) {
            if (ex.getCause() instanceof BatchUpdateException &&
                    ((PSQLException) ex.getCause().getCause()).getSQLState().equalsIgnoreCase(PLSQL_DUPLICATED_RECORD_ERROR_CODE)) {
                logger.error(
                        ExceptionCode.DUPLICATED_RECORD.toString(),
                        ex
                );

                throw new StoreException(
                        ExceptionCode.DUPLICATED_RECORD,
                        ex
                );
            }
        }
    }

    @Override
    public List<FinancialRecord> list() {
        final Result<BankTransactionRecord> fetch = dataSource.selectFrom(BankTransaction.BANK_TRANSACTION)
                .fetch();

        return fetch.stream().map(transaction -> new FinancialRecord(
                transaction.getUuid().toString(),
                transaction.getBankName(),
                transaction.getDate(),
                transaction.getDescription(),
                BigDecimal.valueOf(transaction.getValue().doubleValue()),
                BigDecimal.valueOf(transaction.getValue().doubleValue()),
                transaction.getCategory()
        )).collect(Collectors.toList());
    }
}
