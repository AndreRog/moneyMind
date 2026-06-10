package com.moneymind.finance.di;

import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BankType {
    String value();
    /** ISO 3166-1 alpha-2 country code (e.g. "PT"). Selects the Rule Pack for generic bank lines. */
    String country();
}