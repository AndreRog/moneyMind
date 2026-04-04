package com.moneymind.finance.domain.ports;

import java.util.Set;

public interface BankRegistry {

    Set<String> listAvailable();

    TransactionsParser getParser(String bankType);
}
