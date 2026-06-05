package com.moneymind.domain.transactions;

import com.moneymind.FinancialRecordTestFactory;
import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.transactions.ExportTransactions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class ExportTransactionsTest {

    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);

    ExportTransactions exportTransactions;

    @BeforeEach
    void beforeEach() {
        this.exportTransactions = new ExportTransactions(transactionRepository);
    }

    @Test
    void emptyRepositoryProducesHeaderOnly() {
        Mockito.when(transactionRepository.search(Mockito.any()))
                .thenReturn(new PagedResult<>(List.of(), 10000, null));

        String csv = exportTransactions.execute();

        String[] lines = csv.trim().split("\n");
        Assertions.assertEquals(1, lines.length);
        Assertions.assertTrue(lines[0].contains("ID"));
        Assertions.assertTrue(lines[0].contains("Bank"));
        Assertions.assertTrue(lines[0].contains("Category"));
    }

    @Test
    void singleRecordProducesHeaderAndDataLine() {
        String id = UUID.randomUUID().toString();
        FinancialRecord record = FinancialRecordTestFactory.createMockFinancialRecord(id);
        Mockito.when(transactionRepository.search(Mockito.any()))
                .thenReturn(new PagedResult<>(List.of(record), 10000, null));

        String csv = exportTransactions.execute();

        String[] lines = csv.trim().split("\n");
        Assertions.assertEquals(2, lines.length);
        Assertions.assertTrue(lines[1].contains(id));
        Assertions.assertTrue(lines[1].contains("RANDOM"));
    }

    @Test
    void nullFieldsAreWrittenAsEmptyString() {
        FinancialRecord record = new FinancialRecord(
                UUID.randomUUID().toString(),
                "BANK",
                null,
                "desc",
                null,
                null,
                "CAT"
        );
        Mockito.when(transactionRepository.search(Mockito.any()))
                .thenReturn(new PagedResult<>(List.of(record), 10000, null));

        String csv = exportTransactions.execute();

        // should not throw; null date/amount/balance become empty strings
        Assertions.assertNotNull(csv);
        Assertions.assertTrue(csv.contains("BANK"));
    }

    @Test
    void fetchesWithLimitOf10000() {
        Mockito.when(transactionRepository.search(Mockito.any()))
                .thenReturn(new PagedResult<>(List.of(), 10000, null));

        exportTransactions.execute();

        ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
        Mockito.verify(transactionRepository).search(captor.capture());
        Assertions.assertEquals(10000, captor.getValue().limit());
    }
}
