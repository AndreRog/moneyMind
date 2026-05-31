package com.moneymind.domain.summary;

import com.moneymind.finance.domain.core.CategoryType;
import com.moneymind.finance.domain.summary.CategoryBreakdown;
import com.moneymind.finance.domain.summary.CategoryDelta;
import com.moneymind.finance.domain.summary.SubcategoryBreakdown;
import com.moneymind.finance.domain.summary.Summary;
import com.moneymind.finance.domain.summary.SummaryEngine;
import com.moneymind.finance.domain.summary.SummaryTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.moneymind.finance.domain.core.CategoryType.EXCLUDED;
import static com.moneymind.finance.domain.core.CategoryType.EXPENSE;
import static com.moneymind.finance.domain.core.CategoryType.INCOME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryEngineTest {

    private final SummaryEngine engine = new SummaryEngine();

    @Test
    void computesTotals() {
        List<SummaryTransaction> transactions = List.of(
                tx("1000", "Salary", "Monthly Salary", INCOME),
                tx("-300", "Housing", "Rent", EXPENSE),
                tx("-50", "Food", "Groceries", EXPENSE)
        );

        Summary summary = engine.compute(transactions);

        assertAmount("1000", summary.income());
        assertAmount("350", summary.expense());
        assertAmount("650", summary.savings());
    }

    @Test
    void excludedTransactionsAreInvisibleToTotalsAndBreakdowns() {
        List<SummaryTransaction> transactions = List.of(
                tx("1000", "Salary", "Monthly Salary", INCOME),
                tx("-500", "Transfers", "Own Account Transfer", EXCLUDED)
        );

        Summary summary = engine.compute(transactions);

        assertAmount("1000", summary.income());
        assertAmount("0", summary.expense());
        assertAmount("1000", summary.savings());

        assertTrue(summary.categories().stream().noneMatch(c -> c.categoryName().equals("Transfers")));
        assertTrue(summary.subcategories().stream().noneMatch(s -> s.subcategoryName().equals("Own Account Transfer")));
    }

    @Test
    void categoryRollupSumsToTotals() {
        List<SummaryTransaction> transactions = List.of(
                tx("2000", "Income", "Salary", INCOME),
                tx("500", "Income", "Bonus", INCOME),
                tx("-400", "Housing", "Rent", EXPENSE),
                tx("-100", "Food", "Groceries", EXPENSE)
        );

        Summary summary = engine.compute(transactions);

        BigDecimal incomeFromCategories = summary.categories().stream()
                .filter(c -> c.type() == INCOME)
                .map(CategoryBreakdown::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAmount(summary.income().toPlainString(), incomeFromCategories);

        BigDecimal expenseFromCategories = summary.categories().stream()
                .filter(c -> c.type() == EXPENSE)
                .map(CategoryBreakdown::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAmount(summary.expense().toPlainString(), expenseFromCategories);
    }

    @Test
    void subcategoryRollupSumsToCorrespondingCategoryTotal() {
        List<SummaryTransaction> transactions = List.of(
                tx("-200", "Housing", "Rent", EXPENSE),
                tx("-80", "Housing", "Electricity", EXPENSE),
                tx("-120", "Food", "Groceries", EXPENSE)
        );

        Summary summary = engine.compute(transactions);

        BigDecimal housingFromSubcats = summary.subcategories().stream()
                .filter(s -> s.categoryName().equals("Housing"))
                .map(SubcategoryBreakdown::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal housingCategory = summary.categories().stream()
                .filter(c -> c.categoryName().equals("Housing"))
                .map(CategoryBreakdown::total)
                .findFirst().orElse(BigDecimal.ZERO);

        assertAmount(housingCategory.toPlainString(), housingFromSubcats);
    }

    @Test
    void compareReturnsCategoryDeltas() {
        List<SummaryTransaction> period1 = List.of(
                tx("1000", "Salary", "Monthly Salary", INCOME),
                tx("-400", "Housing", "Rent", EXPENSE)
        );
        List<SummaryTransaction> period2 = List.of(
                tx("1200", "Salary", "Monthly Salary", INCOME),
                tx("-450", "Housing", "Rent", EXPENSE)
        );

        Summary base = engine.compute(period1);
        Summary compare = engine.compute(period2);

        List<CategoryDelta> deltas = engine.compare(base, compare);

        CategoryDelta salaryDelta = deltas.stream()
                .filter(d -> d.categoryName().equals("Salary"))
                .findFirst().orElseThrow();
        assertAmount("1000", salaryDelta.baseTotal());
        assertAmount("1200", salaryDelta.compareTotal());
        assertAmount("200", salaryDelta.delta());

        CategoryDelta housingDelta = deltas.stream()
                .filter(d -> d.categoryName().equals("Housing"))
                .findFirst().orElseThrow();
        assertAmount("400", housingDelta.baseTotal());
        assertAmount("450", housingDelta.compareTotal());
        assertAmount("50", housingDelta.delta());
    }

    @Test
    void compareIncludesCategoriesPresentInOnlyOnePeriod() {
        Summary base = engine.compute(List.of(
                tx("-200", "Housing", "Rent", EXPENSE)
        ));
        Summary compare = engine.compute(List.of(
                tx("-200", "Housing", "Rent", EXPENSE),
                tx("-60", "Transport", "Fuel", EXPENSE)
        ));

        List<CategoryDelta> deltas = engine.compare(base, compare);

        CategoryDelta transportDelta = deltas.stream()
                .filter(d -> d.categoryName().equals("Transport"))
                .findFirst().orElseThrow();
        assertAmount("0", transportDelta.baseTotal());
        assertAmount("60", transportDelta.compareTotal());
        assertAmount("60", transportDelta.delta());
    }

    private static SummaryTransaction tx(String amount, String category, String subcategory, CategoryType type) {
        return new SummaryTransaction(new BigDecimal(amount), category, subcategory, type);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but got " + actual);
    }
}
