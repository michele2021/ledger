package com.accounting.core.reports;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.FileManagerException;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.commons.repository.TxTypeRepoIf;
import com.accounting.commons.repository.TxVariantRepoIf;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.db.MismatchedTokensDAO;
import com.accounting.core.entities.*;
import com.accounting.core.exceptions.DsrReprocessingException;
import com.accounting.core.exceptions.TokenMatchingException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.processing.templates.TokenMatch;
import com.accounting.core.reports.enums.ErrorStatus;
import com.accounting.core.reports.model.DailySalesReport;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Singleton
public final class DailySalesReportsProcessor {

    private final Account account;
    private final JournalDAO journalDAO;
    private final TxTypeRepoIf txTypeRepo;
    private final JournalType journalType;
    private final JournalMaker journalMaker;
    private final TxVariantRepoIf txVariantRepo;
    private final CurrencyRepoIf currencyRepoIf;
    private final RegisterTypeRepoIf registerTypeRepo;
    private final MismatchedTokensDAO mismatchedTokensDAO;
    private final DailySalesReportsImporter reportsImporter;
    private final DsrNotificationsService dsrNotificationsService;

    @Inject
    public DailySalesReportsProcessor(JournalMaker journalMaker,
                                      DailySalesReportsImporter reportsImporter,
                                      DsrNotificationsService dsrNotificationsService,
                                      RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.reportsImporter = reportsImporter;
        this.dsrNotificationsService = dsrNotificationsService;
        this.txTypeRepo = repositoryHelper.getTxTypeRepo();
        this.txVariantRepo = repositoryHelper.getTxVariantRepo();
        this.currencyRepoIf = repositoryHelper.getCurrencyRepo();
        this.registerTypeRepo = repositoryHelper.getRegisterTypeRepo();
        this.journalDAO = repositoryHelper.getJournalDAO();
        this.mismatchedTokensDAO = repositoryHelper.getMismatchedTokensDAO();
        this.journalType = repositoryHelper.getJournalTypeRepo().get(Constants.JournalType.Matched.name());

        this.account = repositoryHelper.getAccountRepo().getByCodeAndType(Constants.ClientAccount.DEFAULT.name(),
                Constants.AccountType.Client.name());
    }

    @Getter
    @AllArgsConstructor
    private static final class ReportErrors {
        private String reportName;
        private List<String> errors;
    }

    public CompletableFuture<Void> processReport(String partialReportName, boolean reprocessing) {

        return CompletableFuture.runAsync(() -> {
            String messagePrefix = reprocessing ? "Re-processing" : "Processing";
            log.info("{}{} daily sales report start. Partial report name:{}", DailySalesReportUtils.LOG_PREFIX, messagePrefix, partialReportName);

            ReportErrors reportErrors = processReportData(partialReportName, reprocessing);
            List<String> errors = reportErrors.getErrors();
            String fullReportName = reportErrors.getReportName();

            if (errors.isEmpty()) {
                reportsImporter.moveToProcessedDir(fullReportName);
                log.info("{}{} daily sales report completed successfully.", DailySalesReportUtils.LOG_PREFIX, messagePrefix);
                return;
            }
            dsrNotificationsService.sendReportProcessedWithErrorsNotification(errors, fullReportName);
            reportsImporter.moveToErrorDir(fullReportName);
            String errorsMessage = String.join("\n", errors);
            log.error("{}{} daily sales report completed with following errors:\n{}", DailySalesReportUtils.LOG_PREFIX, messagePrefix, errorsMessage);
        });
    }

    private ReportErrors processReportData(String partialReportName, boolean reprocessing) {
        final List<String> errors = new ArrayList<>();
        final DailySalesReport report = importDailySalesReport(partialReportName, errors);
        final ReportErrors reportErrors = new ReportErrors(report.getFileName(), errors);

        if (report.getReportRows().isEmpty()) {
            return reportErrors;
        }

        final long reportTxId = createDailySalesReportTx(report, reprocessing);
        processReportRows(reprocessing, errors, report, reportTxId);
        return reportErrors;
    }

    private DailySalesReport importDailySalesReport(String partialReportName, List<String> errors) {
        try {
            return reportsImporter.importReport(partialReportName, errors);
        } catch (FileManagerException ex) {
            log.error("{}Completed with following errors: {}", DailySalesReportUtils.LOG_PREFIX, ex.getMessage());
            dsrNotificationsService.sendReportFileNotFoundNotification(partialReportName);
            throw ex;
        }
    }

