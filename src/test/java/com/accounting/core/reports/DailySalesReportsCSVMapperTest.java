package com.accounting.core.reports;

import com.accounting.commons.entities.Currency;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.core.exceptions.FileParsingException;
import com.accounting.core.reports.enums.TransactionType;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.accounting.core.testUtils.TestDailySalesReportsUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.accounting.core.reports.DailySalesReportsCSVMapper.COLUMN_DELIMITER;
import static com.accounting.core.testUtils.TestDailySalesReportsUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySalesReportsCSVMapperTest {

    private static final long ROW_NUMBER = 1L;

    @Mock
    private CurrencyRepoIf currencyRepo;

    @InjectMocks
    private DailySalesReportsCSVMapper csvMapper;

    private final Currency currency = new Currency(2, 1, "EUR");

    @AfterEach
    void reset() {
        Mockito.reset(currencyRepo);
    }

    @Test
    void testMapSuccess() throws FileParsingException {
        // given
        DailySalesReportRow expectedRow = TestDailySalesReportsUtils.getReportRow();
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString(expectedRow);
        String rowGrossQuantity = csvLine.split(COLUMN_DELIMITER)[DailySalesReportsCSVMapper.Columns.GROSS_AMOUNT];
        String rowNetQuantity = csvLine.split(COLUMN_DELIMITER)[DailySalesReportsCSVMapper.Columns.NET_AMOUNT];
        String rowCommissionQuantity = csvLine.split(COLUMN_DELIMITER)[DailySalesReportsCSVMapper.Columns.COMMISSION];
        String rowAmountQuantity = csvLine.split(COLUMN_DELIMITER)[DailySalesReportsCSVMapper.Columns.AMOUNT];

        when(currencyRepo.get(CURRENCY_CODE)).thenReturn(currency);

        // when
        DailySalesReportRow actualRow = csvMapper.map(csvLine, ROW_NUMBER);

        // then
        assertEquals(new BigDecimal(GROSS_QUANTITY).toString(), rowGrossQuantity);
        assertEquals(new BigDecimal(NET_QUANTITY).toString(), rowNetQuantity);
        assertEquals(new BigDecimal(COMMISSION_QUANTITY).toString(), rowCommissionQuantity);
        assertEquals(new BigDecimal(AMOUNT_QUANTITY).toString(), rowAmountQuantity);

        assertEquals(expectedRow, actualRow);
        assertEquals(GROSS_QUANTITY_LONG, actualRow.getGrossAmountQuantity());
        assertEquals(NET_QUANTITY_LONG, actualRow.getNetAmountQuantity());
        assertEquals(COMMISSION_QUANTITY_LONG, actualRow.getCommissionAmountQuantity());

        verify(currencyRepo).get(CURRENCY_CODE);
    }

    @Test
    void testMapColumnWithQuotationSuccess() throws FileParsingException {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(";" + CURRENCY_CODE + ";", ";\"" + CURRENCY_CODE + "\";");

        String currencyString = csvLine.split(COLUMN_DELIMITER)[DailySalesReportsCSVMapper.Columns.CURRENCY];
        assertThat(currencyString).contains("\"");

        when(currencyRepo.get(CURRENCY_CODE)).thenReturn(currency);

        // when
        csvMapper.map(csvLine, ROW_NUMBER);

        // then
        verify(currencyRepo).get(CURRENCY_CODE);
    }

    /*
     *  Valid CVS header
     * "Product Type;Transaction Type;EAN;Serial/PAN;Terminal ID;TXID;DateTime;Gross Value;Net amount;Commission;rule30 Transaction ID;Amount;Currency;Activating location Ref"
     */
    @Test
    void testMapShouldFailIfColumnsCountNotMatch() {
        // given
        String dailySalesReportRowAsString = TestDailySalesReportsUtils.getDailySalesReportRowAsString();
        String toManyColumnsLine = "INVALID_COLUMN" + ";" + dailySalesReportRowAsString;

        String toLessColumnsLine = dailySalesReportRowAsString.replace(";", "");

        when(currencyRepo.get(CURRENCY_CODE)).thenReturn(currency);

        // when
        FileParsingException exception1 = assertThrows(FileParsingException.class, () -> csvMapper.map(toManyColumnsLine, ROW_NUMBER));
        FileParsingException exception2 = assertThrows(FileParsingException.class, () -> csvMapper.map(toLessColumnsLine, ROW_NUMBER));

        // then
        String expectedMessage = format("Row number [%d]. Invalid number of columns", ROW_NUMBER);
        assertThat(exception1.getMessage()).contains(expectedMessage);
        assertThat(exception2.getMessage()).contains(expectedMessage);
        verifyNoInteractions(currencyRepo);
    }

    @Test
    void testMapInvalidProductType() {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(PRODUCT_TYPE.name() + ";", "INVALID;");

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid product type", ROW_NUMBER));
        verifyNoInteractions(currencyRepo);
    }

    @Test
    void testMapInvalidTransactionType() {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(";" + TRANSACTION_TYPE.name() + ";", ";INVALID;");

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid transaction type", ROW_NUMBER));
        verifyNoInteractions(currencyRepo);
    }

    @Test
    void testMapInvalidNonNumericSerialNumber() {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(";" + SERIAL_NUMBER + ";", ";Non Numeric Id 921312;");

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid serialNumber", ROW_NUMBER));
        verifyNoInteractions(currencyRepo);
    }

    public static Stream<Arguments> nonNumericAmountQuantitySupplier() {
        return Stream.of(
                Arguments.of(";50.00EUR;", "GROSS_AMOUNT", ";" + GROSS_QUANTITY + ";"),
                Arguments.of(";50.00 USD;", "NET_AMOUNT", ";" + NET_QUANTITY + ";"),
                Arguments.of(";EUR 300;", "COMMISSION", ";" + COMMISSION_QUANTITY + ";")
        );
    }

    @ParameterizedTest
    @MethodSource("nonNumericAmountQuantitySupplier")
    void testMapInvalidNonNumericAmountQuantity(String invalidQuantity, String columnName, String currentQuantity) {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(currentQuantity, invalidQuantity);

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid '%s' column", ROW_NUMBER, columnName));
        verifyNoInteractions(currencyRepo);
    }

    @Test
    void testMapAmountsBalancedCancellationTransactionType() {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(";" + COMMISSION_QUANTITY + ";", ";3.00;")
                .replace(";" + TRANSACTION_TYPE.name() + ";", ";" + TransactionType.D.name() + ";");

        when(currencyRepo.get(CURRENCY_CODE)).thenReturn(currency);

        // when
        DailySalesReportRow dailySalesReportRow = assertDoesNotThrow(() -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(dailySalesReportRow.getCommissionAmountQuantity()).isEqualTo(300L);
        verify(currencyRepo).get(CURRENCY_CODE);
    }

    public static Stream<Arguments> amountNotBalancedSupplier() {
        return Stream.of(
                Arguments.of(";50.00;", ";" + GROSS_QUANTITY + ";", TransactionType.A.name()),
                Arguments.of(";56.00;", ";" + GROSS_QUANTITY + ";", TransactionType.A.name()),
                Arguments.of(";53.00;", ";" + NET_QUANTITY + ";", TransactionType.A.name()),
                Arguments.of(";56.00;", ";" + NET_QUANTITY + ";", TransactionType.A.name()),
                Arguments.of(";4.00;", ";" + COMMISSION_QUANTITY + ";", TransactionType.A.name()),
                Arguments.of(";-6.00;", ";" + COMMISSION_QUANTITY + ";", TransactionType.D.name()),
                Arguments.of(";-4.00;", ";" + COMMISSION_QUANTITY + ";", TransactionType.D.name())
        );
    }

    @ParameterizedTest
    @MethodSource("amountNotBalancedSupplier")
    void testMapAmountsNotBalanced(String invalidQuantity, String currentQuantity, String transactionType) {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(currentQuantity, invalidQuantity)
                .replace(";" + TRANSACTION_TYPE.name() + ";", ";" + transactionType + ";");

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        if (transactionType.equals(TransactionType.A.name())) {
            assertThat(exception.getMessage()).contains(format("Row number [%d]. Amounts for transaction type 'A' are not properly balanced", ROW_NUMBER));
        }

        if (transactionType.equals(TransactionType.D.name())) {
            assertThat(exception.getMessage()).contains(format("Row number [%d]. Amounts for transaction type 'D' are not properly balanced", ROW_NUMBER));
        }
        verifyNoInteractions(currencyRepo);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2022-06-01T09:05:07", "2021/05/03 10:06:27", "2023-10-11", "2022-06-01 09:05"
    })
    void testMapInvalidDateFormat(String invalidDate) {
        // given
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(CREATION_DATE_TEXT, invalidDate);

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid date time format", ROW_NUMBER));
        verifyNoInteractions(currencyRepo);
    }

    @Test
    void testMapInvalidCurrencyCode() {
        // given
        String invalidCurrencyCode = "INVALID";
        String csvLine = TestDailySalesReportsUtils.getDailySalesReportRowAsString()
                .replace(";" + CURRENCY_CODE + ";", ";" + invalidCurrencyCode + ";");

        when(currencyRepo.get(invalidCurrencyCode)).thenReturn(null);

        // when
        FileParsingException exception = assertThrows(FileParsingException.class, () -> csvMapper.map(csvLine, ROW_NUMBER));

        // then
        assertThat(exception.getMessage()).contains(format("Row number [%d]. Invalid currency code", ROW_NUMBER));
        verify(currencyRepo).get(invalidCurrencyCode);
    }
}
