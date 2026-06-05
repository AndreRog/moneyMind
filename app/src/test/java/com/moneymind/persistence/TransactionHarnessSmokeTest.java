package com.moneymind.persistence;

import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionHarnessSmokeTest extends PersistenceHarness {

    @Test
    void insertedRowCanBeReadBack() {
        BigDecimal value = new BigDecimal("123.45");
        BigDecimal balance = new BigDecimal("1000.00");
        String description = "Smoke test transaction";
        String bankName = "TEST_BANK";
        OffsetDateTime date = OffsetDateTime.parse("2026-01-15T12:00:00Z");
        String category = "TEST_CATEGORY";

        BankTransactionRecord inserted = seedTransaction(value, balance, description, bankName, date, category);

        assertNotNull(inserted);
        assertNotNull(inserted.getUuid(), "uuid should be auto-generated");

        BankTransactionRecord fetched = dsl.selectFrom(BankTransaction.BANK_TRANSACTION)
                .where(BankTransaction.BANK_TRANSACTION.UUID.eq(inserted.getUuid()))
                .fetchOne();

        assertNotNull(fetched);
        assertEquals(0, value.compareTo(fetched.getValue()), "value");
        assertEquals(0, balance.compareTo(fetched.getBalance()), "balance");
        assertEquals(description, fetched.getDescription(), "description");
        assertEquals(bankName, fetched.getBankName(), "bankName");
        assertEquals(category, fetched.getCategory(), "category");
        assertNotNull(fetched.getCreatedAt(), "createdAt set by trigger");
        assertNotNull(fetched.getUpdatedAt(), "updatedAt set by trigger");
    }

    @Test
    void truncateIsCalledBeforeEachTest() {
        seedTransaction(BigDecimal.ONE, BigDecimal.TEN, "Previous test row", "BANK", OffsetDateTime.now(), null);

        int count = dsl.fetchCount(BankTransaction.BANK_TRANSACTION);
        assertEquals(1, count, "table should contain only the row seeded in this test — prior rows truncated");
    }
}
