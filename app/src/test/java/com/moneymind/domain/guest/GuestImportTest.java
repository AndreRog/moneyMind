package com.moneymind.domain.guest;

import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.core.CategoryType;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.guest.CategorySlice;
import com.moneymind.finance.domain.guest.GuestImport;
import com.moneymind.finance.domain.guest.GuestReview;
import com.moneymind.finance.domain.guest.MonthlyReview;
import com.moneymind.finance.domain.ports.BankRegistry;
import com.moneymind.finance.domain.ports.CategoryRepository;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionsParser;
import com.moneymind.finance.domain.summary.SummaryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class GuestImportTest {

    private final BankRegistry bankRegistry = Mockito.mock(BankRegistry.class);
    private final TransactionsParser parser = Mockito.mock(TransactionsParser.class);
    private final TransactionClassifier classifier = Mockito.mock(TransactionClassifier.class);
    private final CategoryRepository categoryRepository = Mockito.mock(CategoryRepository.class);
    private final SummaryEngine summaryEngine = new SummaryEngine();

    private GuestImport guestImport;

    @BeforeEach
    void beforeEach() {
        this.guestImport = new GuestImport(bankRegistry, classifier, categoryRepository, summaryEngine);
        Mockito.when(categoryRepository.findCategories(null)).thenReturn(systemCategories());
    }

    @Test
    void returnsIncomeExpenseAndSavingsTotalsForTheFilesMonth() throws Exception {
        FinancialRecord salary = record("2026-05-31", "Salário", new BigDecimal("2450.00"));
        FinancialRecord rent = record("2026-05-02", "Renda", new BigDecimal("-750.00"));
        FinancialRecord groceries = record("2026-05-18", "Continente", new BigDecimal("-63.15"));

        givenDetectedBank("CGD", salary, rent, groceries);
        givenClassifications(
                classified("SALARY", salary),
                classified("RENT", rent),
                classified("GROCERIES", groceries));

        GuestReview review = guestImport.execute(csv());

        assertEquals(1, review.months().size());
        MonthlyReview may = review.months().get(0);
        assertEquals("2026-05", may.period());
        assertAmount("2450.00", may.income());
        assertAmount("813.15", may.expense());
        assertAmount("1636.85", may.savings());
        assertEquals(3, may.countedTransactions());
    }

    @Test
    void excludedTransfersAreLeftOutOfTotalsAndOnlyCounted() throws Exception {
        FinancialRecord salary = record("2026-05-31", "Salário", new BigDecimal("2450.00"));
        FinancialRecord transfer = record("2026-05-28", "Transferência p/ Poupança", new BigDecimal("-500.00"));
        FinancialRecord rent = record("2026-05-02", "Renda", new BigDecimal("-750.00"));

        givenDetectedBank("CGD", salary, transfer, rent);
        givenClassifications(
                classified("SALARY", salary),
                classified("TRANSFER BETWEEN ACCOUNTS", transfer),
                classified("RENT", rent));

        MonthlyReview may = guestImport.execute(csv()).months().get(0);

        assertAmount("2450.00", may.income());
        assertAmount("750.00", may.expense());
        assertAmount("1700.00", may.savings());
        assertEquals(1, may.excludedCount());
        assertEquals(2, may.countedTransactions());
        assertTrue(may.categories().stream().noneMatch(c -> c.name().equals("TRANSFERS")),
                "EXCLUDED categories must never appear in the breakdown");
    }

    @Test
    void groupsTransactionsByMonthMostRecentFirst() throws Exception {
        FinancialRecord may = record("2026-05-31", "Salário", new BigDecimal("2450.00"));
        FinancialRecord april = record("2026-04-30", "Salário", new BigDecimal("2450.00"));

        givenDetectedBank("CGD", may, april);
        givenClassifications(classified("SALARY", may), classified("SALARY", april));

        List<MonthlyReview> months = guestImport.execute(csv()).months();

        assertEquals(2, months.size());
        assertEquals("2026-05", months.get(0).period());
        assertEquals("2026-04", months.get(1).period());
    }

    @Test
    void topCategoriesAreSortedBySpendDescendingWithShareOfExpense() throws Exception {
        FinancialRecord rent = record("2026-05-02", "Renda", new BigDecimal("-750.00"));
        FinancialRecord groceries = record("2026-05-18", "Continente", new BigDecimal("-63.15"));

        givenDetectedBank("CGD", rent, groceries);
        givenClassifications(classified("RENT", rent), classified("GROCERIES", groceries));

        MonthlyReview may = guestImport.execute(csv()).months().get(0);

        assertEquals(2, may.categories().size());
        CategorySlice top = may.categories().get(0);
        CategorySlice second = may.categories().get(1);
        assertEquals("HOUSING", top.name());
        assertEquals("FOOD & DINING", second.name());
        assertTrue(top.share() > second.share());
        // Shares are computed against the month's total expense (813.15).
        assertEquals(750.00 / 813.15, top.share(), 0.001);
    }

    @Test
    void throwsWhenNoRegisteredParserCanReadTheFile() throws Exception {
        Mockito.when(bankRegistry.listAvailable()).thenReturn(java.util.Set.of("CGD"));
        Mockito.when(bankRegistry.getParser("CGD")).thenReturn(parser);
        Mockito.when(parser.parse(any(InputStream.class))).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.moneymind.finance.domain.guest.UnsupportedBankException.class,
                () -> guestImport.execute(csv()));
    }

    // ---- helpers -------------------------------------------------------

    private void givenDetectedBank(String bank, FinancialRecord... records) {
        Mockito.when(bankRegistry.listAvailable()).thenReturn(java.util.Set.of(bank));
        Mockito.when(bankRegistry.getParser(bank)).thenReturn(parser);
        try {
            Mockito.when(parser.parse(any(InputStream.class))).thenReturn(List.of(records));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void givenClassifications(ClassifiedFinancialRecord... classified) {
        try {
            Mockito.when(classifier.classify(Mockito.anyList())).thenReturn(List.of(classified));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassifiedFinancialRecord classified(String subcategory, FinancialRecord record) {
        return new ClassifiedFinancialRecord(subcategory, new BigDecimal("0.9"), record);
    }

    private static FinancialRecord record(String date, String description, BigDecimal amount) {
        return new FinancialRecord(
                UUID.randomUUID().toString(),
                "CGD",
                OffsetDateTime.parse(date + "T00:00:00Z"),
                description,
                amount,
                BigDecimal.ZERO,
                "UNCATEGORIZED");
    }

    private static InputStream csv() {
        return new ByteArrayInputStream("any".getBytes(StandardCharsets.UTF_8));
    }

    private static List<Category> systemCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(category("INCOME", CategoryType.INCOME, sub("SALARY", CategoryType.INCOME)));
        categories.add(category("HOUSING", CategoryType.EXPENSE, sub("RENT", CategoryType.EXPENSE)));
        categories.add(category("FOOD & DINING", CategoryType.EXPENSE, sub("GROCERIES", CategoryType.EXPENSE)));
        categories.add(category("TRANSFERS", CategoryType.EXCLUDED,
                sub("TRANSFER BETWEEN ACCOUNTS", CategoryType.EXCLUDED)));
        return categories;
    }

    private static Category category(String name, CategoryType type, Category... subs) {
        return new Category(UUID.randomUUID(), name, type, List.of(subs));
    }

    private static Category sub(String name, CategoryType type) {
        return new Category(UUID.randomUUID(), name, type, List.of());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but got " + actual);
    }
}