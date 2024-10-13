package com.app.ports;


import com.app.domain.core.FinancialRecord;

import java.util.List;

/**
 * This eventually can evolve to an automatic system, using ElasticSearch or ML Classification engine
 */
public interface TransactionClassifier {

    FinancialRecord classify(FinancialRecord financialRecord);

    FinancialRecord classify(List<FinancialRecord> financialRecords);

}
