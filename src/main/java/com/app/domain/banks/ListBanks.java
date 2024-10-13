package com.app.domain.banks;

import com.app.parsers.TransactionsParserFactory;

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