    private void processReportRows(boolean reprocessing, List<String> errors, DailySalesReport report, long reportTxId) {
        for (DailySalesReportRow reportRow : report.getReportRows()) {
            TxStatus tx = journalDAO.getTxStatusByReference(reportRow.getTxReference());
            try {
                validateTokensMatch(reportTxId, tx, reprocessing, reportRow);
                // For a "Cancelled" row, we expect to have a preceding "Activated" row, so we do not create again a tx/journal.
                // TODO: to be determined with EPay if we can
                if(reportRow.getJournalType() == Constants.JournalType.Manual) {
                    continue;
                }
                // TODO Consider the possibility to create the TokenReconciliationTx in the TokenMatch template (leveraging the journalMaker framework)
                createMatchedJournal(reportTxId, reportRow, null);
            } catch (TokenMatchingException e) {
                errors.add(e.getMessage());
            } catch (DsrReprocessingException ex) {
                log.error("Exception occurred: ", ex);
            } catch (RuntimeException ex) {
                log.error("Exception occurred: ", ex);
                errors.add(ex.getMessage());
            }
        }
    }

    private void createMatchedJournal(long reportTxId, DailySalesReportRow reportRow, Tx tokenReconciliationTx) throws TokenMatchingException {
        try {
            JournalDb journalDb = journalMaker.createJournal(buildTokenMatchTemplate(reportRow, tokenReconciliationTx));

            // dailySalesReportTx association
            journalDAO.createJournalTx(journalDb.getJournalId(), reportTxId);
        } catch (AccountingMakerException e) {
            handleError(tokenReconciliationTx.getTxId(), reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, e.getMessage());
        }
    }

    private TokenMatch buildTokenMatchTemplate(DailySalesReportRow reportRow, Tx tokenReconciliationTx) {
        return new TokenMatch(account, journalDAO, journalType, tokenReconciliationTx,
                journalMaker, registerTypeRepo, reportRow);
    }

    private long createDailySalesReportTx(DailySalesReport report, boolean reprocessing) {
        // TODO: Currency hardcoded to EUR, as we do not support multi-currency yet
        // TODO: decide what to do in case of error here (database transaction management)
        try {
            Currency currency = currencyRepoIf.get(Constants.Currencies.EUR.name());
            int txVariantId = txVariantRepo.get(Constants.TxVariant.BASE.toString()).getTxVariantId();
            TxType txType = txTypeRepo.get(Constants.TxType.DailySalesReport.name());

            long totalAmount = report.getReportRows().stream()
                    .mapToLong(DailySalesReportRow::getGrossAmountQuantity)
                    .sum();

            Tx tx = Tx.builder()
                    .txTypeId(txType.getTxTypeId())
                    .accountId(account.getAccountId())
                    .amount(Amount.of(totalAmount, currency.getCurrencyId()))
                    .txReference(report.getFileName())
                    .creationDate(OffsetDateTime.now())
                    .txVariantId(txVariantId)
                    .build();

            return reprocessing ? journalDAO.createTxIfNotExists(tx) : journalDAO.createTx(tx);
        } catch (Exception e) {
            log.error("Error while creating the DailySalesReport Tx: ", e);
            dsrNotificationsService.sendReportErrorWhenCreatingDSRTxNotification(report.getFileName());
            throw e;
        }
    }

    private void validateTokensMatch(long reportTxId,
                                     TxStatus tx,
                                     boolean reprocess,
                                     DailySalesReportRow reportRow) throws TokenMatchingException {

        validateToken(reportTxId, tx, reprocess, reportRow);
        validateTransaction(reportTxId, reportRow, tx);
        validateAmountWithFee(reportTxId, reportRow, tx);
    }

    private void validateToken(long reportTxId, TxStatus tx, boolean reprocess,
                               DailySalesReportRow reportRow) throws TokenMatchingException {

        if (isNull(tx)) {
            handleError(null, reportTxId, reportRow, ErrorStatus.NOT_FOUND, "Token not found.");
        }

        validateTokenType(reportTxId, reportRow, tx);

        if(reportRow.getJournalType() == Constants.JournalType.Manual) {
            // For "Cancelled" journal we expect to have already the created the "Matched" journal previously.
            // We return without additional checks (that have already been performed when performing the previous "Activated" line)
            return;
        }
    }

