package com.moneymind.finance.infrastrucuture.postgres;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import org.jooq.*;
import org.jooq.Record;
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

import static org.jooq.impl.DSL.sum;

public class TransactionStore extends Store implements TransactionRepository {

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
    public List<FinancialRecord> insertTransactions(List<FinancialRecord> financialRecords) {
        try {
            final List<BankTransactionRecord> transactionRecords = financialRecords.stream().map(this::toRecord).toList();

            // Build multi-row INSERT with RETURNING
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

            // Execute with RETURNING to get generated UUIDs
            assert insert != null;
            Result<BankTransactionRecord> insertedRecords = insert.returning().fetch();

            // Return the inserted records with generated IDs
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
        final int sanitizedLimit = this.sanitizeLimit(query.limit());
        final int sanitizedCursor = this.sanitizeCursor(query.cursor());

        // Handle aggregation queries
        boolean hasGroupByColumn = query.aggregateByColumn() != null && !query.aggregateByColumn().isEmpty();
        if (query.aggregateByPeriod() != null && !query.aggregateByPeriod().isEmpty()) {
            if (hasGroupByColumn) {
                // Both present -> buildAggregatedQuery
                return buildAggregatedQuery(query);
            } else {
                // Only period -> searchByPeriod
                return searchByPeriod(query);
            }
        } else if (hasGroupByColumn) {
            // Only column -> searchByColumn
            return searchByColumn(query);
        }

        // Regular search (no aggregation) - Strategy Pattern Implementation
        QueryContext context = new QueryContext(dataSource, query, sanitizedLimit, sanitizedCursor);
        RegularSearchStrategy strategy = new RegularSearchStrategy(new QueryBuilderHelper(), new PaginationHandler());
        return strategy.execute(context);
    }

    private PagedResult<FinancialRecord> buildAggregatedQuery(final TransactionSearchQuery query) {
        // Determine the date truncation based on period (month or year)
        final String truncation = query.aggregateByPeriod().equalsIgnoreCase("year") ? "year" : "month";

        final SelectJoinStep<Record4<String, String, BigDecimal, OffsetDateTime>> fromClause = this.dataSource
                .select(
                        org.jooq.impl.DSL.field(
                                "TO_CHAR(DATE_TRUNC('" + truncation + "', {0}), {1})",
                                String.class,
                                BankTransaction.BANK_TRANSACTION.DATE,
                                truncation.equals("year") ? "YYYY" : "YYYY-MM"
                        ).as("period"),
                        BankTransaction.BANK_TRANSACTION.CATEGORY,
                        sum(BankTransaction.BANK_TRANSACTION.VALUE).as("total"),
                        org.jooq.impl.DSL.max(BankTransaction.BANK_TRANSACTION.DATE).as("max_date")
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record4<String, String, BigDecimal, OffsetDateTime>> where =
                applyDateFilters(fromClause, query.from(), query.to());

        Result<Record4<String, String, BigDecimal, OffsetDateTime>> results = Objects.requireNonNullElse(where, fromClause)
                .groupBy(
                        org.jooq.impl.DSL.field("DATE_TRUNC('" + truncation + "', {0})", BankTransaction.BANK_TRANSACTION.DATE),
                        BankTransaction.BANK_TRANSACTION.CATEGORY
                )
                .orderBy(org.jooq.impl.DSL.field("period").asc(), BankTransaction.BANK_TRANSACTION.CATEGORY.asc())
                .limit(sanitizeLimit(query.limit()) + 1)
                .fetch();

        List<FinancialRecord> records = results.stream()
                .map(record -> new FinancialRecord(
                        record.get("period", String.class),
                        record.get(BankTransaction.BANK_TRANSACTION.CATEGORY),
                        record.get("total", BigDecimal.class),
                        record.get("max_date", OffsetDateTime.class)
                ))
                .toList();

        return new PagedResult<>(records, query.limit(), null);
    }

    private <R extends org.jooq.Record> SelectConditionStep<R> applyDateFilters(
            SelectJoinStep<R> fromClause,
            String from,
            String to
    ) {
        SelectConditionStep<R> where = null;

        if(from != null && !from.isEmpty()) {
            where = fromClause.where(
                    BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(from))
            );
        }

        if(to != null && !to.isEmpty()) {
            if(where != null) {
                where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to)));
            } else {
                where = fromClause.where(
                        BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to))
                );
            }
        }

        return where;
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
        if ( hasMoreRecords ) {
            BankTransactionRecord rec = bankTransactionRecords.removeLast();
            newCursor = new String(
                    Base64.getEncoder().encode(String.valueOf(rec.getId()).getBytes(StandardCharsets.UTF_8)));
        }

        return new PagedResult<>(
                bankTransactionRecords.stream().map(TransactionStore::toModel).collect(Collectors.toList()),
                sanitizedLimit,
                newCursor
        );
    }

    private PagedResult<FinancialRecord> searchByColumn(TransactionSearchQuery query) {

        final SelectJoinStep<Record2<String, BigDecimal>> fromClause = this.dataSource
                .select(
                        Objects.requireNonNull(BankTransaction.BANK_TRANSACTION.field(query.aggregateByColumn())).cast(String.class),
                        sum(BankTransaction.BANK_TRANSACTION.VALUE)
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record2<String, BigDecimal>> where = applyDateFilters(fromClause, query.from(), query.to());

        Result<Record2<String, BigDecimal>> txByColumn;
        txByColumn = Objects.requireNonNullElse(where, fromClause)
                .groupBy(BankTransaction.BANK_TRANSACTION.field(query.aggregateByColumn()))
                .fetch();

        List<FinancialRecord> records = txByColumn.stream()
                .map(record -> new FinancialRecord(
                        record.get(0, String.class), record.get(1, BigDecimal.class))
                ).toList();

        return new PagedResult<>(
                records,
                query.limit(),
                null //  TODO: build cursor which is missing

        );
    }

    private PagedResult<FinancialRecord> searchByPeriod(TransactionSearchQuery query) {
        final int sanitizedLimit = this.sanitizeLimit(query.limit());
        final int sanitizedCursor = this.sanitizeCursor(query.cursor()); // TODO: parse cursor
        // Determine the date truncation based on a period (month or year)
        final String truncation = query.aggregateByPeriod().equalsIgnoreCase("year") ? "year" : "month";

        final SelectJoinStep<Record2<String, BigDecimal>> fromClause = this.dataSource
                .select(
                        org.jooq.impl.DSL.field(
                                "TO_CHAR(DATE_TRUNC('" + truncation + "', {0}), {1})",
                                String.class,
                                BankTransaction.BANK_TRANSACTION.DATE,
                                truncation.equals("year") ? "YYYY" : "YYYY-MM"
                        ).as("period"),
                        sum(BankTransaction.BANK_TRANSACTION.VALUE).as("total")
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record2<String, BigDecimal>> where = applyDateFilters(fromClause, query.from(), query.to());

        Result<Record2<String, BigDecimal>> txByPeriod = Objects.requireNonNullElse(where, fromClause)
                .groupBy(org.jooq.impl.DSL.field("DATE_TRUNC('" + truncation + "', {0})", BankTransaction.BANK_TRANSACTION.DATE))
                .orderBy(org.jooq.impl.DSL.field("period").asc())
                .limit(sanitizedLimit + 1)
                .fetch();

        String newCursor = null;
        if(txByPeriod.size() > sanitizedLimit ) {
            newCursor = buildCursor(txByPeriod.removeLast(), query.aggregateByColumn(), query.aggregateByPeriod());
        }

        List<FinancialRecord> records = txByPeriod.stream()
                .map(record -> new FinancialRecord(
                        record.get("period", String.class), record.get("total", BigDecimal.class))
                ).toList();

        return new PagedResult<>(
                records,
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
        List<BankTransactionRecord> transactionRecord =  this.dataSource.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.UUID.eq(id))
                .fetchInto(BankTransactionRecord.class);

        if(transactionRecord.isEmpty()) {
            return null;
        }

        return toModel(transactionRecord.getFirst());
    }

    private String buildCursor(final Record record, final String aggregatedByColumn, final String aggregatedByPeriod){
        final boolean hasGroupByColumn = aggregatedByColumn != null && !aggregatedByColumn.isEmpty();
        final boolean hasGroupByPeriod = aggregatedByPeriod != null && !aggregatedByPeriod.isEmpty();

        if(hasGroupByColumn && hasGroupByPeriod) {
            return new String(
                    Base64.getEncoder().encode((record.getValue("period") + "," + record.getValue(BankTransaction.BANK_TRANSACTION.CATEGORY)).getBytes(StandardCharsets.UTF_8)));
        }

        if(hasGroupByPeriod) {
            return new String(
                    Base64.getEncoder().encode(String.valueOf(record.getValue("period")).getBytes(StandardCharsets.UTF_8)));
        }

        if(hasGroupByColumn) {
            return new String(
                    Base64.getEncoder().encode(String.valueOf(record.getValue(BankTransaction.BANK_TRANSACTION.CATEGORY)).getBytes(StandardCharsets.UTF_8)));
        }

        return new String(
            Base64.getEncoder().encode((record.getValue(BankTransaction.BANK_TRANSACTION.DATE) + "," + record.getValue(BankTransaction.BANK_TRANSACTION.ID)).getBytes(StandardCharsets.UTF_8)));

    }

    // ========== Refactoring: Value Objects and Helper Classes ==========

    enum AggregationPeriod {
        YEAR("year", "YYYY"),
        MONTH("month", "YYYY-MM");

        private final String sqlTruncation;
        private final String dateFormat;

        AggregationPeriod(String sqlTruncation, String dateFormat) {
            this.sqlTruncation = sqlTruncation;
            this.dateFormat = dateFormat;
        }

        public String getSqlTruncation() {
            return sqlTruncation;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public static AggregationPeriod fromString(String period) {
            if (period == null || period.isEmpty()) {
                return null;
            }
            return period.equalsIgnoreCase("year") ? YEAR : MONTH;
        }
    }

    record QueryContext(
            DSLContext dsl,
            TransactionSearchQuery query,
            int sanitizedLimit,
            int sanitizedCursor
    ) {}

    static class PaginationHandler {
        String encodeCursor(String value) {
            return Base64.getEncoder()
                    .encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        <R extends Record> PagedResult<FinancialRecord> buildPagedResult(
                Result<R> results,
                int limit,
                java.util.function.Function<R, FinancialRecord> mapper,
                java.util.function.Function<R, String> cursorBuilder
        ) {
            String cursor = null;
            boolean hasMore = results.size() > limit;

            if (hasMore) {
                R lastRecord = results.remove(results.size() - 1);
                cursor = cursorBuilder.apply(lastRecord);
            }

            List<FinancialRecord> records = results.stream()
                    .map(mapper)
                    .collect(Collectors.toList());

            return new PagedResult<>(records, limit, cursor);
        }
    }

    static class QueryBuilderHelper {
        private boolean hasValue(String s) {
            return s != null && !s.isEmpty();
        }

        <R extends Record> SelectConditionStep<R> applyDateFilters(
                SelectJoinStep<R> fromClause,
                String from,
                String to
        ) {
            SelectConditionStep<R> where = null;

            if(from != null && !from.isEmpty()) {
                where = fromClause.where(
                        BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(from))
                );
            }

            if(to != null && !to.isEmpty()) {
                if(where != null) {
                    where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to)));
                } else {
                    where = fromClause.where(
                            BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(to))
                    );
                }
            }

            return where;
        }

        SelectConditionStep<BankTransactionRecord> applyCommonFilters(
                SelectConditionStep<BankTransactionRecord> where,
                TransactionSearchQuery searchQuery
        ) {
            if (hasValue(searchQuery.id())) {
                where = where.and(BankTransaction.BANK_TRANSACTION.UUID.eq(UUID.fromString(searchQuery.id())));
            }
            if (hasValue(searchQuery.category())) {
                where = where.and(BankTransaction.BANK_TRANSACTION.CATEGORY.eq(searchQuery.category()));
            }
            if (hasValue(searchQuery.bank())) {
                where = where.and(BankTransaction.BANK_TRANSACTION.BANK_NAME.eq(searchQuery.bank()));
            }
            if (hasValue(searchQuery.from())) {
                where = where.and(BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(searchQuery.from())));
            }
            if (hasValue(searchQuery.to())) {
                where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(searchQuery.to())));
            }
            return where;
        }

        List<SortField<?>> buildSortFields(String sort) {
            if (sort != null && sort.equals("ASC")) {
                return List.of(
                        BankTransaction.BANK_TRANSACTION.DATE.asc(),
                        BankTransaction.BANK_TRANSACTION.ID.asc()
                );
            }
            return List.of(
                    BankTransaction.BANK_TRANSACTION.DATE.desc(),
                    BankTransaction.BANK_TRANSACTION.ID.desc()
            );
        }
    }

    interface TransactionQueryStrategy {
        PagedResult<FinancialRecord> execute(QueryContext context);
        boolean canHandle(TransactionSearchQuery query);
    }

    static class RegularSearchStrategy implements TransactionQueryStrategy {
        private final QueryBuilderHelper queryBuilder;
        private final PaginationHandler paginationHandler;

        RegularSearchStrategy(QueryBuilderHelper queryBuilder, PaginationHandler paginationHandler) {
            this.queryBuilder = queryBuilder;
            this.paginationHandler = paginationHandler;
        }

        @Override
        public boolean canHandle(TransactionSearchQuery query) {
            boolean hasGroupByColumn = query.aggregateByColumn() != null && !query.aggregateByColumn().isEmpty();
            boolean hasGroupByPeriod = query.aggregateByPeriod() != null && !query.aggregateByPeriod().isEmpty();
            return !hasGroupByColumn && !hasGroupByPeriod;
        }

        @Override
        public PagedResult<FinancialRecord> execute(QueryContext ctx) {
            SelectConditionStep<BankTransactionRecord> where = ctx.dsl()
                    .selectFrom(BankTransaction.BANK_TRANSACTION)
                    .where(BankTransaction.BANK_TRANSACTION.ID.ge(ctx.sanitizedCursor()));

            where = queryBuilder.applyCommonFilters(where, ctx.query());

            List<SortField<?>> sortBy = queryBuilder.buildSortFields(ctx.query().sort());
            Result<BankTransactionRecord> results = where
                    .orderBy(sortBy)
                    .limit(ctx.sanitizedLimit() + 1)
                    .fetch();

            return paginationHandler.buildPagedResult(
                    results,
                    ctx.sanitizedLimit(),
                    TransactionStore::toModel,
                    rec -> paginationHandler.encodeCursor(String.valueOf(rec.getId()))
            );
        }
    }
}
