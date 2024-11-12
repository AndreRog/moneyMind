package com.moneymind.finance.domain.banks;

import com.moneymind.finance.parsers.TransactionsParserFactory;

import java.util.Set;

public class ListBanks {

    private final TransactionsParserFactory transactionsParserFactory;

    public ListBanks(TransactionsParserFactory transactionsParserFactory) {
        this.transactionsParserFactory = transactionsParserFactory;
    }

    public Set<String> execute(){
        return transactionsParserFactory.listKeys();
    }
}
