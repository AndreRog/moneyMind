package com.moneymind.finance.infrastructure.postgres;

import com.moneymind.finance.domain.core.Cursor;

public class Store {

    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_DEFAULT_LIMIT = 100;

    public int sanitizeLimit(int limit){
        return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_DEFAULT_LIMIT);
    }

    public int sanitizeCursor(String inputCursor) {
        try {
            return Cursor.decodeId(inputCursor);
        } catch (Exception ex) {
            throw new StoreException(ExceptionCode.INVALID_PARAM, ex);
        }
    }

}
