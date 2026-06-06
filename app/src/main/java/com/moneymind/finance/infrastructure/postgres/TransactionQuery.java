package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.AggregatedResult;
import com.moneymind.finance.domain.core.AggregationPeriod;
import com.moneymind.finance.domain.core.Cursor;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.sum;

class TransactionQuery extends Store {

    private final DSLContext dsl;

    TransactionQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ─── public API ───────────────────────────────────────────────────────────

    PagedResult<FinancialRecord> fetchNoCategoriesTransaction(int limit, String cursor) {
        int sanitizedLimit = sanitizeLimit(limit);
        int sanitizedCursor = sanitizeCursor(cursor);

        List<BankTransactionRecord> records = dsl.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.CATEGORY.isNull())
                .and(BankTransaction.BANK_TRANSACTION.ID.ge(sanitizedCursor))
                .limit(sanitizedLimit + 1)
                .fetchInto(BankTransactionRecord.class);

        String newCursor = null;
        if (!records.isEmpty() && records.size() >= sanitizedLimit + 1) {
            newCursor = Cursor.forId(records.removeLast().getId());
        }

        return new PagedResult<>(
                records.stream().map(TransactionStore::toModel).collect(Collectors.toList()),
                sanitizedLimit,
                newCursor
        );
    }

    PagedResult<FinancialRecord> search(TransactionSearchQuery query) {
        int limit = sanitizeLimit(query.limit());
        Result<BankTransactionRecord> results = buildSearchSelect(query).fetch();

        String newCursor = null;
        if (results.size() > limit) {
            newCursor = Cursor.forId(results.remove(results.size() - 1).getId());
        }

        return new PagedResult<>(
                results.stream().map(TransactionStore::toModel).collect(Collectors.toList()),
                limit,
                newCursor
        );
    }

    PagedResult<AggregatedResult> searchAggregated(TransactionSearchQuery query) {
        boolean hasPeriod = hasValue(query.aggregateByPeriod());
        boolean hasColumn = hasValue(query.aggregateByColumn());

        if (hasPeriod && hasColumn) return byPeriodAndColumn(query);
        if (hasPeriod) return byPeriod(query);
        return byColumn(query);
    }

    // ─── package-private builders (for rendered-SQL tests) ───────────────────

    Select<BankTransactionRecord> buildSearchSelect(TransactionSearchQuery query) {
        int limit = sanitizeLimit(query.limit());
        int cursor = sanitizeCursor(query.cursor());

        SelectConditionStep<BankTransactionRecord> q = dsl
                .selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.ID.ge(cursor));

        q = applyCommonFilters(q, query);
        return q.orderBy(buildSortFields(query.sort())).limit(limit + 1);
    }

    Select<?> buildByPeriodSelect(TransactionSearchQuery query) {
        AggregationPeriod period = AggregationPeriod.fromString(query.aggregateByPeriod());
        String trunc = period.getSqlTruncation();
        String fmt = period.getDateFormat();
        int limit = sanitizeLimit(query.limit());

        SelectJoinStep<Record2<String, BigDecimal>> from = dsl
                .select(
                        org.jooq.impl.DSL.field(
                                "TO_CHAR(DATE_TRUNC('" + trunc + "', {0}), '" + fmt + "')",
                                String.class,
                                BankTransaction.BANK_TRANSACTION.DATE
                        ).as("period"),
                        sum(BankTransaction.BANK_TRANSACTION.VALUE).as("total")
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record2<String, BigDecimal>> where =
                applyDateFilters(from, query.from(), query.to());

        return (where != null ? where : from)
                .groupBy(org.jooq.impl.DSL.field("DATE_TRUNC('" + trunc + "', {0})", BankTransaction.BANK_TRANSACTION.DATE))
                .orderBy(org.jooq.impl.DSL.field("period").asc())
                .limit(limit + 1);
    }

    Select<?> buildByColumnSelect(TransactionSearchQuery query) {
        SelectJoinStep<Record2<String, BigDecimal>> from = dsl
                .select(
                        BankTransaction.BANK_TRANSACTION.field(query.aggregateByColumn(), String.class),
                        sum(BankTransaction.BANK_TRANSACTION.VALUE).as("total")
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record2<String, BigDecimal>> where =
                applyDateFilters(from, query.from(), query.to());

        return (where != null ? where : from)
                .groupBy(BankTransaction.BANK_TRANSACTION.field(query.aggregateByColumn()));
    }

    Select<?> buildByPeriodAndColumnSelect(TransactionSearchQuery query) {
        AggregationPeriod period = AggregationPeriod.fromString(query.aggregateByPeriod());
        String trunc = period.getSqlTruncation();
        String fmt = period.getDateFormat();

        SelectJoinStep<Record3<String, String, BigDecimal>> from = dsl
                .select(
                        org.jooq.impl.DSL.field(
                                "TO_CHAR(DATE_TRUNC('" + trunc + "', {0}), '" + fmt + "')",
                                String.class,
                                BankTransaction.BANK_TRANSACTION.DATE
                        ).as("period"),
                        BankTransaction.BANK_TRANSACTION.CATEGORY,
                        sum(BankTransaction.BANK_TRANSACTION.VALUE).as("total")
                )
                .from(BankTransaction.BANK_TRANSACTION);

        SelectConditionStep<Record3<String, String, BigDecimal>> where =
                applyDateFilters(from, query.from(), query.to());

        return (where != null ? where : from)
                .groupBy(
                        org.jooq.impl.DSL.field("DATE_TRUNC('" + trunc + "', {0})", BankTransaction.BANK_TRANSACTION.DATE),
                        BankTransaction.BANK_TRANSACTION.CATEGORY
                )
                .orderBy(
                        org.jooq.impl.DSL.field("period").asc(),
                        BankTransaction.BANK_TRANSACTION.CATEGORY.asc()
                )
                .limit(sanitizeLimit(query.limit()) + 1);
    }

    // ─── private execution wrappers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private PagedResult<AggregatedResult> byPeriod(TransactionSearchQuery query) {
        int limit = sanitizeLimit(query.limit());
        Result<Record2<String, BigDecimal>> results =
                (Result<Record2<String, BigDecimal>>) buildByPeriodSelect(query).fetch();

        String newCursor = null;
        if (results.size() > limit) {
            Record2<String, BigDecimal> last = results.removeLast();
            newCursor = Cursor.forPeriod(last.get("period", String.class));
        }

        return new PagedResult<>(
                results.stream()
                        .map(r -> new AggregatedResult(r.get("period", String.class), null, r.get("total", BigDecimal.class)))
                        .toList(),
                limit,
                newCursor
        );
    }

    @SuppressWarnings("unchecked")
    private PagedResult<AggregatedResult> byColumn(TransactionSearchQuery query) {
        Result<Record2<String, BigDecimal>> results =
                (Result<Record2<String, BigDecimal>>) buildByColumnSelect(query).fetch();

        List<AggregatedResult> records = results.stream()
                .map(r -> new AggregatedResult(null, r.get(0, String.class), r.get("total", BigDecimal.class)))
                .toList();

        return new PagedResult<>(records, query.limit(), null);
    }

    @SuppressWarnings("unchecked")
    private PagedResult<AggregatedResult> byPeriodAndColumn(TransactionSearchQuery query) {
        Result<Record3<String, String, BigDecimal>> results =
                (Result<Record3<String, String, BigDecimal>>) buildByPeriodAndColumnSelect(query).fetch();

        List<AggregatedResult> records = results.stream()
                .map(r -> new AggregatedResult(
                        r.get("period", String.class),
                        r.get(BankTransaction.BANK_TRANSACTION.CATEGORY),
                        r.get("total", BigDecimal.class)
                ))
                .toList();

        return new PagedResult<>(records, query.limit(), null);
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private <R extends Record> SelectConditionStep<R> applyDateFilters(
            SelectJoinStep<R> from, String fromDate, String toDate) {
        SelectConditionStep<R> where = null;
        if (hasValue(fromDate)) {
            where = from.where(BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(fromDate)));
        }
        if (hasValue(toDate)) {
            where = where != null
                    ? where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(toDate)))
                    : from.where(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(toDate)));
        }
        return where;
    }

    private SelectConditionStep<BankTransactionRecord> applyCommonFilters(
            SelectConditionStep<BankTransactionRecord> where, TransactionSearchQuery q) {
        if (hasValue(q.id()))
            where = where.and(BankTransaction.BANK_TRANSACTION.UUID.eq(UUID.fromString(q.id())));
        if (hasValue(q.category()))
            where = where.and(BankTransaction.BANK_TRANSACTION.CATEGORY.eq(q.category()));
        if (hasValue(q.bank()))
            where = where.and(BankTransaction.BANK_TRANSACTION.BANK_NAME.eq(q.bank()));
        if (hasValue(q.from()))
            where = where.and(BankTransaction.BANK_TRANSACTION.DATE.greaterOrEqual(OffsetDateTime.parse(q.from())));
        if (hasValue(q.to()))
            where = where.and(BankTransaction.BANK_TRANSACTION.DATE.lessOrEqual(OffsetDateTime.parse(q.to())));
        if (q.excludeCategories() != null && !q.excludeCategories().isEmpty())
            where = where.and(BankTransaction.BANK_TRANSACTION.CATEGORY.notIn(q.excludeCategories()));
        return where;
    }

    private List<SortField<?>> buildSortFields(String sort) {
        if ("ASC".equals(sort)) {
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

    private boolean hasValue(String s) {
        return s != null && !s.isEmpty();
    }
}
