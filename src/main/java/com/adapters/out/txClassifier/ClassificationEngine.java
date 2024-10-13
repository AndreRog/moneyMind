package com.adapters.out.txClassifier;


import com.app.domain.core.FinancialRecord;
import com.app.ports.TransactionClassifier;

import java.util.List;

public class ClassificationEngine implements TransactionClassifier {
    @Override
    public FinancialRecord classify(FinancialRecord financialRecord) {
        return null;
    }

    @Override
    public FinancialRecord classify(List<FinancialRecord> financialRecords) {
        return null;
    }
}