    private void handleTooManyChildTxs(long reportTxId, DailySalesReportRow reportRow, long parentTxId) throws TokenMatchingException {
        String errorMessage = format("More then one child tx with parentTxId '%d' and txType 'TokenReconciliation' exists.", parentTxId);
        handleError(parentTxId, reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
    }

    private void validateTokenType(long reportTxId, DailySalesReportRow reportRow, TxStatus tokenTx) throws TokenMatchingException {
        Constants.TokenType txTokenType = tokenTx.getTokenType();
        Constants.TokenType reportRowTokenType = reportRow.getProductType().getTokenType();

        if (reportRowTokenType.equals(txTokenType)) {
            return;
        }

        String errorMessage = format("Product type mismatch. Report: '%s' actual: '%s'.", reportRowTokenType, txTokenType);
        handleError(tokenTx.getTxId(), reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
    }

    private void handleAlreadyMatchedReprocessing(DailySalesReportRow reportRow, long childTxId) {
        String errorMessage = format("Journal of type 'Matched' already exists for tx with id: '%d' and txReference: %s.",
                childTxId, reportRow.getTxReference());
        log.warn(errorMessage);
        throw new DsrReprocessingException(errorMessage);
    }

    private void handleAlreadyMatched(long reportTxId, DailySalesReportRow reportRow, long childTxId) throws TokenMatchingException {
        String errorMessage = "Journal of type 'Matched' already exists for that token.";
        handleError(childTxId, reportTxId, reportRow, ErrorStatus.ALREADY_MATCHED, errorMessage);
    }

    private void handleNotMatchedButChildTxExists(long reportTxId, DailySalesReportRow reportRow, long childTxId) throws TokenMatchingException {
        String errorMessage = "Child tx of type 'TokenReconciliation' exists, but an associated journal of type 'Matched' was not found.";
        handleError(childTxId, reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
    }

    private void validateTransaction(long reportTxId, DailySalesReportRow reportRow, TxStatus tx) throws TokenMatchingException {
        Journal journal = journalDAO.getJournalByTxIdAndType(tx.getTxId(), reportRow.getJournalType());

        if (isNull(journal)) {
            String errorMessage = format("Journal of type '%s' does not exists for given token.", reportRow.getJournalType());
            handleError(tx.getTxId(), reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
        }

        LocalDate rowBookingDate = reportRow.getBookingDate().toLocalDate();
        LocalDate journalBookingDate = journal.getBookingDate().toLocalDate();
        if (journalBookingDate.isEqual(rowBookingDate)) {
            return;
        }

        String errorMessage = format("Booking dates mismatch report '%s' actual '%s'.", rowBookingDate, journalBookingDate);
        handleError(tx.getTxId(), reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
    }

    private void validateAmountWithFee(long reportTxId, DailySalesReportRow reportRow, TxStatus tx) throws TokenMatchingException {
        long tokenTxId = tx.getTxId();
        Amount tokenTxAmount = tx.getAmount();

        Integer rowCurrencyId = reportRow.getCurrencyId();
        Integer tokenCurrencyId = tokenTxAmount.getCurrencyId();
        if (!tokenCurrencyId.equals(rowCurrencyId)) {
            String errorMessage = format("Currency ids mismatch. Report: '%d' vs Token: '%d'.", rowCurrencyId, tokenCurrencyId);
            handleError(tokenTxId, reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
        }

        Amount receivedFee = journalDAO.getReceivedFee(tokenTxId, reportRow.getJournalType());

        Long txQuantity = tokenTxAmount.getQuantity();
        Long reportRowGrossQuantity = reportRow.getGrossAmountQuantity();
        Long tokenTxTotalAmount = txQuantity;

        if (nonNull(receivedFee)) {
            Long receivedFeeQuantity = Math.abs(receivedFee.getQuantity());
            Long commissionAmountQuantity = reportRow.getCommissionAmountQuantity();
            if (!receivedFeeQuantity.equals(commissionAmountQuantity)) {
                String errorMessage = format("Commission amount quantity mismatch. Report: '%d' vs Token: '%d'", commissionAmountQuantity, receivedFeeQuantity);
                handleError(tokenTxId, reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
            }
            tokenTxTotalAmount = txQuantity + receivedFeeQuantity;
        }
        if (!tokenTxTotalAmount.equals(reportRowGrossQuantity)) {
            String errorMessage = format("Amount quantity mismatch. Report: '%d' vs Token: '%d'", reportRowGrossQuantity, tokenTxTotalAmount);
            handleError(tokenTxId, reportTxId, reportRow, ErrorStatus.VALIDATION_ERROR, errorMessage);
        }
    }

    private void handleError(Long tokenTxId,
                             Long reportTxId,
                             DailySalesReportRow reportRow,
                             ErrorStatus status,
                             String errorMessage) throws TokenMatchingException {

        String message = format("Row number: [%d]. %s", reportRow.getRowNumber(), errorMessage);
        saveErrorRow(tokenTxId, reportTxId, reportRow, status, message);
        throw new TokenMatchingException(message);
    }

    private void saveErrorRow(Long tokenTxId,
                              Long reportTxId,
                              DailySalesReportRow reportRow,
                              ErrorStatus status,
                              String errorMessage) {

        MismatchedToken mismatchedToken = MismatchedToken.builder()
                .txId(reportTxId)
                .tokenTxId(tokenTxId)
                .txReference(reportRow.getTxReference())
                .status(status)
                .errorMessage(errorMessage)
                .build();

        mismatchedTokensDAO.createMismatchedToken(mismatchedToken);
    }
}
