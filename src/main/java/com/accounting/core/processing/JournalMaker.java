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
     * For example, if the creation of the journalLines fails, the previous tables would remain populated.
     * For now this is accepted; we consider that journals that do not have journal lines should be ignored.
     */
    public JournalDb createJournal(JournalTemplateIf template) throws AccountingMakerException {
        var journalType = template.getJournalType();
        int journalTypeId = journalType.getJournalTypeId();

        log.info("createJournal with jt code: {}, journalTypeId: {}", journalType.getCode(), journalTypeId);

        // Journal lines, initially created with a fictitious journalId in order to allow validation as a first step
        // (to avoid creating a journal in the db if the validation fails)
        var journalLines = template.buildJournalLines(1);
        validateJL(journalLines);

        // journal
        long journalId = dao.createJournalWithInfoAt(journalTypeId,
                template.getJournalBookingDate(),
                template.journalInfo());

        // tx (create a new tx or add a journal to an existing one)
        boolean addNewTx = template.addNewTx();
        boolean addSpecializedTx = template.addSpecializedTx();
        var linkedTxs = Optional.ofNullable(template.linkTxs())
                .orElse(Collections.emptyList());

        if (addSpecializedTx && !addNewTx) {
            throw new AccountingMakerException("A specialized tx can only be added if there is a new tx");
        }
        if (!addNewTx && linkedTxs.isEmpty()) {
            throw new AccountingMakerException("At least a linked tx has to be specified");
        }

        long mainTxId = 0;
        if (addNewTx) {
            Tx tx = template.buildTx();
            mainTxId = dao.createTx(tx);
        } else {
            // by default, it does nothing.
            template.updateSpecializedTx();
        }
        if (addSpecializedTx) {
            var stxs = template.buildSpecializedTxs(mainTxId);
            for (var tx : stxs) {
                tx.save(dao);
            }
        }

        // journalTx
        var txIdsToLink = new ArrayList<Long>();
        if(addNewTx) {
            txIdsToLink.add(mainTxId);
        }
        txIdsToLink.addAll(linkedTxs);
        dao.createJournalTxBulk(journalId, txIdsToLink);

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
        var currencyBalance = new HashMap<Integer, Long>();
        for (JournalLine jl : jls) {
            currencyBalance.merge(jl.getCurrencyId(), jl.getQuantity(), Long::sum);
        }
        Optional<Map.Entry<Integer, Long>> entry = currencyBalance.entrySet()
                .stream()
                .filter(e -> e.getValue() != 0)
                .findFirst();
        if (entry.isPresent()) {
            int currencyId = entry.get().getKey();
            long value = entry.get().getValue();
            throw new AccountingMakerException(format("Journal Lines are not balanced, currencyId: %s value: %s", currencyId, value));
        }
    }
}
