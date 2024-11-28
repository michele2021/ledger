package com.accounting.core.jobs.datafix;

import com.accounting.core.db.DataFixDAO;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.Set;

@Slf4j
public class JournalCleaningExecutor {

    private final Jdbi jdbi;

    @Inject
    public JournalCleaningExecutor(RepositoryHelper repositoryHelper) {
        this.jdbi = repositoryHelper.getJdbi();
    }

    public void execute(Set<String> txReferences, int hoursInterval) throws JobProcessingException {
        try (Handle handle = jdbi.open()) {
            DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);
            dataFixDAO.createJournalsTempTable();
            for(String txReference : txReferences) {
                log.info("Processing journal cleaning operation for txReference: {}", txReference);
                int n = dataFixDAO.selectJournalsToDelete(txReference, hoursInterval);
                log.info("Selected {} journals to be deleted for txReference {}.", n, txReference);
                int[] ret = dataFixDAO.deleteJournals();
                log.info("JournalLines records deleted: {}", ret[0]);
                log.info("JournalTx records deleted: {}", ret[1]);
                log.info("Journal records deleted: {}", ret[2]);
                dataFixDAO.truncateTJournals();
            }
        } catch (Exception e) {
            throw new JobProcessingException("Exception while processing the journal cleaning operation: ", e);
        }
    }
}