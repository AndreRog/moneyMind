package com.moneymind.finance.domain.guest;

import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.core.CategoryType;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.BankRegistry;
import com.moneymind.finance.domain.ports.CategoryRepository;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionsParser;
import com.moneymind.finance.domain.summary.CategoryBreakdown;
import com.moneymind.finance.domain.summary.Summary;
import com.moneymind.finance.domain.summary.SummaryEngine;
import com.moneymind.finance.domain.summary.SummaryTransaction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless guest import use case: parses an uploaded bank file in memory, classifies
 * each transaction into a System Subcategory, then runs the {@link SummaryEngine} to
 * produce a monthly review. Nothing is persisted — there is deliberately no
 * {@code TransactionRepository} dependency here.
 */
public class GuestImport {

    private final BankRegistry bankRegistry;
    private final TransactionClassifier classifier;
    private final CategoryRepository categoryRepository;
    private final SummaryEngine summaryEngine;

    public GuestImport(final BankRegistry bankRegistry,
                       final TransactionClassifier classifier,
                       final CategoryRepository categoryRepository,
                       final SummaryEngine summaryEngine) {
        this.bankRegistry = bankRegistry;
        this.classifier = classifier;
        this.categoryRepository = categoryRepository;
        this.summaryEngine = summaryEngine;
    }

    public GuestReview execute(final InputStream file) throws Exception {
        final byte[] bytes = file.readAllBytes();

        final Detected detected = detectAndParse(bytes);
        final List<ClassifiedFinancialRecord> classified = classifier.classify(detected.records());
        final Map<String, Resolved> typeBySubcategory = resolveCategoryTypes();

        final List<MonthlyReview> months = buildMonthlyReviews(classified, typeBySubcategory);
        return new GuestReview(detected.bank(), months);
    }

    private Detected detectAndParse(final byte[] bytes) {
        for (String bank : bankRegistry.listAvailable()) {
            final TransactionsParser parser = bankRegistry.getParser(bank);
            if (parser == null) {
                continue;
            }
            try {
                final List<FinancialRecord> parsed = parser.parse(new ByteArrayInputStream(bytes));
                if (parsed != null && !parsed.isEmpty()) {
                    return new Detected(bank, parsed);
                }
            } catch (Exception ignored) {
                // This parser could not read the file; try the next bank.
            }
        }
        throw new UnsupportedBankException();
    }

    private List<MonthlyReview> buildMonthlyReviews(final List<ClassifiedFinancialRecord> classified,
                                                    final Map<String, Resolved> typeBySubcategory) {
        final Map<String, List<ClassifiedFinancialRecord>> byPeriod = new LinkedHashMap<>();
        for (ClassifiedFinancialRecord record : classified) {
            byPeriod.computeIfAbsent(period(record), k -> new ArrayList<>()).add(record);
        }

        return byPeriod.entrySet().stream()
                .sorted(Map.Entry.<String, List<ClassifiedFinancialRecord>>comparingByKey().reversed())
                .map(e -> monthlyReview(e.getKey(), e.getValue(), typeBySubcategory))
                .toList();
    }

    private MonthlyReview monthlyReview(final String period,
                                        final List<ClassifiedFinancialRecord> records,
                                        final Map<String, Resolved> typeBySubcategory) {
        final List<SummaryTransaction> transactions = records.stream()
                .map(r -> toSummaryTransaction(r, typeBySubcategory))
                .toList();

        final Summary summary = summaryEngine.compute(transactions);

        final List<CategorySlice> categories = summary.categories().stream()
                .filter(c -> c.type() == CategoryType.EXPENSE)
                .sorted(Comparator.comparing(CategoryBreakdown::total).reversed())
                .map(c -> new CategorySlice(c.categoryName(), c.total(), share(c.total(), summary.expense())))
                .toList();

        final int excludedCount = (int) transactions.stream()
                .filter(t -> t.categoryType() == CategoryType.EXCLUDED)
                .count();

        return new MonthlyReview(
                period,
                summary.income(),
                summary.expense(),
                summary.savings(),
                categories,
                transactions.size() - excludedCount,
                excludedCount);
    }

    private SummaryTransaction toSummaryTransaction(final ClassifiedFinancialRecord record,
                                                    final Map<String, Resolved> typeBySubcategory) {
        final String subcategory = record.category();
        final Resolved resolved = typeBySubcategory.getOrDefault(
                subcategory, new Resolved(subcategory, CategoryType.EXPENSE));
        return new SummaryTransaction(
                record.record().getAmount(),
                resolved.categoryName(),
                subcategory,
                resolved.type());
    }

    private Map<String, Resolved> resolveCategoryTypes() {
        final Map<String, Resolved> bySubcategory = new LinkedHashMap<>();
        // null userId → system/global categories (not user-scoped)
        for (Category category : categoryRepository.findCategories(null)) {
            for (Category sub : category.subcategories()) {
                bySubcategory.put(sub.name(), new Resolved(category.name(), sub.type()));
            }
        }
        return bySubcategory;
    }

    private static String period(final ClassifiedFinancialRecord record) {
        final OffsetDateTime date = record.record().getDate();
        return YearMonth.from(date).toString();
    }

    private static double share(final BigDecimal categoryTotal, final BigDecimal expense) {
        if (expense == null || expense.signum() == 0) {
            return 0d;
        }
        return categoryTotal.divide(expense, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private record Detected(String bank, List<FinancialRecord> records) {}

    private record Resolved(String categoryName, CategoryType type) {}
}
