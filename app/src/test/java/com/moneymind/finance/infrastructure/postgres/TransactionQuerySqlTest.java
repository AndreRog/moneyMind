package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.core.TransactionSearchQuery;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rendered-SQL tests: assert the SQL jOOQ builds for each query shape
 * and filter without touching a real database.
 */
class TransactionQuerySqlTest {

    private TransactionQuery txQuery;

    @BeforeEach
    void setUp() {
        DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
        txQuery = new TransactionQuery(dsl);
    }

    // ─── regular search ───────────────────────────────────────────────────────

    @Test
    void search_baseQuery_selectsFromBankTransaction() {
        String sql = sql(txQuery.buildSearchSelect(query().build()));
        assertContains(sql, "bank_transaction");
    }

    @Test
    void search_defaultSort_ordersByDateDescIdDesc() {
        String sql = sql(txQuery.buildSearchSelect(query().build()));
        assertContains(sql, "order by");
        assertContains(sql, "\"date\" desc");
        assertContains(sql, "\"id\" desc");
    }

    @Test
    void search_ascSort_ordersByDateAscIdAsc() {
        String sql = sql(txQuery.buildSearchSelect(query().sort("ASC").build()));
        assertContains(sql, "\"date\" asc");
        assertContains(sql, "\"id\" asc");
    }

    @Test
    void search_limitIsAppliedPlusOne() {
        String sql = sql(txQuery.buildSearchSelect(query().limit(5).build()));
        assertContains(sql, "fetch next 6 rows only");
    }

    @Test
    void search_categoryFilter_addsCategoryCondition() {
        String sql = sql(txQuery.buildSearchSelect(query().category("Food").build()));
        assertContains(sql, "\"category\" = 'food'");
    }

    @Test
    void search_bankFilter_addsBankCondition() {
        String sql = sql(txQuery.buildSearchSelect(query().bank("MY_BANK").build()));
        assertContains(sql, "\"bank_name\" = 'my_bank'");
    }

    @Test
    void search_idFilter_addsUuidCondition() {
        String uuid = "00000000-0000-0000-0000-000000000001";
        String sql = sql(txQuery.buildSearchSelect(query().id(uuid).build()));
        assertContains(sql, uuid.toLowerCase());
    }

    @Test
    void search_fromDateFilter_addsGreaterOrEqualCondition() {
        String sql = sql(txQuery.buildSearchSelect(query().from("2024-01-01T00:00:00Z").build()));
        assertContains(sql, "\"date\" >= ");
        assertContains(sql, "2024-01-01");
    }

    @Test
    void search_toDateFilter_addsLessOrEqualCondition() {
        String sql = sql(txQuery.buildSearchSelect(query().to("2024-12-31T23:59:59Z").build()));
        assertContains(sql, "\"date\" <= ");
        assertContains(sql, "2024-12-31");
    }

    @Test
    void search_dateRange_appliesBothConditions() {
        String sql = sql(txQuery.buildSearchSelect(
                query().from("2024-01-01T00:00:00Z").to("2024-12-31T23:59:59Z").build()));
        assertContains(sql, "\"date\" >= ");
        assertContains(sql, "\"date\" <= ");
    }

    @Test
    void search_excludeCategories_addsNotInCondition() {
        String sql = sql(txQuery.buildSearchSelect(
                query().excludeCategories(List.of("Excluded", "Hidden")).build()));
        assertContains(sql, "not in");
        assertContains(sql, "'excluded'");
        assertContains(sql, "'hidden'");
    }

    // ─── aggregate by period ─────────────────────────────────────────────────

    @Test
    void byPeriod_month_usesTruncMonth() {
        String sql = sql(txQuery.buildByPeriodSelect(query().aggregateByPeriod("month").build()));
        assertContains(sql, "date_trunc('month'");
        assertContains(sql, "yyyy-mm");
        assertContains(sql, "group by");
    }

    @Test
    void byPeriod_year_usesTruncYear() {
        String sql = sql(txQuery.buildByPeriodSelect(query().aggregateByPeriod("year").build()));
        assertContains(sql, "date_trunc('year'");
        assertContains(sql, "yyyy");
        assertContains(sql, "group by");
    }

    @Test
    void byPeriod_limitIsAppliedPlusOne() {
        String sql = sql(txQuery.buildByPeriodSelect(query().aggregateByPeriod("month").limit(3).build()));
        assertContains(sql, "fetch next 4 rows only");
    }

    @Test
    void byPeriod_dateRangeFilter_appliesBothConditions() {
        String sql = sql(txQuery.buildByPeriodSelect(
                query().aggregateByPeriod("month")
                        .from("2024-01-01T00:00:00Z")
                        .to("2024-06-30T23:59:59Z")
                        .build()));
        assertContains(sql, "\"date\" >= ");
        assertContains(sql, "\"date\" <= ");
    }

    // ─── aggregate by column ─────────────────────────────────────────────────

    @Test
    void byColumn_groupsByRequestedColumn() {
        String sql = sql(txQuery.buildByColumnSelect(query().aggregateByColumn("category").build()));
        assertContains(sql, "\"category\"");
        assertContains(sql, "group by");
    }

    @Test
    void byColumn_dateRangeFilter_appliesBothConditions() {
        String sql = sql(txQuery.buildByColumnSelect(
                query().aggregateByColumn("category")
                        .from("2024-01-01T00:00:00Z")
                        .to("2024-12-31T23:59:59Z")
                        .build()));
        assertContains(sql, "\"date\" >= ");
        assertContains(sql, "\"date\" <= ");
    }

    // ─── aggregate by period + column ────────────────────────────────────────

    @Test
    void byPeriodAndColumn_groupsByPeriodAndCategory() {
        String sql = sql(txQuery.buildByPeriodAndColumnSelect(
                query().aggregateByPeriod("month").aggregateByColumn("category").build()));
        assertContains(sql, "date_trunc('month'");
        assertContains(sql, "yyyy-mm");
        assertContains(sql, "group by");
        assertContains(sql, "\"category\"");
    }

    @Test
    void byPeriodAndColumn_limitIsAppliedPlusOne() {
        String sql = sql(txQuery.buildByPeriodAndColumnSelect(
                query().aggregateByPeriod("month").aggregateByColumn("category").limit(2).build()));
        assertContains(sql, "fetch next 3 rows only");
    }

    @Test
    void byPeriodAndColumn_dateRangeFilter_appliesBothConditions() {
        String sql = sql(txQuery.buildByPeriodAndColumnSelect(
                query().aggregateByPeriod("month").aggregateByColumn("category")
                        .from("2024-01-01T00:00:00Z")
                        .to("2024-06-30T23:59:59Z")
                        .build()));
        assertContains(sql, "\"date\" >= ");
        assertContains(sql, "\"date\" <= ");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static TransactionSearchQuery.Builder query() {
        return TransactionSearchQuery.builder().limit(10);
    }

    private static String sql(Select<?> select) {
        return select.getSQL(ParamType.INLINED).toLowerCase();
    }

    private static void assertContains(String sql, String fragment) {
        assertTrue(sql.contains(fragment.toLowerCase()),
                "Expected SQL to contain: " + fragment + "\nActual SQL: " + sql);
    }
}
