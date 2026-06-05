package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.AggregatedResult;
import com.moneymind.finance.domain.core.Cursor;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransactionStore extends Store implements TransactionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransactionStore.class);
    private static final String PLSQL_DUPLICATED_RECORD_ERROR_CODE = "23505";

    final DSLContext dataSource;
    private final TransactionQuery txQuery;

    public TransactionStore(final DSLContext dataSource) {
        this.dataSource = dataSource;
        this.txQuery = new TransactionQuery(dataSource);
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
    public List<FinancialRecord> insertTransactions(List<FinancialRecord> financialRecords) {
        try {
            final List<BankTransactionRecord> transactionRecords = financialRecords.stream().map(this::toRecord).toList();

            InsertValuesStep5<BankTransactionRecord, BigDecimal, String, OffsetDateTime, BigDecimal, String> insert = null;

            for (BankTransactionRecord record : transactionRecords) {
                insert = Objects.requireNonNullElseGet(insert, () -> dataSource.insertInto(BankTransaction.BANK_TRANSACTION,
                        BankTransaction.BANK_TRANSACTION.VALUE,
                        BankTransaction.BANK_TRANSACTION.DESCRIPTION,
                        BankTransaction.BANK_TRANSACTION.DATE,
                        BankTransaction.BANK_TRANSACTION.BALANCE,
                        BankTransaction.BANK_TRANSACTION.BANK_NAME)).values(record.getValue(), record.getDescription(), record.getDate(),
                        record.getBalance(), record.getBankName());
            }

            assert insert != null;
            Result<BankTransactionRecord> insertedRecords = insert.returning().fetch();

            return insertedRecords.stream()
                    .map(TransactionStore::toModel)
                    .collect(Collectors.toList());

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
            throw ex;
        }
    }

    @Override
    public PagedResult<FinancialRecord> search(final TransactionSearchQuery query) {
        return txQuery.search(query);
    }

    @Override
    public PagedResult<AggregatedResult> searchAggregated(final TransactionSearchQuery query) {
        return txQuery.searchAggregated(query);
    }

    @Override
    public PagedResult<FinancialRecord> fetchNoCategoriesTransaction(int limit, String cursor) {
        int sanitizedLimit = this.sanitizeLimit(limit);
        int sanitizedCursor = this.sanitizeCursor(cursor);

        List<BankTransactionRecord> bankTransactionRecords = this.dataSource.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.CATEGORY.isNull())
                .and(BankTransaction.BANK_TRANSACTION.ID.ge(sanitizedCursor))
                .limit(sanitizedLimit + 1)
                .fetchInto(BankTransactionRecord.class);

        String newCursor = null;
        boolean hasMoreRecords = !bankTransactionRecords.isEmpty() && bankTransactionRecords.size() >= sanitizedLimit + 1;
        if (hasMoreRecords) {
            BankTransactionRecord rec = bankTransactionRecords.removeLast();
            newCursor = Cursor.forId(rec.getId());
        }

        return new PagedResult<>(
                bankTransactionRecords.stream().map(TransactionStore::toModel).collect(Collectors.toList()),
                sanitizedLimit,
                newCursor
        );
    }

    @Override
    public void update(final UUID id, final String category) {
        this.dataSource.update(BankTransaction.BANK_TRANSACTION)
                .set(BankTransaction.BANK_TRANSACTION.CATEGORY, category)
                .where(BankTransaction.BANK_TRANSACTION.UUID.eq(id))
                .execute();
    }

    @Override
    public FinancialRecord getById(UUID id) {
        List<BankTransactionRecord> transactionRecord = this.dataSource.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.UUID.eq(id))
                .fetchInto(BankTransactionRecord.class);

        if (transactionRecord.isEmpty()) {
            return null;
        }

        return toModel(transactionRecord.getFirst());
    }
}
