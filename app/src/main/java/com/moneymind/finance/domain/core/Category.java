package com.moneymind.finance.domain.core;

import java.util.List;
import java.util.UUID;

public record Category(UUID id, String name, CategoryType type, List<Category> subcategories) {
    public static final String UNCATEGORIZED = "UNCATEGORIZED";
}