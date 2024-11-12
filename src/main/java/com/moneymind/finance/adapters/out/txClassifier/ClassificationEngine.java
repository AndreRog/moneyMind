package com.moneymind.finance.adapters.out.txClassifier;


import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.ports.TransactionClassifier;

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
