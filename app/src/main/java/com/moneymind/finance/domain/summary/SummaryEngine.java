package com.moneymind.finance.domain.summary;

import com.moneymind.finance.domain.core.CategoryType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SummaryEngine {

    public Summary compute(List<SummaryTransaction> transactions) {
        List<SummaryTransaction> visible = transactions.stream()
                .filter(t -> t.categoryType() != CategoryType.EXCLUDED)
                .toList();

        BigDecimal income = sumByType(visible, CategoryType.INCOME);
        BigDecimal expense = sumByType(visible, CategoryType.EXPENSE).negate();
        BigDecimal savings = income.subtract(expense);

        return new Summary(income, expense, savings, categories(visible), subcategories(visible));
    }

    public List<CategoryDelta> compare(Summary base, Summary compare) {
        Map<String, BigDecimal> baseMap = index(base.categories());
        Map<String, BigDecimal> compareMap = index(compare.categories());

        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(baseMap.keySet());
        allNames.addAll(compareMap.keySet());

        return allNames.stream()
                .map(name -> {
                    BigDecimal b = baseMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal c = compareMap.getOrDefault(name, BigDecimal.ZERO);
                    return new CategoryDelta(name, b, c, c.subtract(b));
                })
                .toList();
    }

    private BigDecimal sumByType(List<SummaryTransaction> transactions, CategoryType type) {
        return transactions.stream()
                .filter(t -> t.categoryType() == type)
                .map(SummaryTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryBreakdown> categories(List<SummaryTransaction> transactions) {
        Map<String, List<SummaryTransaction>> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(SummaryTransaction::categoryName, LinkedHashMap::new, Collectors.toList()));

        return byCategory.entrySet().stream()
                .map(e -> {
                    CategoryType type = e.getValue().get(0).categoryType();
                    BigDecimal raw = e.getValue().stream()
                            .map(SummaryTransaction::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal total = type == CategoryType.EXPENSE ? raw.negate() : raw;
                    return new CategoryBreakdown(e.getKey(), type, total);
                })
                .toList();
    }

    private List<SubcategoryBreakdown> subcategories(List<SummaryTransaction> transactions) {
        record Key(String subcategoryName, String categoryName, CategoryType type) {}

        Map<Key, BigDecimal> totals = new LinkedHashMap<>();
        for (SummaryTransaction t : transactions) {
            Key key = new Key(t.subcategoryName(), t.categoryName(), t.categoryType());
            BigDecimal contribution = t.categoryType() == CategoryType.EXPENSE
                    ? t.amount().negate()
                    : t.amount();
            totals.merge(key, contribution, BigDecimal::add);
        }

        return totals.entrySet().stream()
                .map(e -> new SubcategoryBreakdown(
                        e.getKey().subcategoryName(),
                        e.getKey().categoryName(),
                        e.getKey().type(),
                        e.getValue()))
                .toList();
    }

    private Map<String, BigDecimal> index(List<CategoryBreakdown> breakdowns) {
        return breakdowns.stream()
                .collect(Collectors.toMap(CategoryBreakdown::categoryName, CategoryBreakdown::total));
    }
}
