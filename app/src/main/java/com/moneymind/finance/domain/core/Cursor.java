package com.moneymind.finance.domain.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Cursor {

    private Cursor() {}

    public static String forId(int id) {
        return encode(String.valueOf(id));
    }

    public static String forPeriod(String period) {
        return encode(period);
    }

    public static String forPeriodAndColumn(String period, String column) {
        return encode(period + "," + column);
    }

    /** Decodes a cursor to an integer offset. Returns 0 if {@code cursor} is null. */
    public static int decodeId(String cursor) {
        if (cursor == null) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(decodeRaw(cursor)), 0);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed cursor: " + cursor, ex);
        }
    }

    /** Decodes a cursor to its raw string value. Returns null if {@code cursor} is null. */
    public static String decodeString(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return decodeRaw(cursor);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed cursor: " + cursor, ex);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeRaw(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
