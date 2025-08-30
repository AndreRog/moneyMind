package com.moneymind.finance.infrastrucuture.postgres;

import java.util.Base64;

public class Store {

    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_DEFAULT_LIMIT = 100;

    public int sanitizeLimit(int limit){
        return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_DEFAULT_LIMIT);
    }

    public int sanitizeCursor(String inputCursor) {
        int cursor = 0;

        if(inputCursor != null) {
            try {
                cursor = Integer.parseInt(new String(Base64.getDecoder().decode(inputCursor)));
            } catch (Exception ex){
                throw new StoreException(
                        ExceptionCode.INVALID_PARAM,
                        ex);
            }
        }

        return Math.max(cursor, 0);
    }

}
