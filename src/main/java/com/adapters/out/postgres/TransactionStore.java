package com.adapters.out.postgres;

import com.app.domain.SearchResponse;
import com.app.domain.core.FinancialRecord;
import com.app.exceptions.ExceptionCode;
import com.app.exceptions.StoreException;
import com.app.ports.TransactionRepository;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransactionStore extends Store implements TransactionRepository  {

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

    public static FinancialRecord toModel(BankTransactionRecord record) {
        return new FinancialRecord(
                record.getUuid().toString(),
                record.getBankName(),
                record.getDate(),
                record.getDescription(),
                BigDecimal.valueOf(record.getValue().doubleValue()),
                BigDecimal.valueOf(record.getBalance().doubleValue()),
                record.getCategory()
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
    public SearchResponse<FinancialRecord> search(String id, String category, String bank, String from, String to,
                                                  int limit, String cursor) {

        final int sanitizedLimit = this.sanitizeLimit(limit);
        final int sanitizedCursor = this.sanitizeCursor(cursor);
        SelectConditionStep<BankTransactionRecord> where = this.dataSource
                .selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.ID.ge(sanitizedCursor));

        if(id != null && !id.isEmpty()) {
            where = where.and(BankTransaction.BANK_TRANSACTION.UUID.eq(UUID.fromString(id)));
        }

        if(category != null && !category.isEmpty()) {
            where = where.and(BankTransaction.BANK_TRANSACTION.CATEGORY.eq(category));
        }

        if(bank != null && !bank.isEmpty()) {
            where = where.and(BankTransaction.BANK_TRANSACTION.BANK_NAME.eq(bank));
        }

        if(from != null && !from.isEmpty()) {
            where = where.and(BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(from)));

        }

        if(to != null && !to.isEmpty()) {
            where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to)));

        }

        final Result<BankTransactionRecord> financialRecords = where.limit(sanitizedLimit + 1).fetch();

        String newCursor = null;

        boolean hasMoreRecords = !financialRecords.isEmpty() && financialRecords.size() >= sanitizedLimit + 1;
        if ( hasMoreRecords ) {
            BankTransactionRecord rec = financialRecords.remove(financialRecords.size() - 1);
            newCursor = new String(
                    Base64.getEncoder().encode(String.valueOf(rec.getId()).getBytes(StandardCharsets.UTF_8)));
        }

        return new SearchResponse<>(
                financialRecords.stream().map(TransactionStore::toModel).collect(Collectors.toList()),
                sanitizedLimit,
                newCursor
        );
    }
}
