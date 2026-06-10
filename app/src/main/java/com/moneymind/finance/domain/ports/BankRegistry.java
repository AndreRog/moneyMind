package com.moneymind.finance.domain.ports;

import java.util.Set;

public interface BankRegistry {

    Set<String> listAvailable();

    TransactionsParser getParser(String bankType);

    /** Returns the ISO 3166-1 alpha-2 country code declared by the bank's parser. */
    String countryOf(String bankType);
}
