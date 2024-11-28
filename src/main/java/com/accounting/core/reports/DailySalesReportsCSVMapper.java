package com.accounting.core.reports;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Currency;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.commons.utils.EnumsUtils;
import com.accounting.core.exceptions.FileParsingException;
import com.accounting.core.reports.enums.ProductType;
import com.accounting.core.reports.enums.TransactionType;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static com.accounting.core.reports.DailySalesReportsCSVMapper.Columns.*;
import static java.lang.String.format;
import static java.util.Objects.isNull;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
class DailySalesReportsCSVMapper {

    public static final int HEADER_ROWS_NUMBER = 1;
    public static final String COLUMN_DELIMITER = ";";
    public static final String REPORT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final CurrencyRepoIf currencyRepo;

    static final class Columns {
        private static int startIndex = 0;

        // IMPORTANT: Please note that order is crucial as it must match columns order in CSV file.
        public static final int PRODUCT_TYPE = startIndex++;
        public static final int TRANSACTION_TYPE = startIndex++;
        public static final int EAN = startIndex++;
        public static final int SERIAL_NUMBER = startIndex++;
        public static final int TERMINAL_ID = startIndex++;
        public static final int EXTERNAL_TX_ID = startIndex++;
        public static final int BOOKING_DATE_AND_TIME = startIndex++;
        public static final int GROSS_AMOUNT = startIndex++;
        public static final int NET_AMOUNT = startIndex++;
        public static final int COMMISSION = startIndex++;
        public static final int CARD_TOKEN_ID = startIndex++;
        public static final int AMOUNT = startIndex++;
        public static final int CURRENCY = startIndex++;
        public static final int ACTIVATING_LOCATION_REF = startIndex++;

        public static int count() {
            return startIndex;
        }
    }

    public DailySalesReportRow map(String reportLine, Long rowNumber) throws FileParsingException {
        reportLine = sanitizeQuotes(reportLine);
        String[] csvArray = reportLine.split(COLUMN_DELIMITER);
        csvArray = Arrays.stream(csvArray).map(String::trim).toArray(String[]::new);

        validateLine(csvArray, rowNumber);

        final BigDecimal grossAmount = new BigDecimal(csvArray[GROSS_AMOUNT]);
        final BigDecimal netAmount = new BigDecimal(csvArray[NET_AMOUNT]);
        final BigDecimal commission = new BigDecimal(csvArray[COMMISSION]);
        final TransactionType transactionType = TransactionType.valueOf(csvArray[TRANSACTION_TYPE]);

        validateAmountsAreBalanced(grossAmount, netAmount, commission, transactionType, rowNumber);

        final Currency currency = getCurrency(csvArray, rowNumber);
        final Long grossQuantity = getAmountInMinorUnits(grossAmount, currency);
        final Long netQuantity = getAmountInMinorUnits(netAmount, currency);
        final Long commissionQuantity = getAmountInMinorUnits(commission, currency);

        final LocalDateTime bookingDate = LocalDateTime.parse(
                csvArray[BOOKING_DATE_AND_TIME],
                DateTimeFormatter.ofPattern(REPORT_DATE_TIME_FORMAT)
        );

        return DailySalesReportRow.builder()
                .rowNumber(rowNumber)
                .EAN(csvArray[EAN])
                .externalId(csvArray[EXTERNAL_TX_ID])
                .terminalId(csvArray[TERMINAL_ID])
                .txReference(csvArray[CARD_TOKEN_ID])
                .serialNumber(csvArray[SERIAL_NUMBER])
                .grossAmountQuantity(grossQuantity)
                .netAmountQuantity(netQuantity)
                .currency(currency)
                .commissionAmountQuantity(commissionQuantity)
                .productType(ProductType.valueOf(csvArray[PRODUCT_TYPE]))
                .activatingLocationRef(csvArray[ACTIVATING_LOCATION_REF])
                .bookingDate(bookingDate)
                .transactionType(transactionType)
                .build();
    }

    private String sanitizeQuotes(String line) {
        return line.replaceAll("\"", "");
    }

