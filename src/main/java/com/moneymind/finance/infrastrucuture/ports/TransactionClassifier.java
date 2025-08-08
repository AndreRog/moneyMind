package com.moneymind.finance.ports;

import com.moneymind.finance.domain.core.FinancialRecord;

import java.util.List;

/**
 * This eventually can evolve to an automatic system, using ElasticSearch or ML Classification engine
 */
public interface TransactionClassifier {

    List<FinancialRecord> classify();

    FinancialRecord classify(FinancialRecord financialRecord);

    List<FinancialRecord> classify(List<FinancialRecord> financialRecords) throws Exception;
}
