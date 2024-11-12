package com.moneymind.classfication;

import com.moneymind.classfication.adapters.out.postgres.TransactionsStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import org.jooq.DSLContext;

public class DI {


    /**
     * For now the injection of this is on the money mind layer we should put here.
     */
    @ApplicationScoped
    @Produces
    TransactionsStore producesTxStore(final DSLContext dataSource) {
        return new TransactionsStore(dataSource);
    }

}
