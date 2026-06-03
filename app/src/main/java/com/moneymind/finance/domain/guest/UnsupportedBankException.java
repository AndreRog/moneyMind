package com.moneymind.finance.domain.guest;

/**
 * Raised when no registered parser can read the uploaded file — the bank format
 * is not supported yet. Carries no parsed data; nothing was persisted.
 */
public class UnsupportedBankException extends RuntimeException {
    public UnsupportedBankException() {
        super("Unsupported bank: no registered parser could read the file");
    }
}
