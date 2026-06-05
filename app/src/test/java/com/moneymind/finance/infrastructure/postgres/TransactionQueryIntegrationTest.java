package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.AggregatedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.persistence.PersistenceHarness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionQueryIntegrationTest extends PersistenceHarness {

    private TransactionQuery txQuery;

    private static final OffsetDateTime JAN = OffsetDateTime.parse("2024-01-15T12:00:00Z");
    private static final OffsetDateTime FEB = OffsetDateTime.parse("2024-02-15T12:00:00Z");
    private static final OffsetDateTime MAR = OffsetDateTime.parse("2024-03-15T12:00:00Z");

    @BeforeEach
    void init() {
        txQuery = new TransactionQuery(dsl);
    }

    // ─── search ──────────────────────────────────────────────────────────────

    @Test
    void search_returnsAllRows_whenNoFilters() {
        seedTransaction(bd("10"), bd("100"), "tx1", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "tx2", "BANK_B", FEB, "Transport");

        PagedResult<FinancialRecord> result = txQuery.search(query().build());

        assertEquals(2, result.list().size());
    }

    @Test
    void search_categoryFilter_returnsOnlyMatchingRows() {
        seedTransaction(bd("10"), bd("100"), "food tx", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "transport tx", "BANK_B", FEB, "Transport");

        PagedResult<FinancialRecord> result = txQuery.search(query().category("Food").build());

        assertEquals(1, result.list().size());
        assertEquals("Food", result.list().getFirst().getCategory());
    }

    @Test
    void search_bankFilter_returnsOnlyMatchingRows() {
        seedTransaction(bd("10"), bd("100"), "tx1", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "tx2", "BANK_B", FEB, "Food");

        PagedResult<FinancialRecord> result = txQuery.search(query().bank("BANK_A").build());

        assertEquals(1, result.list().size());
        assertEquals("BANK_A", result.list().getFirst().getBankName());
    }

    @Test
    void search_fromDateFilter_excludesEarlierRows() {
        seedTransaction(bd("10"), bd("100"), "old", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "new", "BANK_A", MAR, "Food");

        PagedResult<FinancialRecord> result = txQuery.search(
                query().from("2024-02-01T00:00:00Z").build());

        assertEquals(1, result.list().size());
        assertEquals("new", result.list().getFirst().getDescription());
    }

    @Test
    void search_toDateFilter_excludesLaterRows() {
        seedTransaction(bd("10"), bd("100"), "old", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "new", "BANK_A", MAR, "Food");

        PagedResult<FinancialRecord> result = txQuery.search(
                query().to("2024-02-01T00:00:00Z").build());

        assertEquals(1, result.list().size());
        assertEquals("old", result.list().getFirst().getDescription());
    }

    @Test
    void search_excludeCategories_omitsMatchingRows() {
        seedTransaction(bd("10"), bd("100"), "food tx", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "excluded tx", "BANK_A", FEB, "Excluded");

        PagedResult<FinancialRecord> result = txQuery.search(
                query().excludeCategories(List.of("Excluded")).build());

        assertEquals(1, result.list().size());
        assertEquals("Food", result.list().getFirst().getCategory());
    }

    @Test
    void search_pagination_cursorAdvancesToNextPage() {
        seedTransaction(bd("10"), bd("100"), "tx1", "BANK_A", MAR, "Food");
        seedTransaction(bd("20"), bd("200"), "tx2", "BANK_A", FEB, "Food");
        seedTransaction(bd("30"), bd("300"), "tx3", "BANK_A", JAN, "Food");

        PagedResult<FinancialRecord> page1 = txQuery.search(query().limit(2).build());
        assertEquals(2, page1.list().size());
        assertNotNull(page1.cursor(), "cursor should point to page 2");

        PagedResult<FinancialRecord> page2 = txQuery.search(query().limit(2).cursor(page1.cursor()).build());
        assertEquals(1, page2.list().size());
        assertNull(page2.cursor(), "no further pages");
    }

    @Test
    void search_ascSort_returnsOldestFirst() {
        seedTransaction(bd("10"), bd("100"), "old", "BANK_A", JAN, "Food");
        seedTransaction(bd("20"), bd("200"), "new", "BANK_A", MAR, "Food");

        PagedResult<FinancialRecord> result = txQuery.search(query().sort("ASC").build());

        assertEquals("old", result.list().getFirst().getDescription());
        assertEquals("new", result.list().getLast().getDescription());
    }

    // ─── aggregate by period ─────────────────────────────────────────────────

    @Test
    void searchAggregated_byMonth_groupsCorrectly() {
        seedTransaction(bd("100"), bd("1000"), "jan tx", "BANK_A", JAN, null);
        seedTransaction(bd("50"), bd("950"), "jan tx2", "BANK_A", JAN, null);
        seedTransaction(bd("200"), bd("1200"), "feb tx", "BANK_A", FEB, null);

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month").build());

        assertEquals(2, result.list().size());
        AggregatedResult jan = result.list().stream()
                .filter(r -> "2024-01".equals(r.period())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("150").compareTo(jan.total()));
        assertNull(jan.groupKey());
    }

    @Test
    void searchAggregated_byYear_groupsCorrectly() {
        seedTransaction(bd("100"), bd("1000"), "tx1", "BANK_A", JAN, null);
        seedTransaction(bd("200"), bd("1200"), "tx2", "BANK_A", MAR, null);

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("year").build());

        assertEquals(1, result.list().size());
        assertEquals("2024", result.list().getFirst().period());
        assertEquals(0, new BigDecimal("300").compareTo(result.list().getFirst().total()));
    }

    @Test
    void searchAggregated_byPeriod_cursorNonNullWhenMoreResults() {
        seedTransaction(bd("100"), bd("1000"), "jan", "BANK_A", JAN, null);
        seedTransaction(bd("200"), bd("1200"), "feb", "BANK_A", FEB, null);
        seedTransaction(bd("300"), bd("1500"), "mar", "BANK_A", MAR, null);

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month").limit(2).build());

        assertEquals(2, result.list().size());
        assertNotNull(result.cursor(), "cursor indicates more results exist");
    }

    @Test
    void searchAggregated_byPeriod_cursorNullWhenAllResultsFit() {
        seedTransaction(bd("100"), bd("1000"), "jan", "BANK_A", JAN, null);
        seedTransaction(bd("200"), bd("1200"), "feb", "BANK_A", FEB, null);

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month").limit(10).build());

        assertEquals(2, result.list().size());
        assertNull(result.cursor());
    }

    @Test
    void searchAggregated_byPeriod_dateRangeFilter_honoured() {
        seedTransaction(bd("100"), bd("1000"), "jan", "BANK_A", JAN, null);
        seedTransaction(bd("200"), bd("1200"), "mar", "BANK_A", MAR, null);

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month")
                        .from("2024-02-01T00:00:00Z")
                        .build());

        assertEquals(1, result.list().size());
        assertEquals("2024-03", result.list().getFirst().period());
    }

    // ─── aggregate by column ─────────────────────────────────────────────────

    @Test
    void searchAggregated_byColumn_groupsByCategory() {
        seedTransaction(bd("100"), bd("1000"), "food1", "BANK_A", JAN, "Food");
        seedTransaction(bd("50"), bd("950"), "food2", "BANK_A", FEB, "Food");
        seedTransaction(bd("200"), bd("1200"), "transport", "BANK_A", MAR, "Transport");

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByColumn("category").build());

        assertEquals(2, result.list().size());
        AggregatedResult food = result.list().stream()
                .filter(r -> "Food".equals(r.groupKey())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("150").compareTo(food.total()));
        assertNull(food.period());
    }

    @Test
    void searchAggregated_byColumn_returnsCursorNull() {
        seedTransaction(bd("100"), bd("1000"), "tx", "BANK_A", JAN, "Food");

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByColumn("category").build());

        assertNull(result.cursor(), "aggregate-by-column never returns a cursor");
    }

    // ─── aggregate by period + column ────────────────────────────────────────

    @Test
    void searchAggregated_byPeriodAndColumn_groupsByBoth() {
        seedTransaction(bd("100"), bd("1000"), "jan food", "BANK_A", JAN, "Food");
        seedTransaction(bd("50"), bd("950"), "jan transport", "BANK_A", JAN, "Transport");
        seedTransaction(bd("200"), bd("1200"), "feb food", "BANK_A", FEB, "Food");

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month").aggregateByColumn("category").build());

        assertEquals(3, result.list().size());
        AggregatedResult janFood = result.list().stream()
                .filter(r -> "2024-01".equals(r.period()) && "Food".equals(r.groupKey()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(janFood.total()));
    }

    @Test
    void searchAggregated_byPeriodAndColumn_returnsCursorNull() {
        seedTransaction(bd("100"), bd("1000"), "tx", "BANK_A", JAN, "Food");

        PagedResult<AggregatedResult> result = txQuery.searchAggregated(
                query().aggregateByPeriod("month").aggregateByColumn("category").build());

        assertNull(result.cursor(), "aggregate-by-period+column never returns a cursor");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static TransactionSearchQuery.Builder query() {
        return TransactionSearchQuery.builder().limit(10);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
