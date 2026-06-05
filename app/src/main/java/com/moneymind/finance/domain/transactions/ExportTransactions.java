package com.moneymind.finance.domain.transactions;

import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class ExportTransactions {

    private final TransactionRepository transactionRepository;

    public ExportTransactions(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String execute() {
        TransactionSearchQuery query = TransactionSearchQuery.builder()
                .limit(10000)
                .build();

        List<FinancialRecord> records = transactionRepository.search(query).list();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[]{"ID", "Bank", "Date", "Description", "Amount", "Balance", "Category"});
            for (FinancialRecord record : records) {
                csvWriter.writeNext(new String[]{
                        record.getId(),
                        record.getBankName(),
                        record.getDate() != null ? record.getDate().toString() : "",
                        record.getDescription(),
                        record.getAmount() != null ? record.getAmount().toString() : "",
                        record.getFinalBalance() != null ? record.getFinalBalance().toString() : "",
                        record.getCategory()
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV", e);
        }

        return stringWriter.toString();
    }
}