    private void validateLine(String[] csvArray, Long rowNumber) throws FileParsingException {
        validateLineLength(csvArray, rowNumber);
        validateProductType(csvArray[PRODUCT_TYPE], rowNumber);
        validateTransactionType(csvArray[TRANSACTION_TYPE], rowNumber);
        validateSerialNumber(csvArray[SERIAL_NUMBER], rowNumber);
        validateDateFormat(csvArray[BOOKING_DATE_AND_TIME], rowNumber);
        validateAmountQuantity(csvArray[GROSS_AMOUNT], rowNumber, "GROSS_AMOUNT");
        validateAmountQuantity(csvArray[NET_AMOUNT], rowNumber, "NET_AMOUNT");
        validateAmountQuantity(csvArray[COMMISSION], rowNumber, "COMMISSION");
        validateAmountQuantity(csvArray[AMOUNT], rowNumber, "AMOUNT");
    }

    private void validateAmountsAreBalanced(BigDecimal grossAmount, BigDecimal netAmount,
                                            BigDecimal commissionAmount, TransactionType transactionType,
                                            Long rowNumber) throws FileParsingException {

        if (!grossAmount.subtract(netAmount).equals(commissionAmount)) {
            String msg = format("Amounts for transaction type '%s' are not properly balanced. Gross: %s, Net: %s, Commission: %s", transactionType, grossAmount, netAmount, commissionAmount);
            throwFileParsingException(msg, rowNumber);
        }
    }

    private void validateDateFormat(String dateAndTime, Long rowNumber) throws FileParsingException {
        // e.g. date format 2022-06-01 09:05:07
        if (!dateAndTime.matches("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$")) {
            throwFileParsingException(format("Invalid date time format '%s'.", dateAndTime), rowNumber);
        }
    }

    private void validateLineLength(String[] csvArray, Long rowNumber) throws FileParsingException {
        if (csvArray.length != count()) {
            throwFileParsingException(format("Invalid number of columns. Actual column count=%s, expected=%s", csvArray.length, count()), rowNumber);
        }
    }

    private void validateProductType(String productType, Long rowNumber) throws FileParsingException {
        if (!EnumsUtils.isValidEnumType(ProductType.values(), productType)) {
            throwFileParsingException(format("Invalid product type '%s'.", productType), rowNumber);
        }
    }

    private void validateTransactionType(String transactionType, Long rowNumber) throws FileParsingException {
        if (!EnumsUtils.isValidEnumType(TransactionType.values(), transactionType)) {
            throwFileParsingException(format("Invalid transaction type '%s'.", transactionType), rowNumber);
        }
    }

    private void validateSerialNumber(String serialNumber, Long rowNumber) throws FileParsingException {
        if (!StringUtils.isNumeric(serialNumber)) {
            throwFileParsingException(format("Invalid serialNumber '%s'.", serialNumber), rowNumber);
        }
    }

    private void validateAmountQuantity(String amount, Long rowNumber, String columnName) throws FileParsingException {
        if (!NumberUtils.isParsable(amount)) {
            throwFileParsingException(format("Invalid '%s' column '%s'.", columnName, amount), rowNumber);
        }
    }

    private Long getAmountInMinorUnits(BigDecimal quantity, Currency currency) {
        return Constants.toMinorUnits(Constants.Currencies.valueOf(currency.getCode()), quantity);
    }

    private Currency getCurrency(String[] csvArray, Long rowNumber) throws FileParsingException {
        String currencyCode = csvArray[CURRENCY];
        Currency currency = currencyRepo.get(currencyCode);

        if (isNull(currency)) {
            throwFileParsingException(format("Invalid currency code '%s'.", currencyCode), rowNumber);
        }
        return currency;
    }

    private void throwFileParsingException(String message, Long rowNumber) throws FileParsingException {
        String errorMsg = format("%sRow number [%d]. %s", DailySalesReportUtils.LOG_PREFIX, rowNumber, message);
        log.error(errorMsg);
        throw new FileParsingException(errorMsg);
    }

}
