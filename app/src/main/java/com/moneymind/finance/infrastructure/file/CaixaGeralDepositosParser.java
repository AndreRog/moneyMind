package com.moneymind.finance.infrastructure.file;

import com.moneymind.finance.di.BankType;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.ports.TransactionsParser;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@BankType(value = "CAIXAGERALDEPOSITOS", country = "PT")
public class CaixaGeralDepositosParser implements TransactionsParser {

    private static final Logger logger = LoggerFactory.getLogger(CaixaGeralDepositosParser.class);

    // Maps unambiguous CGD Categoria labels to system categories.
    // Broad labels (COMPRAS, LEVANTAMENTOS, Diversos) are intentionally absent so rules/ML
    // classify those transactions by merchant description instead.
    private static final Map<String, String> CGD_CATEGORY_MAP = Map.of(
        "AGUA",     "WATER",
        "SEGUROS",  "HOUSING",
        "DEPOSITO", "INCOME"
    );

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
                String bankCategory = row.length > 7 ? row[7].strip() : "";
                String category = CGD_CATEGORY_MAP.getOrDefault(bankCategory.toUpperCase(), "UNCATEGORIZED");

                financialRecords.add(new FinancialRecord(
                        null,
                        bankTypeAnnotation.value(),
                        transactionDate,
                        description,
                        BigDecimal.valueOf(valueSpent),
                        BigDecimal.valueOf(finalBalance),
                        category
                ));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return financialRecords;
    }
}
