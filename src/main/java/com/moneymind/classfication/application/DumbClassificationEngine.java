package com.moneymind.classfication.application;

import com.moneymind.finance.domain.core.Category;

import java.util.HashMap;
import java.util.Map;

public class DumbClassificationEngine {
    Map<Category, PartialTransaction> categoryTransactionMap;

    public DumbClassificationEngine() {
        this.categoryTransactionMap = new HashMap<>();
    }

    public String categorize(final PartialTransaction partialTransaction) {

        return null;
    }

//    private void build


}
