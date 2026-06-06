package com.moneymind.finance.domain.ports;

import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;

import java.util.List;

public interface TransactionClassifier {

    List<ClassifiedFinancialRecord> classify(List<FinancialRecord> financialRecords) throws Exception;
}
