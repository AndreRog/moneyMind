package com.moneymind.finance.domain.core;

public enum AggregationPeriod {
    YEAR("year", "YYYY"),
    MONTH("month", "YYYY-MM");

    private final String sqlTruncation;
    private final String dateFormat;

    AggregationPeriod(String sqlTruncation, String dateFormat) {
        this.sqlTruncation = sqlTruncation;
        this.dateFormat = dateFormat;
    }

    public String getSqlTruncation() {
        return sqlTruncation;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public static AggregationPeriod fromString(String period) {
        if (period == null || period.isEmpty()) return null;
        return period.equalsIgnoreCase("year") ? YEAR : MONTH;
    }
}
