package com.moneymind.finance.domain.guest;

import java.math.BigDecimal;

/**
 * One expense Category's contribution to a month's review: its total spend and
 * its share (0–1) of the month's total expense. Used to render the breakdown bars.
 */
public record CategorySlice(String name, BigDecimal amount, double share) {}