package com.app.ports;

import com.app.domain.core.FinancialRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface TransactionsParser {

    List<FinancialRecord> parse(InputStream inputStream) throws IOException;
}
