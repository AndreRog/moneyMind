package com.moneymind.finance.domain.core;

import java.util.List;

public record TransactionSearchQuery(
        String id,
        String category,
        String aggregateByPeriod,
        String aggregateByColumn,
        List<String> excludeCategories,
        String bank,
        String from,
        String to,
        int limit,
        String cursor,
        String sort
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String category;
        private String aggregateByPeriod;
        private String aggregateByColumn;
        List<String> excludeCategories;
        private String bank;
        private String from;
        private String to;
        private int limit = 100;
        private String cursor;
        private String sort;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder aggregateByPeriod(String aggregateByPeriod) {
            this.aggregateByPeriod = aggregateByPeriod;
            return this;
        }

        public Builder aggregateByColumn(String aggregateByColumn) {
            this.aggregateByColumn = aggregateByColumn;
            return this;
        }

        public Builder excludeCategories(List<String > excludeCategories) {
            this.excludeCategories = excludeCategories;
            return this;
        }

        public Builder bank(String bank) {
            this.bank = bank;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder cursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public TransactionSearchQuery build() {
            return new TransactionSearchQuery(
                    id,
                    category,
                    aggregateByPeriod,
                    aggregateByColumn,
                    excludeCategories,
                    bank,
                    from,
                    to,
                    limit,
                    cursor,
                    sort
            );
        }
    }

}
