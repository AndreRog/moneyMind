package com.moneymind.finance.domain.categories;

import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.ports.CategoryRepository;

import java.util.List;
import java.util.UUID;

public class ListCategories {

    private final CategoryRepository categoryRepository;

    public ListCategories(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> execute(final UUID userId) {
        return categoryRepository.findCategories(userId);
    }
}