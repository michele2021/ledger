package com.accounting.core.reports;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.entities.TxVariant;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.FileManagerException;
import com.accounting.core.entities.*;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.processing.templates.TokenMatch;
import com.accounting.core.reports.enums.ErrorStatus;
import com.accounting.core.reports.enums.ProductType;
import com.accounting.core.reports.enums.TransactionType;
import com.accounting.core.reports.model.DailySalesReport;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.testUtils.TestDailySalesReportsUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySalesReportsProcessorTest {

    private static final long REPORT_TX_ID = 1L;
    private static final String TEST_REPORT_NAME = "test-report";
    private static final TxVariant TX_VARIANT = new TxVariant(1, "BASE");
    private static final Currency CURRENCY = new Currency(2, 1, "EUR");
    private static final TxType DSR_TX_TYPE = new TxType(9, Constants.TxType.DailySalesReport.name());
    private static final Account ACCOUNT = Account.builder()
            .accountId(16)
            .accountCode("DEFAULT")
            .accountTypeId(7)
            .accountTypeCode(Constants.AccountType.Client.name())
            .parentId(15L)
            .parentAccountCode("EPAY")
            .description("")
            .test(false)
            .created(OffsetDateTime.now())
            .createdBy("user1")
            .build();
    private static final JournalType JOURNAL_TYPE = new JournalType(14, Constants.JournalType.Matched.name());

    @Mock
    private JournalMaker journalMaker;
    @Mock
    private DailySalesReportsImporter reportsImporter;
    @Mock
    private DsrNotificationsService dsrNotificationsService;

    @SuppressWarnings("unused")
    private final RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    private DailySalesReportsProcessor reportsProcessor;

    private Journal journal;

    @BeforeEach
    void init() throws IllegalAccessException {

        FieldUtils.writeField(reportsProcessor, "account", ACCOUNT, true);
        FieldUtils.writeField(reportsProcessor, "journalType", JOURNAL_TYPE, true);

        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(TX_VARIANT);
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(CURRENCY);
        when(repositoryHelper.getTxTypeRepo().get(Constants.TxType.DailySalesReport.name())).thenReturn(DSR_TX_TYPE);

        OffsetDateTime bookingDate = OffsetDateTime.of(TestDailySalesReportsUtils.CREATION_DATE_TIME, ZoneOffset.ofTotalSeconds(0));
        journal = Journal.builder()
                .journalId(1L)
                .journalTypeId(1)
                .bookingDate(bookingDate)
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Disabled
    void testProcessReportSuccess(boolean reprocessing) throws AccountingMakerException, ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);

        Constants.JournalType journalType = reportRow.getJournalType();
        Integer rowCurrencyId = reportRow.getCurrencyId();

        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        final long parentTxId = 1L;
        TxStatus parentTx = new TxStatus();
        parentTx.setTxId(parentTxId);
        parentTx.setTokenType(Constants.TokenType.POSA);
        parentTx.setAmount(new Amount(5000L, CURRENCY));

        final long feeQuantity = 300L;
        Long rowQuantity = reportRow.getGrossAmountQuantity();
        Long txTotalQuantity = parentTx.getAmount().getQuantity() + feeQuantity;

        final long childTxId = 2L;
        final long journalId = 1L;
        JournalDb journalDb = JournalDb.builder()
                .txId(parentTxId)
                .journalId(journalId)
                .build();

        assertEquals(CURRENCY.getCurrencyId(), rowCurrencyId);
        assertEquals(txTotalQuantity, rowQuantity);

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(parentTx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(parentTxId, journalType)).thenReturn(journal);
        when(repositoryHelper.getJournalDAO().getReceivedFee(parentTxId, journalType)).thenReturn(Amount.of(feeQuantity, rowCurrencyId));

        when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(childTxId);
        when(journalMaker.createJournal(any(TokenMatch.class))).thenReturn(journalDb);
        whenTxCreate(reprocessing, REPORT_TX_ID);

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verify(repositoryHelper.getTxTypeRepo()).get(Constants.TxType.DailySalesReport.name());
        verify(repositoryHelper.getTxVariantRepo()).get(Constants.TxVariant.BASE.toString());

        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(parentTxId, journalType);
        verify(repositoryHelper.getJournalDAO()).getReceivedFee(parentTxId, journalType);
        verify(journalMaker).createJournal(any(TokenMatch.class));
        verify(repositoryHelper.getJournalDAO()).createJournalTxBulk(journalDb.getJournalId(), List.of(REPORT_TX_ID));

        if (reprocessing) {
            verify(repositoryHelper.getJournalDAO()).createTxIfNotExists(any(Tx.class));
            verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        } else {
            verify(repositoryHelper.getJournalDAO(), times(2)).createTx(any(Tx.class));
        }

        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verifyNoInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(reportsImporter).moveToProcessedDir(TEST_REPORT_NAME);

        verifyNoInteractions(repositoryHelper.getMismatchedTokensDAO());
        verify(reportsImporter, times(0)).moveToErrorDir(TEST_REPORT_NAME);
    }

    @Test
    @Disabled
    void testProcessReportNoReportFound() {
        // given
        when(reportsImporter.importReport(any(), anyList())).thenThrow(new FileManagerException(""));

        // when
        assertThrows(ExecutionException.class, () -> reportsProcessor.processReport(TEST_REPORT_NAME, false).get());

        // then
        verify(reportsImporter).importReport(any(), anyList());
        verifyNoMoreInteractions(reportsImporter);

        verify(dsrNotificationsService).sendReportFileNotFoundNotification(TEST_REPORT_NAME);
        verifyNoMoreInteractions(dsrNotificationsService);

        verifyNoInteractions(repositoryHelper.getCurrencyRepo());
        verifyNoInteractions(repositoryHelper.getTxVariantRepo());
        verifyNoInteractions(repositoryHelper.getJournalDAO());
        verifyNoInteractions(repositoryHelper.getMismatchedTokensDAO());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessReportWithOnlyInvalidRows(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, Collections.emptyList());

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenAnswer(m -> {
            List<String> argument = m.getArgument(1);
            argument.add("Row number [1]. Invalid product type 'VALUE'.");
            return report;
        });

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
        verifyNoMoreInteractions(reportsImporter);

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verifyNoInteractions(repositoryHelper.getCurrencyRepo());
        verifyNoInteractions(repositoryHelper.getTxVariantRepo());
        verifyNoInteractions(repositoryHelper.getJournalDAO());
        verifyNoInteractions(repositoryHelper.getMismatchedTokensDAO());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessReportTokenNotFound(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();
        String txReference2 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow1 = TestDailySalesReportsUtils.getReportRow();
        reportRow1.setRowNumber(1L);
        reportRow1.setTxReference(txReference1);
        reportRow1.setTransactionType(TransactionType.A);

        DailySalesReportRow reportRow2 = TestDailySalesReportsUtils.getReportRow();
        reportRow2.setRowNumber(2L);
        reportRow2.setTxReference(txReference2);
        reportRow2.setTransactionType(TransactionType.D);

        List<DailySalesReportRow> reportRows = List.of(reportRow1, reportRow2);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);
        long expectedReportTxId = 1L;

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(null);

        whenTxCreate(reprocessing, expectedReportTxId);

        MismatchedToken expectedMismatchToken1 = MismatchedToken.builder()
                .tokenTxId(null)
                .txId(expectedReportTxId)
                .txReference(reportRow1.getTxReference())
                .status(ErrorStatus.NOT_FOUND)
                .errorMessage(format("Row number: [%d]. Token not found.", reportRow1.getRowNumber()))
                .build();

        MismatchedToken expectedMismatchToken2 = MismatchedToken.builder()
                .tokenTxId(null)
                .txId(expectedReportTxId)
                .txReference(reportRow2.getTxReference())
                .status(ErrorStatus.NOT_FOUND)
                .errorMessage(format("Row number: [%d]. Token not found.", reportRow2.getRowNumber()))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verifyTxCreate(reprocessing);

        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow1.getTxReference());
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow2.getTxReference());
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken1);
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken2);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @Test
    void testProcessReportTokenTypeMismatch() throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow1 = TestDailySalesReportsUtils.getReportRow();
        reportRow1.setRowNumber(1L);
        reportRow1.setTxReference(txReference1);
        reportRow1.setTransactionType(TransactionType.A);
        reportRow1.setProductType(ProductType.POSA);

        List<DailySalesReportRow> reportRows = List.of(reportRow1);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);

        long expectedReportTxId = 1L;
        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(expectedReportTxId);

        Constants.TokenType txTokenType = Constants.TokenType.PoR;
        Constants.TokenType reportRowTokenType = reportRow1.getProductType().getTokenType();

        TxStatus txMock = mock(TxStatus.class);
        txMock.setTxId(1L);

        when(txMock.getTokenType()).thenReturn(txTokenType);
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(txMock);

        MismatchedToken expectedMismatchToken1 = MismatchedToken.builder()
                .tokenTxId(txMock.getTxId())
                .txId(expectedReportTxId)
                .txReference(reportRow1.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Product type mismatch. Report: '%s' actual: '%s'.",
                        reportRow1.getRowNumber(), reportRowTokenType, txTokenType))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, false).get();

        // then
        verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow1.getTxReference());

        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken1);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @Test
    @Disabled
    void testProcessReportTooManyChildTxs() throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow1 = TestDailySalesReportsUtils.getReportRow();
        reportRow1.setRowNumber(1L);
        reportRow1.setTxReference(txReference1);
        reportRow1.setTransactionType(TransactionType.A);
        reportRow1.setProductType(ProductType.POSA);

        List<DailySalesReportRow> reportRows = List.of(reportRow1);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);

        long expectedReportTxId = 1L;
        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(expectedReportTxId);

        long parentTxId = 1L;
        TxStatus txMock = mock(TxStatus.class);
        txMock.setTxId(parentTxId);

        when(txMock.getTxId()).thenReturn(parentTxId);
        when(txMock.getTokenType()).thenReturn(reportRow1.getProductType().getTokenType());
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(txMock);

        MismatchedToken expectedMismatchToken1 = MismatchedToken.builder()
                .tokenTxId(txMock.getTxId())
                .txId(expectedReportTxId)
                .txReference(reportRow1.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. More then one child tx with parentTxId '%d' and txType 'TokenReconciliation' exists.",
                        reportRow1.getRowNumber(), expectedReportTxId))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, false).get();

        // then
        verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow1.getTxReference());

        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken1);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @Test
    @Disabled
    void testProcessReportTokenAlreadyMatched() throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference1);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        List<DailySalesReportRow> reportRows = List.of(reportRow);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);
        long expectedReportTxId = 1L;

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(expectedReportTxId);

        long parentTxId = 1L;
        TxStatus parentTxMock = mock(TxStatus.class);
        parentTxMock.setTxId(parentTxId);

        when(parentTxMock.getTxId()).thenReturn(parentTxId);
        when(parentTxMock.getTokenType()).thenReturn(reportRow.getProductType().getTokenType());

        long childTxId = 2L;
        TxStatus childTxMock = mock(TxStatus.class);
        childTxMock.setTxId(childTxId);

        when(childTxMock.getTxId()).thenReturn(childTxId);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(parentTxMock);
        when(repositoryHelper.getJournalDAO().doesJournalExist(anyLong(), eq(Constants.JournalType.Matched))).thenReturn(true);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(childTxMock.getTxId())
                .txId(expectedReportTxId)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.ALREADY_MATCHED)
                .errorMessage(format("Row number: [%d]. Journal of type 'Matched' already exists for that token.", reportRow.getRowNumber()))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, false).get();

        // then
        verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow.getTxReference());
        verify(repositoryHelper.getJournalDAO()).doesJournalExist(anyLong(), eq(Constants.JournalType.Matched));
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @Test
    @Disabled
    void testProcessReportTokenAlreadyMatchedReportReprocessing() throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference1);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        List<DailySalesReportRow> reportRows = List.of(reportRow);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);
        long expectedReportTxId = 1L;

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().createTxIfNotExists(any(Tx.class))).thenReturn(expectedReportTxId);

        long parentTxId = 1L;
        TxStatus parentTxMock = mock(TxStatus.class);
        parentTxMock.setTxId(parentTxId);

        when(parentTxMock.getTxId()).thenReturn(parentTxId);
        when(parentTxMock.getTokenType()).thenReturn(reportRow.getProductType().getTokenType());

        long childTxId = 2L;
        TxStatus childTxMock = mock(TxStatus.class);
        childTxMock.setTxId(childTxId);

        when(childTxMock.getTxId()).thenReturn(childTxId);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(parentTxMock);
        when(repositoryHelper.getJournalDAO().doesJournalExist(anyLong(), eq(Constants.JournalType.Matched))).thenReturn(true);

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, true).get();

        // then
        verify(repositoryHelper.getJournalDAO()).createTxIfNotExists(any(Tx.class));
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow.getTxReference());
        verify(repositoryHelper.getJournalDAO()).doesJournalExist(anyLong(), eq(Constants.JournalType.Matched));

        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());
        verifyNoInteractions(repositoryHelper.getMismatchedTokensDAO());
        verifyNoInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(reportsImporter).moveToProcessedDir(TEST_REPORT_NAME);
    }

    @Test
    @Disabled
    void testProcessReportNotMatchedButChildTxExists() throws ExecutionException, InterruptedException {
        // given
        String txReference1 = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference1);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        List<DailySalesReportRow> reportRows = List.of(reportRow);
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, reportRows);
        long expectedReportTxId = 1L;

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(expectedReportTxId);

        long parentTxId = 1L;
        TxStatus parentTxMock = mock(TxStatus.class);
        parentTxMock.setTxId(parentTxId);

        long childTxId = 2L;
        TxStatus childTxMock = mock(TxStatus.class);
        childTxMock.setTxId(childTxId);

        when(parentTxMock.getTxId()).thenReturn(parentTxId);
        when(parentTxMock.getTokenType()).thenReturn(reportRow.getProductType().getTokenType());

        when(childTxMock.getTxId()).thenReturn(childTxId);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(anyString())).thenReturn(parentTxMock);
        when(repositoryHelper.getJournalDAO().doesJournalExist(anyLong(), eq(Constants.JournalType.Matched))).thenReturn(false);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(childTxMock.getTxId())
                .txId(expectedReportTxId)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Child tx of type 'TokenReconciliation' exists, but an associated journal of type 'Matched' was not found.", reportRow.getRowNumber()))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, false).get();

        // then
        verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(reportRow.getTxReference());
        verify(repositoryHelper.getJournalDAO()).doesJournalExist(anyLong(), eq(Constants.JournalType.Matched));
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Disabled
    void testProcessReportJournalOfTransactionTypeDoesNotExists(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        long parentTxId = 1L;
        TxStatus parentTx = new TxStatus();
        parentTx.setTxId(parentTxId);
        parentTx.setTokenType(reportRow.getProductType().getTokenType());

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);

        whenTxCreate(reprocessing, REPORT_TX_ID);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(parentTx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(parentTxId, reportRow.getJournalType())).thenReturn(null);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(parentTxId)
                .txId(REPORT_TX_ID)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Journal of type '%s' does not exists for given token.",
                        reportRow.getRowNumber(), reportRow.getJournalType()))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verifyTxCreate(reprocessing);
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(parentTxId, reportRow.getJournalType());
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessReportBookingDateMismatch(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        long parentTxId = 1L;
        TxStatus parentTx = new TxStatus();
        parentTx.setTxId(parentTxId);
        parentTx.setTokenType(reportRow.getProductType().getTokenType());

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);

        whenTxCreate(reprocessing, REPORT_TX_ID);

        journal.setBookingDate(OffsetDateTime.of(reportRow.getBookingDate().minusDays(1), ZoneOffset.ofTotalSeconds(0)));

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(parentTx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(parentTxId, reportRow.getJournalType())).thenReturn(journal);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(parentTxId)
                .txId(REPORT_TX_ID)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Booking dates mismatch report '%s' actual '%s'.",
                        reportRow.getRowNumber(), reportRow.getBookingDate().toLocalDate(), journal.getBookingDate().toLocalDate()))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verifyTxCreate(reprocessing);
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(parentTxId, reportRow.getJournalType());
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessReportCurrencyCodeNotMatch(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        whenTxCreate(reprocessing, REPORT_TX_ID);

        long txId = 1L;
        TxStatus tx = new TxStatus();
        tx.setTxId(txId);
        tx.setAmount(Amount.of(5000, 9999));
        tx.setTokenType(reportRow.getProductType().getTokenType());

        Integer txCurrencyId = tx.getAmount().getCurrencyId();
        Integer rowCurrencyId = reportRow.getCurrencyId();

        assertNotEquals(txCurrencyId, rowCurrencyId);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(tx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(txId, reportRow.getJournalType())).thenReturn(journal);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(txId)
                .txId(REPORT_TX_ID)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Currency ids mismatch. Report: '%d' vs Token: '%d'.",
                        reportRow.getRowNumber(), rowCurrencyId, txCurrencyId))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verifyTxCreate(reprocessing);
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(txId, reportRow.getJournalType());
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessReportAmountQuantityNotMatch(boolean reprocessing) throws ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.POSA);

        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        whenTxCreate(reprocessing, REPORT_TX_ID);

        Integer rowCurrencyId = reportRow.getCurrencyId();

        long txId = 1L;
        TxStatus tx = new TxStatus();
        tx.setTxId(txId);
        tx.setAmount(Amount.of(10000, rowCurrencyId));
        tx.setTokenType(reportRow.getProductType().getTokenType());

        Constants.JournalType rowJournalType = reportRow.getJournalType();
        Integer txCurrencyId = tx.getAmount().getCurrencyId();

        long feeQuantity = 300L;
        Long rowQuantity = reportRow.getGrossAmountQuantity();
        Long txTotalQuantity = tx.getAmount().getQuantity() + feeQuantity;
        assertEquals(txCurrencyId, rowCurrencyId);
        assertNotEquals(txTotalQuantity, rowQuantity);

        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(tx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(txId, rowJournalType)).thenReturn(journal);
        when(repositoryHelper.getJournalDAO().getReceivedFee(txId, rowJournalType)).thenReturn(Amount.of(feeQuantity, rowCurrencyId));

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(txId)
                .txId(REPORT_TX_ID)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Amount quantity mismatch. Report: '%d' vs Token: '%d'",
                        reportRow.getRowNumber(), rowQuantity, txTotalQuantity))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        verifyTxCreate(reprocessing);
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(txId, rowJournalType);
        verify(repositoryHelper.getJournalDAO()).getReceivedFee(txId, rowJournalType);
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Disabled
    void testProcessReportCreateJournalsException(boolean reprocessing) throws AccountingMakerException, ExecutionException, InterruptedException {
        // given
        String txReference = UUID.randomUUID().toString();

        DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();
        reportRow.setRowNumber(1L);
        reportRow.setTxReference(txReference);
        reportRow.setTransactionType(TransactionType.A);
        reportRow.setProductType(ProductType.PIN);

        Integer rowCurrencyId = reportRow.getCurrencyId();
        DailySalesReport report = new DailySalesReport(TEST_REPORT_NAME, List.of(reportRow));

        long parentTxId = 1L;
        TxStatus parentTx = new TxStatus();
        parentTx.setTxId(parentTxId);
        parentTx.setAmount(Amount.of(5000, rowCurrencyId));
        parentTx.setTokenType(reportRow.getProductType().getTokenType());
        Integer txCurrencyId = parentTx.getAmount().getCurrencyId();
        Constants.JournalType rowJournalType = reportRow.getJournalType();

        long feeQuantity = 300L;
        Long rowQuantity = reportRow.getGrossAmountQuantity();
        Long txTotalQuantity = parentTx.getAmount().getQuantity() + feeQuantity;

        assertEquals(txCurrencyId, rowCurrencyId);
        assertEquals(txTotalQuantity, rowQuantity);

        whenTxCreate(reprocessing, REPORT_TX_ID);
        when(reportsImporter.importReport(eq(TEST_REPORT_NAME), anyList())).thenReturn(report);
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(txReference)).thenReturn(parentTx);
        when(repositoryHelper.getJournalDAO().getJournalByTxIdAndType(parentTxId, rowJournalType)).thenReturn(journal);
        when(repositoryHelper.getJournalDAO().getReceivedFee(parentTxId, rowJournalType)).thenReturn(Amount.of(feeQuantity, rowCurrencyId));

        final long childTxId = 2L;
        if (reprocessing) {
            when(repositoryHelper.getJournalDAO().createTxIfNotExists(any(Tx.class))).thenReturn(REPORT_TX_ID);
            when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(childTxId);
        } else {
            when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(REPORT_TX_ID, childTxId);
        }

        when(journalMaker.createJournal(any(TokenMatch.class)))
                .thenThrow(new AccountingMakerException(format("Specialized Tx is of an unrecognized type, for txId: %s", childTxId)));

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .tokenTxId(childTxId)
                .txId(REPORT_TX_ID)
                .txReference(reportRow.getTxReference())
                .status(ErrorStatus.VALIDATION_ERROR)
                .errorMessage(format("Row number: [%d]. Specialized Tx is of an unrecognized type, for txId: %s",
                        reportRow.getRowNumber(), childTxId))
                .build();

        // when
        reportsProcessor.processReport(TEST_REPORT_NAME, reprocessing).get();

        // then
        if (reprocessing) {
            verify(repositoryHelper.getJournalDAO()).createTxIfNotExists(any(Tx.class));
            verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        } else {
            verify(repositoryHelper.getJournalDAO(), times(2)).createTx(any(Tx.class));
        }

        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(txReference);
        verify(repositoryHelper.getJournalDAO()).getJournalByTxIdAndType(parentTxId, rowJournalType);
        verify(repositoryHelper.getJournalDAO()).getReceivedFee(parentTxId, rowJournalType);
        verify(journalMaker).createJournal(any(TokenMatch.class));
        verifyNoMoreInteractions(repositoryHelper.getJournalDAO());

        verify(dsrNotificationsService).sendReportProcessedWithErrorsNotification(anyList(), eq(report.getFileName()));
        verifyNoMoreInteractions(dsrNotificationsService);

        verify(reportsImporter).importReport(eq(TEST_REPORT_NAME), anyList());
        verify(repositoryHelper.getMismatchedTokensDAO()).createMismatchedToken(expectedMismatchToken);
        verify(reportsImporter).moveToErrorDir(TEST_REPORT_NAME);
    }

    private void whenTxCreate(boolean reprocessing, long expectedReportTxId) {
        if (reprocessing) {
            when(repositoryHelper.getJournalDAO().createTxIfNotExists(any(Tx.class))).thenReturn(expectedReportTxId);
        } else {
            when(repositoryHelper.getJournalDAO().createTx(any(Tx.class))).thenReturn(expectedReportTxId);
        }
    }

    private void verifyTxCreate(boolean reprocessing) {
        if (reprocessing) {
            verify(repositoryHelper.getJournalDAO()).createTxIfNotExists(any(Tx.class));
        } else {
            verify(repositoryHelper.getJournalDAO()).createTx(any(Tx.class));
        }
    }
}
