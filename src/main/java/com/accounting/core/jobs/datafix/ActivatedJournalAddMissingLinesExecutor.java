package com.accounting.core.jobs.datafix;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.db.DataFixDAO;
import com.accounting.core.entities.views.JournalTxView;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

@Slf4j
public class ActivatedJournalAddMissingLinesExecutor {

    private final Jdbi jdbi;
    private final JournalMaker journalMaker;
    private final RegisterTypeRepoIf registerTypeRepo;
    private final AccountRepoIf accountRepo;
    private static final String startDate = "2022-10-01";

    @Inject
    public ActivatedJournalAddMissingLinesExecutor(JournalMaker journalMaker, RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.registerTypeRepo = repositoryHelper.getRegisterTypeRepo();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.jdbi = repositoryHelper.getJdbi();
    }

    public void execute(String accountCode, String issuerAccountCode, boolean test) throws JobProcessingException {

        int tot = 0;
        long quantity = 0;
        // N.B. The issuer is hardcoded
        String iac = issuerAccountCode == null ? "Monavate" : issuerAccountCode;
        Account issuerAccount = accountRepo.getByCodeAndType(iac, Constants.AccountType.Issuer.toString());
        if(issuerAccount == null) {
            throw new JobProcessingException("Issuer account with code " + iac + " not found.");
        }
        try (Handle handle = jdbi.open()) {
            try {
                handle.begin();
                DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);
                dataFixDAO.createJournalsTempTable();
                log.info("Processing Activated journal data fix operation (adding missing lines) on account {} with starting date: {}", accountCode, startDate);
                List<JournalTxView> journalTxViews = dataFixDAO.selectActivatedJournalsWithMissingJournalLinesToFix(accountCode, startDate);
                log.info("Selected {} journals to process.", journalTxViews.size());

                for (JournalTxView j : journalTxViews) {
                    long accountId = j.getAccountId();
                    long cardsPayablePendingBatchId = journalMaker.getOrCreateAvailableBatch(getRegisterTypeId(Constants.RegisterType.Payable), issuerAccount.getAccountId());
                    long issuerPrefundAccountBatchId = journalMaker.getOrCreateAvailableBatch(getRegisterTypeId(Constants.RegisterType.Fee), issuerAccount.getAccountId());
                    long openBalanceBatchId = journalMaker.getOrCreateAvailableBatch(getRegisterTypeId(Constants.RegisterType.OpenBalance), accountId);
                    long tokenReceivableBatchId = journalMaker.getOrCreateAvailableBatch(getRegisterTypeId(Constants.RegisterType.TokenReceivable), accountId);
                    log.info("Going to create journal lines for accountId {}, journalId {}, currencyId {}, quantity {}, registers {}, {}, {}, {}",
                            accountId,
                            j.getJournalId(),
                            j.getCurrencyId(),
                            j.getQuantity(),
                            cardsPayablePendingBatchId,
                            issuerPrefundAccountBatchId,
                            openBalanceBatchId,
                            tokenReceivableBatchId);
                    quantity += j.getQuantity();
                    if(test) {
                        continue;
                    }
                    int n = dataFixDAO.insertJournalLine(j.getJournalId(), cardsPayablePendingBatchId, j.getCurrencyId(), j.getQuantity());
                    n += dataFixDAO.insertJournalLine(j.getJournalId(), issuerPrefundAccountBatchId, j.getCurrencyId(), -j.getQuantity());
                    n += dataFixDAO.insertJournalLine(j.getJournalId(), openBalanceBatchId, j.getCurrencyId(), -j.getQuantity());
                    n += dataFixDAO.insertJournalLine(j.getJournalId(), tokenReceivableBatchId, j.getCurrencyId(), j.getQuantity());
                    tot += n;
                    log.info("Journal {}. Added {} journal lines.", j.getJournalId(), n);
                }
                handle.commit();
                log.info("Process completed! Database transaction committed!");
                log.info("Total journal lines added/to be added: {}. Total OpenBalance quantity added/to be added: {}", tot, quantity);
            } catch (Exception e) {
                handle.rollback();
                log.error("Exception while processing the data fix: ", e);
                throw new JobProcessingException("Exception while processing the data fix: ", e);
            }
        }
    }

    private int getRegisterTypeId(Constants.RegisterType registerType) {
        return registerTypeRepo.get(registerType.toString()).getRegisterTypeId();
    }
}
