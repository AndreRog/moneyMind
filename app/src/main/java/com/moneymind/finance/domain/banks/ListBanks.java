package com.moneymind.finance.domain.banks;

import com.moneymind.finance.domain.ports.BankRegistry;

import java.util.Set;

public class ListBanks {

    private final BankRegistry bankRegistry;

    public ListBanks(BankRegistry bankRegistry) {
        this.bankRegistry = bankRegistry;
    }

    public Set<String> execute() {
        return bankRegistry.listAvailable();
    }
}
