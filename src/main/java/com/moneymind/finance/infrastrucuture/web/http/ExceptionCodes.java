package com.moneymind.finance.infrastrucuture.web.http;

public enum ExceptionCodes {

    INTERNAL_SERVER_ERROR("Internal Server Error");
    private final String title;

    ExceptionCodes(final String title) {
        this.title = title;
    }

    public String getTitle(){
        return this.title;
    }

}
