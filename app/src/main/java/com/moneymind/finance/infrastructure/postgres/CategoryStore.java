package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.core.CategoryType;
import com.moneymind.finance.domain.ports.CategoryRepository;
import org.jooq.DSLContext;
import org.jooq.generated.tables.records.CategoryRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jooq.generated.tables.Category.CATEGORY;

public class CategoryStore implements CategoryRepository {

    private final DSLContext dsl;

    public CategoryStore(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<Category> findCategories(final UUID userId) {
        var rows = dsl
                .selectFrom(CATEGORY)
                .where(CATEGORY.USER_ID.isNull()
                        .or(userId != null ? CATEGORY.USER_ID.eq(userId) : org.jooq.impl.DSL.falseCondition()))
                .orderBy(CATEGORY.PARENT_ID.asc().nullsFirst(), CATEGORY.ID.asc())
                .fetchInto(CategoryRecord.class);

        Map<Integer, UUID> uuidById = new LinkedHashMap<>();
        Map<Integer, String> nameById = new LinkedHashMap<>();
        Map<Integer, CategoryType> typeById = new LinkedHashMap<>();
        Map<Integer, List<Category>> childrenByParent = new LinkedHashMap<>();
        List<Integer> topLevelIds = new ArrayList<>();

        for (CategoryRecord row : rows) {
            uuidById.put(row.getId(), row.getUuid());
            nameById.put(row.getId(), row.getName());
            typeById.put(row.getId(), CategoryType.valueOf(row.getType()));

            if (row.getParentId() == null) {
                topLevelIds.add(row.getId());
            } else {
                childrenByParent.computeIfAbsent(row.getParentId(), k -> new ArrayList<>())
                        .add(new Category(row.getUuid(), row.getName(), CategoryType.valueOf(row.getType()), List.of()));
            }
        }

        List<Category> result = new ArrayList<>();
        for (Integer id : topLevelIds) {
            List<Category> subs = childrenByParent.getOrDefault(id, List.of());
            result.add(new Category(uuidById.get(id), nameById.get(id), typeById.get(id), subs));
        }
        return result;
    }
}