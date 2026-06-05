package com.moneymind.persistence;

import org.jooq.DSLContext;
import org.jooq.generated.tables.BankTransaction;
import org.jooq.generated.tables.records.BankTransactionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Tag("integration")
abstract class PersistenceHarness {

    protected static final DSLContext dsl = PostgresTestContainer.DSL_CTX;

    @BeforeEach
    void truncate() {
        dsl.execute("TRUNCATE TABLE bank_transaction RESTART IDENTITY");
    }

    protected BankTransactionRecord seedTransaction(
            BigDecimal value,
            BigDecimal balance,
            String description,
            String bankName,
            OffsetDateTime date,
            String category
    ) {
        return dsl.insertInto(BankTransaction.BANK_TRANSACTION,
                        BankTransaction.BANK_TRANSACTION.VALUE,
                        BankTransaction.BANK_TRANSACTION.BALANCE,
                        BankTransaction.BANK_TRANSACTION.DESCRIPTION,
                        BankTransaction.BANK_TRANSACTION.BANK_NAME,
                        BankTransaction.BANK_TRANSACTION.DATE,
                        BankTransaction.BANK_TRANSACTION.CATEGORY)
                .values(value, balance, description, bankName, date, category)
                .returning()
                .fetchOne();
    }
}
