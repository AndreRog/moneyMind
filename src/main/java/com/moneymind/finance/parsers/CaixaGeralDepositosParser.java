package com.moneymind.finance.parsers;

import com.moneymind.finance.di.BankType;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.ports.TransactionsParser;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@BankType("CAIXAGERALDEPOSITOS")
public class CaixaGeralDepositosParser implements TransactionsParser {

    private static final Logger logger = LoggerFactory.getLogger(CaixaGeralDepositosParser.class);

    @Override
    public List<FinancialRecord> parse(InputStream input) throws IOException {
        final List<FinancialRecord> financialRecords = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(7).withCSVParser(parser).build()) {
            // create csvReader object and skip first Line
            BankType bankTypeAnnotation = this.getClass().getAnnotation(BankType.class);
            List<String[]> allData = csvReader.readAll();

            // skip last line
            for (int i = 0; i < allData.size() - 1; i++) {
                String [] row = allData.get(i);
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                OffsetDateTime transactionDate = LocalDate.parse(
                        row[1],
                        formatter
                ).atStartOfDay().atOffset(ZoneOffset.UTC);

                transactionDate = transactionDate.withOffsetSameInstant(ZoneOffset.UTC);
                double valueSpent;
                if (row[3].isBlank() || row[3].isEmpty()) {
                    valueSpent = Double.parseDouble(row[4].replace(".", "").replace(",", "."));
                } else {
                    valueSpent = -(Double.parseDouble(row[3].replace(".", "").replace(",", ".")));
                }
                String description = row[2];
                double finalBalance = Double.parseDouble(row[5].replace(".", "").replace(",", "."));

                financialRecords.add(new FinancialRecord(
                        bankTypeAnnotation.value(),
                        transactionDate,
                        description,
                        BigDecimal.valueOf(valueSpent),
                        BigDecimal.valueOf(finalBalance)
                ));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return financialRecords;
    }
}
