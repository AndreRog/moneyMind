package com.app.parsers;

import com.app.domain.core.FinancialRecord;
import com.app.ports.TransactionsParser;
import com.di.BankType;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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

/**
 * Santander Excel Parser to a standard format for the application to use.
 * <p>
 * The excel columns are as follow:
 * Data Operação,Data valor,Descrição,Montante( EUR ),Saldo Contabilístico( EUR )
 */
@ApplicationScoped
@BankType("SANTANDER")
public class SantanderParser implements TransactionsParser {

    private static final Logger logger = LoggerFactory.getLogger(SantanderParser.class);

    @Override
    public List<FinancialRecord> parse(InputStream input) {
        final List<FinancialRecord> financialRecords = new ArrayList<>();


        try (BufferedReader fileReader = new BufferedReader(new
                InputStreamReader(input, StandardCharsets.UTF_8))) {
            final HSSFWorkbook workbook = new HSSFWorkbook(input);
            final BankType bankTypeAnnotation = this.getClass().getAnnotation(BankType.class);

            final Sheet sheet = workbook.getSheetAt(0);
            int row_counter = 0;
            for (Row row : sheet) {
                row_counter++;

                // skip header rows
                if (row_counter <= 6 || row == null || row.getCell(0) == null) {
                    continue;
                }


                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                OffsetDateTime transactionDate = LocalDate.parse(
                        row.getCell(0).toString(),
                        formatter
                ).atStartOfDay().atOffset(ZoneOffset.UTC);

                transactionDate = transactionDate.withOffsetSameInstant(ZoneOffset.UTC);
                double valueSpent = row.getCell(3).getNumericCellValue();
                String description = row.getCell(2).getStringCellValue();
                double finalBalance = row.getCell(4).getNumericCellValue();

                financialRecords.add(new FinancialRecord(
                        bankTypeAnnotation.value(),
                        transactionDate,
                        description,
                        BigDecimal.valueOf(valueSpent),
                        BigDecimal.valueOf(finalBalance)
                ));

//                final String debugString = String.format("transactionDate: %s\nvalueSpent: %s\ndescription: %s\nfinalBalance: %s\n",
//                        formatter.format(transactionDate), valueSpent,
//                        description, finalBalance);
//                logger.debug(debugString );
//
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return financialRecords;
    }
}
