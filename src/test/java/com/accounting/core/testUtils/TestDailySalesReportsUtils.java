package com.accounting.core.testUtils;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Currency;
import com.accounting.core.reports.enums.ProductType;
import com.accounting.core.reports.enums.TransactionType;
import com.accounting.core.reports.model.DailySalesReportRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TestDailySalesReportsUtils {

    public static final String REPORT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String CSV_HEADER = "Product Type;Transaction Type;EAN;Serial/PAN;Terminal ID;TXID;DateTime;Gross Value;Net amount;Commission;Clevercard Transaction ID;Amount;Currency;Activating location Ref";

    public static final ProductType PRODUCT_TYPE = ProductType.POSA;
    public static final String EAN = "EAN(International Article Number)";
    public static final String SERIAL_NUMBER = "41704180714188";
    public static final String TERMINAL_ID = "TERMINAL_ID";
    public static final String EXTERNAL_TX_ID = "TX_ID";
    public static final String TX_REFERENCE = "cd5dab12-b3d8-4816-a6cf-017f81b003dc";
    public static final String GROSS_QUANTITY = "53.00";
    public static final String NET_QUANTITY = "50.00";
    public static final String COMMISSION_QUANTITY = "3.00";
    public static final String AMOUNT_QUANTITY = "53.00";

    public static final long GROSS_QUANTITY_LONG = 5300L;
    public static final long NET_QUANTITY_LONG = 5000L;
    public static final long COMMISSION_QUANTITY_LONG = 300L;

    public static final String CURRENCY_CODE = "EUR";
    public static final String ACTIVATION_LOCATION_REF = "testRef";
    public static final TransactionType TRANSACTION_TYPE = TransactionType.A;
    public static final String CREATION_DATE_TEXT = "2022-06-01 09:05:07";
    public static final Currency CURRENCY = new Currency(2, 1, Constants.Currencies.EUR.name());

    public static final LocalDateTime CREATION_DATE_TIME = LocalDateTime.parse(
            CREATION_DATE_TEXT,
            DateTimeFormatter.ofPattern(REPORT_DATE_PATTERN)
    );


    public static String getReportString(DailySalesReportRow reportRow) {
        return String.join("\n", CSV_HEADER, getDailySalesReportRowAsString(reportRow));
    }

    public static String getReportString(List<DailySalesReportRow> list) {
        return list.stream()
                .map(TestDailySalesReportsUtils::getDailySalesReportRowAsString)
                .collect(Collectors.joining("\n", CSV_HEADER + "\n", ""));
    }

    public static String getDailySalesReportRowAsString() {
        final String DELIMITER = ";";
        return PRODUCT_TYPE + DELIMITER +
                TRANSACTION_TYPE + DELIMITER +
                EAN + DELIMITER +
                SERIAL_NUMBER + DELIMITER +
                TERMINAL_ID + DELIMITER +
                EXTERNAL_TX_ID + DELIMITER +
                CREATION_DATE_TEXT + DELIMITER +
                GROSS_QUANTITY + DELIMITER +
                NET_QUANTITY + DELIMITER +
                COMMISSION_QUANTITY + DELIMITER +
                TX_REFERENCE + DELIMITER +
                AMOUNT_QUANTITY + DELIMITER +
                CURRENCY_CODE + DELIMITER +
                ACTIVATION_LOCATION_REF + DELIMITER;
    }


    public static String getDailySalesReportRowAsString(DailySalesReportRow reportRow) {
        final String DELIMITER = ";";
        return reportRow.getProductType() + DELIMITER +
                reportRow.getTransactionType() + DELIMITER +
                reportRow.getEAN() + DELIMITER +
                reportRow.getSerialNumber() + DELIMITER +
                reportRow.getTerminalId() + DELIMITER +
                reportRow.getExternalId() + DELIMITER +
                reportRow.getBookingDate().toString().replace("T", " ") + DELIMITER +
                Constants.fromMU(Constants.Currencies.valueOf(reportRow.getCurrencyCode()), reportRow.getGrossAmountQuantity()) + DELIMITER +
                Constants.fromMU(Constants.Currencies.valueOf(reportRow.getCurrencyCode()), reportRow.getNetAmountQuantity()) + DELIMITER +
                Constants.fromMU(Constants.Currencies.valueOf(reportRow.getCurrencyCode()), reportRow.getCommissionAmountQuantity()) + DELIMITER +
                reportRow.getTxReference() + DELIMITER +
                Constants.fromMU(Constants.Currencies.valueOf(reportRow.getCurrencyCode()), reportRow.getGrossAmountQuantity()) + DELIMITER +
                reportRow.getCurrencyCode() + DELIMITER +
                reportRow.getActivatingLocationRef() + DELIMITER;
    }

    public static DailySalesReportRow getReportRow() {
        return DailySalesReportRow.builder()
                .rowNumber(1L)
                .grossAmountQuantity(GROSS_QUANTITY_LONG)
                .netAmountQuantity(NET_QUANTITY_LONG)
                .commissionAmountQuantity(COMMISSION_QUANTITY_LONG)
                .EAN(EAN)
                .externalId(EXTERNAL_TX_ID)
                .terminalId(TERMINAL_ID)
                .txReference(TX_REFERENCE)
                .serialNumber(SERIAL_NUMBER)
                .productType(PRODUCT_TYPE)
                .currency(CURRENCY)
                .activatingLocationRef(ACTIVATION_LOCATION_REF)
                .bookingDate(CREATION_DATE_TIME)
                .transactionType(TRANSACTION_TYPE)
                .build();
    }

}
