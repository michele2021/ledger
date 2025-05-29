package com.accounting.core.processing;

import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.Batch;
import com.accounting.core.entities.JournalDb;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.templates.JournalTemplateIf;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

import static java.lang.String.format;

@Slf4j
@Singleton
public class JournalMaker {

    private final JournalDAO dao;

    @Inject
    public JournalMaker(JournalDAO dao) {
        this.dao = dao;
    }

    /*
     * The current implementation allows for incomplete journals to be stored in the database.
     * For example if the creation of the journalLines fails, the previous tables would remain populated.
     * For now this is accepted, we consider that journals that do not have journal lines should be ignored.
     */
    public JournalDb createJournal(JournalTemplateIf journalTemplateImpl) throws AccountingMakerException {

        var journalType = journalTemplateImpl.getJournalType();
        int journalTypeId = journalType.getJournalTypeId();
        boolean isTestAccount = journalTemplateImpl.isTestAccount();

        log.info("createJournal with jt code: {}, journalTypeId: {}, isTestAccount: {}" , journalType.getCode(), journalTypeId, isTestAccount);

        // Journal lines, initially created with a fictitious journalId in order to allow validation as a first step
        // (to avoid creating a journal in the db if the validation fails)
        var journalLines = journalTemplateImpl.buildJournalLines(1);
        validateJL(journalLines);

        // journal
        long journalId = dao.createJournalAt(journalTypeId, journalTemplateImpl.getJournalBookingDate());

        // tx (create a new tx or add a journal to an existing one)
        boolean addNewTx = journalTemplateImpl.addNewTx();
        boolean addSpecializedTx = journalTemplateImpl.addSpecializedTx();
        var linkedTxId = journalTemplateImpl.linkTx();

        if(addSpecializedTx && !addNewTx) {
            throw new AccountingMakerException("A specialized tx can only be added if there is a new tx");
        }
        if (!addNewTx) {
            // TODO this should probably be catched and throw an AccountingMakerException
            Objects.requireNonNull(linkedTxId,
                    "txId cannot be null. Either a new tx is created or a reference to an existing one is added.");
        }

        long mainTxId = 0;
        if (addNewTx) {
            Tx tx = journalTemplateImpl.buildTx();
            mainTxId = dao.createTx(tx);
        } else {
            journalTemplateImpl.updateSpecializedTx();
        }
        if (addSpecializedTx) {
            var specializedTxs = journalTemplateImpl.buildSpecializedTxs(mainTxId);
            for (var tx : specializedTxs) {
                tx.save(dao);
            }
        }

        // journalTx
        dao.createJournalTx(journalId, mainTxId);
        if (linkedTxId != null) {
            dao.createJournalTx(journalId, linkedTxId);
        }

        // journalLines
        journalLines.forEach(jl -> jl.setJournalId(journalId));
        dao.createJournalLinesBulk(journalLines);

        return JournalDb.builder()
                .journalId(journalId)
                .txId(mainTxId)
                .build();
    }

    private long createBatch(int registerTypeId, long accountId) {
        var periodBeginDate = LocalDate.now().withDayOfMonth(1);
        var batch = Batch.builder()
                .accountId(accountId)
                .registerTypeId(registerTypeId)
                .batchNumber(1) // TODO temp hardcoded
                .periodBeginDate(periodBeginDate)
                .build();

        return dao.createBatch(batch);
    }

    private Batch getAvailableBatch(int registerTypeId, long accountId) {
        return dao.getAvailableBatch(registerTypeId, accountId);
    }

    public long getOrCreateAvailableBatch(int registerTypeId, long accountId) {
        var batch = getAvailableBatch(registerTypeId, accountId);
        long batchId;
        if (batch == null) {
            batchId = createBatch(registerTypeId, accountId);
            log.info("Batch for registerTypeId: {} and accountId: {} not found, created new batchId: {}", registerTypeId, accountId, batchId);
        } else {
            batchId = batch.getBatchId();
        }
        return batchId;
    }

    private void validateJL(List<JournalLine> jls) throws AccountingMakerException {
        Map<Integer, Long> currencyBalance = new HashMap<>();
        for (JournalLine jl : jls) {
            currencyBalance.merge(jl.getCurrencyId(), jl.getQuantity(), Long::sum);
        }
        Optional<Map.Entry<Integer, Long>> entry = currencyBalance.entrySet().stream().filter(e -> e.getValue() != 0).findFirst();
        if (entry.isPresent()) {
            int currencyId = entry.get().getKey();
            long value = entry.get().getValue();
            throw new AccountingMakerException(format("Journal Lines are not balanced, currencyId: %s value: %s", currencyId, value));
        }
    }
}
