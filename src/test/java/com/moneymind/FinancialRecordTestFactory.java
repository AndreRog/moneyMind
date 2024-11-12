package com.moneymind;

import com.moneymind.finance.domain.core.FinancialRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class FinancialRecordTestFactory {

    public static FinancialRecord createMockFinancialRecord() {
        return new FinancialRecord(
                UUID.randomUUID().toString(),
                "RANDOM",
                OffsetDateTime.now(),
                "Random text",
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                "RANDOM CAT"
        );
    }

    public static FinancialRecord createMockFinancialRecord(String id) {
        return new FinancialRecord(
                id,
                "RANDOM",
                OffsetDateTime.now(),
                "Random text",
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(10.0),
                "RANDOM CAT"
        );
    }


}
