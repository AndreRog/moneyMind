package com.moneymind.domain.transactions;

import com.moneymind.FinancialRecordTestFactory;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.transactions.GetTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

class GetTransactionTest {

    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);

    GetTransaction getTransaction;

    @BeforeEach
    void beforeEach() {
        this.getTransaction = new GetTransaction(transactionRepository);
    }

    @Test
    void validUuidReturnsRecord() {
        UUID id = UUID.randomUUID();
        FinancialRecord expected = FinancialRecordTestFactory.createMockFinancialRecord(id.toString());
        Mockito.when(transactionRepository.getById(id)).thenReturn(expected);

        FinancialRecord result = getTransaction.execute(id.toString());

        Assertions.assertSame(expected, result);
    }

    @Test
    void unknownUuidReturnsNull() {
        UUID id = UUID.randomUUID();
        Mockito.when(transactionRepository.getById(id)).thenReturn(null);

        FinancialRecord result = getTransaction.execute(id.toString());

        Assertions.assertNull(result);
    }

    @Test
    void malformedUuidReturnsNull() {
        FinancialRecord result = getTransaction.execute("not-a-uuid");

        Assertions.assertNull(result);
        Mockito.verify(transactionRepository, Mockito.never()).getById(Mockito.any());
    }
}
