package com.accounting.core.tasks;

import com.accounting.commons.utils.AccountingUtils;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.DataFixProcessor;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.accounting.commons.tasks.TasksSwitcher.getSingleMandatoryParameter;
import static com.accounting.commons.tasks.TasksSwitcher.getSingleOptionalParameter;
import static java.lang.String.format;


@Slf4j
public class DataManagerTask extends Task {

    private final DataFixProcessor dataFixProcessor;
    private final static String ACTION_PARAM = "action";
    private final static String ORIGINAL_ACCOUNT_ID_TX_PARAM = "originalAccountIdTx";
    private final static String NEW_ACCOUNT_ID_TX_PARAM = "newAccountIdTx";
    private final static String NEW_ACCOUNT_ID_ISSUER_PARAM = "newAccountIdIssuer";
    private final static String HOURS_CLEAN_JOURNALS_PARAM = "hours";
    private final static String ACCOUNT_CODE_PARAM = "accountCode";
    private final static String ISSUER_ACCOUNT_CODE_PARAM = "issuerAccountCode";
    private final static String MODE_PARAM = "mode";
    private final static String TXID_PARAM = "txId";
    private final static String NEW_PAYMENT_ID_PARAM = "newPaymentId";
    private final static String TEST_PARAM = "test";
    private final static int HOURS_CLEAN_JOURNALS = 7;

    @Inject
    public DataManagerTask(DataFixProcessor dataFixProcessor) {
        super("data-fix-processor");
        this.dataFixProcessor = dataFixProcessor;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws WebApplicationException, JobProcessingException {
        String action = getSingleOptionalParameter(parameters, ACTION_PARAM).orElse("CHANGE_ACCOUNT");

        if ("CHANGE_ACCOUNT".equalsIgnoreCase(action)) {
            long originalAccountIdTx = Long.parseLong(getSingleMandatoryParameter(parameters, ORIGINAL_ACCOUNT_ID_TX_PARAM));
            long newAccountIdTx = Long.parseLong(getSingleMandatoryParameter(parameters, NEW_ACCOUNT_ID_TX_PARAM));
            Long newAccountIdIssuer = getSingleOptionalParameter(parameters, NEW_ACCOUNT_ID_ISSUER_PARAM).map(Long::valueOf).orElse(null);
            boolean test = Boolean.TRUE.equals(getSingleOptionalParameter(parameters, TEST_PARAM).map(Boolean::valueOf).orElse(false));
            dataFixProcessor.changeAccount(test, originalAccountIdTx, newAccountIdTx, newAccountIdIssuer);
        } else if ("CLEAN_TEST_JOURNALS".equalsIgnoreCase(action)) {
            Set<String> testTokensTxReferences = Collections.emptySet();
            int hoursInterval = getSingleOptionalParameter(parameters, HOURS_CLEAN_JOURNALS_PARAM).map(Integer::valueOf).orElse(HOURS_CLEAN_JOURNALS);
            if (AccountingUtils.isEmptyOrNull(testTokensTxReferences)) {
                log.info("No test tokens to process in this environment!");
                return;
            }
            dataFixProcessor.cleanJournals(testTokensTxReferences, hoursInterval);
        } else if ("ACTIVATED_JOURNAL_FIX".equalsIgnoreCase(action)) {
            String accountCode = getSingleMandatoryParameter(parameters, ACCOUNT_CODE_PARAM);
            boolean test = Boolean.TRUE.equals(getSingleOptionalParameter(parameters, TEST_PARAM).map(Boolean::valueOf).orElse(false));
            dataFixProcessor.activatedJournalFeesOpenBalanceFix(accountCode, test);
        } else if ("ACTIVATED_JOURNAL_ADD_MISSING_JL".equalsIgnoreCase(action)) {
            String accountCode = getSingleMandatoryParameter(parameters, ACCOUNT_CODE_PARAM);
            boolean test = Boolean.TRUE.equals(getSingleOptionalParameter(parameters, TEST_PARAM).map(Boolean::valueOf).orElse(false));
            String issuerAccountCode = getSingleMandatoryParameter(parameters, ISSUER_ACCOUNT_CODE_PARAM);
            dataFixProcessor.activatedJournalAddMissingLines(accountCode, issuerAccountCode, test);
        } else if ("PAYMENT_REVERSE_AND_SUBMIT".equalsIgnoreCase(action)) {
            String mode = getSingleMandatoryParameter(parameters, MODE_PARAM);
            Long txId = getSingleOptionalParameter(parameters, TXID_PARAM).map(Long::valueOf).orElse(null);
            Long newPaymentId = getSingleOptionalParameter(parameters, NEW_PAYMENT_ID_PARAM).map(Long::valueOf).orElse(null);
            boolean test = Boolean.TRUE.equals(getSingleOptionalParameter(parameters, TEST_PARAM).map(Boolean::valueOf).orElse(false));
            dataFixProcessor.reverseAndSubmitPayments(mode, txId, newPaymentId, test);
        } else {
            throw new WebApplicationException(format("Parameter '%s' has an unrecognized value '%s'. Supported values: 'CHANGE_ACCOUNT'.", ACTION_PARAM, action), Response.Status.BAD_REQUEST);
        }
    }

}