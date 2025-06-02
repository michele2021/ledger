package com.accounting.core.jobs;

import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.datafix.*;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class DataFixProcessor {

    private final MoveAccountExecutor moveAccountExecutor;
    private final CreateTestAccountExecutor createTestAccountExecutor;
    private final JournalCleaningExecutor journalCleaningExecutor;
    private final ActivatedJournalFeesOpenBalanceFixExecutor activatedJournalFeesOpenBalanceFixExecutor;
    private final PaymentsReverseExecutor paymentsReverseExecutor;

    @Inject
    public DataFixProcessor(MoveAccountExecutor moveAccountExecutor,
                            CreateTestAccountExecutor createTestAccountExecutor,
                            JournalCleaningExecutor journalCleaningExecutor,
                            ActivatedJournalFeesOpenBalanceFixExecutor activatedJournalFeesOpenBalanceFixExecutor,
                            PaymentsReverseExecutor paymentsReverseExecutor) {
        this.moveAccountExecutor = moveAccountExecutor;
        this.createTestAccountExecutor = createTestAccountExecutor;
        this.journalCleaningExecutor = journalCleaningExecutor;
        this.activatedJournalFeesOpenBalanceFixExecutor = activatedJournalFeesOpenBalanceFixExecutor;
        this.paymentsReverseExecutor = paymentsReverseExecutor;
    }

    public void changeAccount(boolean test, long originalAccountIdTx, long newAccountIdTx, Long newAccountIdIssuer) throws JobProcessingException {
        moveAccountExecutor.execute(test, originalAccountIdTx, newAccountIdTx, newAccountIdIssuer);
    }

    public void cleanJournals(Set<String> txReferences, int hoursInterval) throws JobProcessingException {
        journalCleaningExecutor.execute(txReferences, hoursInterval);
    }

    public void createTestAccountAsCopyOf(String accountCode, String accountTypeCode) throws JobProcessingException {
        createTestAccountExecutor.execute(accountCode, accountTypeCode);
    }

    public void activatedJournalFeesOpenBalanceFix(String accountCode, boolean test) throws JobProcessingException {
        activatedJournalFeesOpenBalanceFixExecutor.execute(accountCode, test);
    }

    public void reverseAndSubmitPayments(String mode, Long txId, Long newPaymentId, boolean test) throws JobProcessingException {
        paymentsReverseExecutor.execute(mode, txId, newPaymentId, test);
    }
}
