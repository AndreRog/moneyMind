package com.moneymind.finance.infrastructure.web.http.dto;

/**
 * Error payload for a guest import that could not be processed — e.g. the
 * uploaded file is from a bank MoneyMind does not parse yet.
 */
public record GuestImportError(String code, String message) {}
