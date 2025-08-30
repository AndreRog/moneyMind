package com.moneymind.domain.transactions;

import com.moneymind.FinancialRecordTestFactory;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

class UpdateTransactionsTest {


    TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);

    UpdateTransactions updateTransactions;

    @BeforeEach
    void beforeEach(){
        this.updateTransactions = new UpdateTransactions(transactionRepository);
    }

    @Test
    public void shouldReturnNullWhenTransactionDoesNotExist(){
        //prepare
        UUID nonExistentTransactionId = UUID.randomUUID();
        Mockito.when(transactionRepository.getById(nonExistentTransactionId)).thenReturn(null);

        //act
        final FinancialRecord record = updateTransactions.execute(nonExistentTransactionId.toString(), "RANDOM");

        //assert
        Assertions.assertNull(record);
    }

    @Test
    public void shouldReturnTransaction(){
        //prepare
        UUID nonExistentTransactionId = UUID.randomUUID();
        Mockito.when(transactionRepository.getById(nonExistentTransactionId))
                .thenReturn(FinancialRecordTestFactory.createMockFinancialRecord(nonExistentTransactionId.toString()));

        //act
        final FinancialRecord record = updateTransactions.execute(nonExistentTransactionId.toString(), "RANDOM");

        //assert
        Assertions.assertNotNull(record);
        Assertions.assertEquals(nonExistentTransactionId, UUID.fromString(record.getId()));
    }
  
}
