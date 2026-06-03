package com.moneymind.finance.domain.ports;

import com.moneymind.finance.domain.core.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository {
    List<Category> findCategories(UUID userId);
}