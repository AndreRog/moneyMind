package com.moneymind.finance.adapters.out.postgres;

import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.infrastrucuture.postgres.ExceptionCode;
import com.moneymind.finance.infrastrucuture.postgres.StoreException;
import com.moneymind.finance.ports.TransactionRepository;
import org.jooq.*;
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
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.sumDistinct;

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
    public SearchResponse<FinancialRecord> search(String id, String category, String dimension, String bank,
                                                  String from, String to, int limit, String cursor,
                                                  String sort) {

        final int sanitizedLimit = this.sanitizeLimit(limit);
        final int sanitizedCursor = this.sanitizeCursor(cursor);

        if(dimension != null && !dimension.isEmpty()) {
            return searchByDimension(dimension, from, to);
        }

        SelectConditionStep<BankTransactionRecord> where = this.dataSource.
                selectFrom(BankTransaction.BANK_TRANSACTION)
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

        List<SortField<?>> sortBy = List.of(BankTransaction.BANK_TRANSACTION.DATE.desc(), BankTransaction.BANK_TRANSACTION.ID.desc());
        if( sort != null && sort.equals("ASC")) {
            sortBy = List.of(BankTransaction.BANK_TRANSACTION.DATE.asc(),  BankTransaction.BANK_TRANSACTION.ID.asc());
        }

        final Result<BankTransactionRecord> financialRecords = where.orderBy(sortBy)
                .limit(sanitizedLimit + 1)
                .fetch();

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

    private SearchResponse<FinancialRecord> searchByDimension(String dimension, String from, String to) {

        final SelectJoinStep<Record2<String, BigDecimal>> fromClause = this.dataSource
                .select(
                        Objects.requireNonNull(BankTransaction.BANK_TRANSACTION.field(dimension)).cast(String.class),
                        sumDistinct(BankTransaction.BANK_TRANSACTION.VALUE)
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record2<String, BigDecimal>> where = null;
        if(from != null && !from.isEmpty()) {
            where = fromClause.where(
                    BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(from))
            );
        }

        if(to != null && !to.isEmpty()) {
            if( where != null) {
                where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to)));
            }else {
                where = fromClause.where(
                        BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(to))
                );
            }
        }

        Result<Record2<String, BigDecimal>> txByCategory;
        txByCategory = Objects.requireNonNullElse(where, fromClause)
                .groupBy(BankTransaction.BANK_TRANSACTION.CATEGORY)
                .fetch();

        List<FinancialRecord> records = txByCategory.stream()
                .map(record -> new FinancialRecord(
                        record.get(0, String.class), record.get(1, BigDecimal.class))
                ).toList();

        return new SearchResponse<>(
                records,
                100,
                null
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
        List<BankTransactionRecord> transactionRecord =  this.dataSource.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.UUID.eq(id))
                .fetchInto(BankTransactionRecord.class);

        if(transactionRecord.isEmpty()) {
            return null;
        }

        return toModel(transactionRecord.get(0));
    }
}
