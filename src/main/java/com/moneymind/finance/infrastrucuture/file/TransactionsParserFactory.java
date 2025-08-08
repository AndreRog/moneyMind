package com.moneymind.finance.parsers;

import com.moneymind.finance.ports.TransactionsParser;
import com.moneymind.finance.di.BankType;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransactionsParserFactory {

    // Map[BankTypeEnum?, TransactionsParser]
    private final Map<String, TransactionsParser> transactionsParser = new HashMap<>();

    /**
     * Simply injecting the parser in the CDI is enough to be discovered. Makes it easier to extend the application.
     *
     * @param availableParsers the parsers present in the cdi .
     */
    public TransactionsParserFactory(@Any Instance<TransactionsParser> availableParsers) {
        for (TransactionsParser parser : availableParsers) {
            BankType bankTypeAnnotation = parser.getClass().getSuperclass().getAnnotation(BankType.class);

            if (bankTypeAnnotation == null ) {
                throw new IllegalStateException("TransactionsParser " + parser.getClass().getName() + " is missing @BankType annotation");
            }

            if (bankTypeAnnotation.value().isBlank()) {
                throw new IllegalStateException("TransactionsParser " + parser.getClass().getName() + " @BankType annotation is blank");
            }

            transactionsParser.put(bankTypeAnnotation.value(), parser);
        }
    }

    public TransactionsParser getParser(String bankType) {
        return transactionsParser.get(bankType);
    }

    public Set<String> listKeys() {
        return transactionsParser.keySet();
    }
}
