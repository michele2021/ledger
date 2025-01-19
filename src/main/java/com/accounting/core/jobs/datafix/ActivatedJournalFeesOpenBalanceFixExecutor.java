package com.accounting.core.jobs.datafix;

import com.accounting.commons.Constants;
import com.accounting.core.db.DataFixDAO;
import com.accounting.core.entities.views.JournalLineView;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

@Slf4j
public class ActivatedJournalFeesOpenBalanceFixExecutor {

    private final Jdbi jdbi;
    private static final String startDate = "2022-10-01";

    @Inject
    public ActivatedJournalFeesOpenBalanceFixExecutor(RepositoryHelper repositoryHelper) {
        this.jdbi = repositoryHelper.getJdbi();
    }

    public void execute(String accountCode, boolean test) throws JobProcessingException {
        try (Handle handle = jdbi.open()) {

            try {
                handle.begin();
                DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);
                dataFixDAO.createJournalsTempTable();
                log.info("Processing journal data fix operation on account {} with starting date: {}", accountCode, startDate);
                List<Long> journalIds = dataFixDAO.selectActivatedJournalsToFix(accountCode, startDate);
                log.info("Selected {} journals to process: {}", journalIds.size(), journalIds);

                for (long journalId : journalIds) {
                    List<JournalLineView> jLines = dataFixDAO.selectJournalLinesByJournalId(journalId);
                    log.info("Processing journal {}. Number of journal lines: {}", journalId, jLines.size());

                    if (test) {
                        log.info("Running in test mode, skipping data updates.");
                        continue;
                    }

                    long feeQuantity = 0;
                    for (JournalLineView jl : jLines) {
                        log.info("Processing journalLine {} with registerTypeCode {} and quantity {}", jl.getJournalLineId(), jl.getRegisterType(), jl.getQuantity());
                        if (jl.getRegisterType().equals(Constants.RegisterType.Fee)) {
                            int n = dataFixDAO.switchJournalLineDebitCredit(jl.getJournalLineId());
                            log.info("'Fee' journal line, switched debit credit. Number of lines updated: {} ", n);
                            continue;
                        }
                        if (jl.getRegisterType().equals(Constants.RegisterType.BankReceived)) {
                            feeQuantity = jl.getQuantity();
                            int n = dataFixDAO.deleteJournalLine(jl.getJournalLineId());
                            log.info("'FeeReceivable' journal line, deleted. Number of lines deleted: {} ", n);
                            continue;
                        }
                        if (jl.getRegisterType().equals(Constants.RegisterType.OpenBalance)) {
                            // we rely on the fact that the journalLines are processed ordered by registerTypeCode
                            if (feeQuantity == 0) {
                                throw new JobProcessingException("feeQuantity could not be determined.");
                            }
                            long quantity = jl.getQuantity() - Math.abs(feeQuantity);
                            int n = dataFixDAO.updateJournalLineQuantity(jl.getJournalLineId(), quantity);
                            log.info("'OpenBalance' journal line, quantity updated. Number of lines updated: {}", n);
                            continue;
                        }
                        log.info("Nothing to do with journalLine");
                    }
                }
                handle.commit();
                log.info("Process completed! Database transaction committed!");
            } catch (Exception e) {
                handle.rollback();
                log.error("Exception while processing the data fix: ", e);
                throw new JobProcessingException("Exception while processing the data fix: ", e);
            }
        }
    }
}