package com.moneymind.finance.domain.ports;

import com.moneymind.finance.domain.core.Category;
import com.moneymind.finance.domain.core.FinancialRecord;

import java.util.List;

public interface Classifier {

    Category classify(FinancialRecord financialRecord);

    List<FinancialRecord> classify(List<FinancialRecord> financialRecords) throws Exception;
}
