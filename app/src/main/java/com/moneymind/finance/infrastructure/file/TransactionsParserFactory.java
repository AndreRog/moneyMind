package com.moneymind.finance.infrastructure.file;

import com.moneymind.finance.di.BankType;
import com.moneymind.finance.domain.ports.BankRegistry;
import com.moneymind.finance.domain.ports.TransactionsParser;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransactionsParserFactory implements BankRegistry {

    private final Map<String, TransactionsParser> transactionsParser = new HashMap<>();
    private final Map<String, String> bankCountry = new HashMap<>();

    public TransactionsParserFactory(@Any Instance<TransactionsParser> availableParsers) {
        for (TransactionsParser parser : availableParsers) {
            BankType bankTypeAnnotation = parser.getClass().getSuperclass().getAnnotation(BankType.class);

            if (bankTypeAnnotation == null) {
                throw new IllegalStateException("TransactionsParser " + parser.getClass().getName() + " is missing @BankType annotation");
            }
            if (bankTypeAnnotation.value().isBlank()) {
                throw new IllegalStateException("TransactionsParser " + parser.getClass().getName() + " @BankType value is blank");
            }
            if (bankTypeAnnotation.country().isBlank()) {
                throw new IllegalStateException("TransactionsParser " + parser.getClass().getName() + " @BankType country is blank — every parser must declare a country");
            }

            transactionsParser.put(bankTypeAnnotation.value(), parser);
            bankCountry.put(bankTypeAnnotation.value(), bankTypeAnnotation.country());
        }
    }

    @Override
    public TransactionsParser getParser(String bankType) {
        return transactionsParser.get(bankType);
    }

    @Override
    public Set<String> listAvailable() {
        return transactionsParser.keySet();
    }

    @Override
    public String countryOf(String bankType) {
        return bankCountry.getOrDefault(bankType, "");
    }
}
