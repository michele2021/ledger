package com.accounting.core.jobs.datafix;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.core.db.DataFixDAO;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.entities.TxJournalLineBatch;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * This class is able to perform 2 kinds of data fix:
 * 1) if newAccountIdTx is a Client "test" account --> move the tokens to the test account.
 * Journal lines with batch associated to a Client account --> new/existing batch associated to newAccountIdTx
 * Journal lines with batch associated to an Issuer account --> new/existing batch associated to newIssuerAccountId or corresponding "_TEST" issuer account
 * Journal lines with batch associated to a Business Entity/BankAccount --> new/existing batch associated to corresponding "_TEST" account
 * 2) if newAccountIdTx is NOT a "test" account --> move the tokens to the new Client account.
 * Journal lines with batch associated to a Client account --> new/existing batch associated to newAccountIdTx
 * All the other journal lines --> no change in the batch
 **/
@Slf4j
public class MoveAccountExecutor {

    private final JournalMaker journalMaker;
    private final AccountRepoIf accountRepo;
    private final Jdbi jdbi;
    private boolean toTestAccount; // whether the destination account is a "test" account.

    @Inject
    public MoveAccountExecutor(JournalMaker journalMaker, RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.jdbi = repositoryHelper.getJdbi();
    }

    public void execute(boolean test, long originalAccountIdTx, long newAccountIdTx, Long newIssuerAccountId) throws JobProcessingException {
        // check the new accounts exists
        checkAccountExists(newAccountIdTx, List.of(Constants.AccountType.Client.toString(), Constants.AccountType.Distributor.toString()));
        if (nonNull(newIssuerAccountId)) {
            checkAccountExists(newIssuerAccountId, List.of(Constants.AccountType.Issuer.toString()));
        }
        // start processing
        try (Handle handle = jdbi.open()) {
            process(handle, test, originalAccountIdTx, newAccountIdTx, newIssuerAccountId);
        }
    }

    private void process(Handle handle, boolean test, long originalAccountIdTx, long newAccountIdTx, Long newIssuerAccountId) throws JobProcessingException {
        try {
            handle.begin();
            log.info("Database transaction begin.");
            DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);

            // fetch Journal lines related to the tokens in txFix.
            List<TxJournalLineBatch> lines = dataFixDAO.getJournalLineBatchToUpdate(originalAccountIdTx);
            log.info("Retrieved {} journal lines to update.", lines.size());

            if(test) {
                log.info("Journal lines: {}", lines);
                log.info("Running in test mode, skipping any data update.");
                handle.rollback();
                return;
            }
            // update tx table (based on records in txFix).
            int numRows = dataFixDAO.updateTx(originalAccountIdTx, newAccountIdTx);
            log.info("Updated {} txs with new accountId: {}", numRows, newAccountIdTx);

            // update the batchId on the journalLine (based on records in txFix)
            for (TxJournalLineBatch line : lines) {
                // switch to test account
                long newAccountIdJL = getNewBatchAccountId(newAccountIdTx, newIssuerAccountId, line);
                if(newAccountIdJL == line.getAccountId()) {
                    log.info("Processing journal line {} on account {}({}). No need to change the batch --> skipping the line.", line.getJournalLineId(), line.getAccountCode(), line.getAccountTypeCode());
                    continue;
                }
                long batchId = journalMaker.getOrCreateAvailableBatch(line.getRegisterTypeId(), newAccountIdJL);
                log.info("Update journal line {} on account {}({}). New batchId: {} with new accountId: {}", line.getJournalLineId(), line.getAccountCode(), line.getAccountTypeCode(), batchId, newAccountIdJL);
                dataFixDAO.updateJournalLineBatch(line.getJournalLineId(), batchId);
            }

            // empty the table txFix containing the tokens that have been updated.
            dataFixDAO.truncateTxFix();

            handle.commit();
            log.info("Database transaction committed");
        } catch (Exception e) {
            handle.rollback();
            log.info("Database transaction rolled back");
            throw new JobProcessingException("Exception while processing the account move operation: " + e, e);
        }
    }

    private long getNewBatchAccountId(long newAccountIdTx, Long newIssuerAccountId, TxJournalLineBatch line) throws JobProcessingException {
        long newAccountIdJL;
        String accountCode = line.getAccountCode();
        String accountTypeCode = line.getAccountTypeCode();

        if (Constants.AccountType.Client.toString().equals(accountTypeCode) || Constants.AccountType.Distributor.toString().equals(accountTypeCode)) {
            newAccountIdJL = newAccountIdTx;
        } else {
            if (toTestAccount) {
                if (Constants.AccountType.Issuer.toString().equals(accountTypeCode)) {
                    // if the new issuer account is specified we use it, otherwise we search for the corresponding _TEST account.
                    if (isNull(newIssuerAccountId)) {
                        Account testAccountIssuer = accountRepo.getByCodeAndType(accountCode + "_TEST", Constants.AccountType.Issuer.toString());
                        if (isNull(testAccountIssuer)) {
                            throw new JobProcessingException("New issuer account not specified and corresponding _TEST account not found, while processing journal line: " + line.getJournalLineId() + ". Account: " + accountCode + " accountTypeCode: " + accountTypeCode);
                        }
                        newAccountIdJL = testAccountIssuer.getID();
                    } else {
                        newAccountIdJL = newIssuerAccountId;
                    }
                } else if (Constants.AccountType.BusinessEntity.toString().equals(accountTypeCode) || Constants.AccountType.BankAccount.toString().equals(accountTypeCode)) {
                    Account testAccount = accountRepo.getByCodeAndType(accountCode + "_TEST", accountTypeCode);
                    if (isNull(testAccount)) {
                        throw new JobProcessingException("Corresponding _TEST account not found, while processing journal line: " + line.getJournalLineId() + ". Account: " + accountCode + " accountTypeCode: " + accountTypeCode);
                    }
                    newAccountIdJL = testAccount.getID();
                } else {
                    throw new JobProcessingException("Found account of unexpected type while processing journal line: " + line.getJournalLineId() + ". Account: " + accountCode + " accountTypeCode: " + accountTypeCode);
                }
            } else {
                newAccountIdJL = line.getAccountId();
            }
        }
        return newAccountIdJL;
    }

    private void checkAccountExists(long accountId, List<String> allowedAccountTypeCodes) throws JobProcessingException {
        Account account = accountRepo.get(accountId);
        if (account == null) {
            throw new JobProcessingException("New account not found for accountId: " + accountId);
        }
        toTestAccount = account.isTest();
        // we expect the new account to be a Client or Distributor.
        if (!allowedAccountTypeCodes.contains(account.getAccountTypeCode())) {
            throw new JobProcessingException("The new account is of type " + account.getAccountTypeCode() + ", but the only allowed account types are: " + allowedAccountTypeCodes);
        }
    }
}