package com.accounting.core.processing;

import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.*;
import com.accounting.core.processing.templates.JournalTemplateIf;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Slf4j
@Singleton
public class JournalMaker {

    private final JournalDAO dao;
    private final LocalDate now = LocalDate.now();
    private final LocalDate PERIOD_BEGIN_DEFAULT = LocalDate.of(now.getYear(), now.getMonth(), 1);

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

        JournalType journalType = journalTemplateImpl.getJournalType();
        int journalTypeId = journalType.getJournalTypeId();
        boolean isTestAccount = journalTemplateImpl.isTestAccount();

        log.info("createJournal with jt code: {}, journalTypeId: {}, isTestAccount: {}" , journalType.getCode(), journalTypeId, isTestAccount);

        // Journal lines, initially created with a fictitious journalId in order to allow validation as a first step
        // (to avoid creating a journal in the db if the validation fails)
        List<JournalLine> journalLines = journalTemplateImpl.buildJournalLines(1);
        validateJL(journalLines);

        // journal
        long journalId = dao.createJournalAt(journalTypeId, journalTemplateImpl.getJournalBookingDate());

        // tx (create a new tx or add a journal to an existing one)
        long txId;
        if (journalTemplateImpl.addNewTx()) {
            Tx tx = journalTemplateImpl.buildTx();
            txId = dao.createTx(tx);
        } else {
            txId = journalTemplateImpl.getTxId();
            journalTemplateImpl.updateSpecializedTx();
        }
        if (journalTemplateImpl.addSpecializedTx()) {
            List<SpecializedTx> specializedTxs = journalTemplateImpl.buildSpecializedTxs(txId);
            for(SpecializedTx specializedTx : specializedTxs) {
                if (specializedTx.isOrder()) {
                    // Order, strictly speaking, is not a specialized Tx, since no corresponding tx is created/linked.
                    // TODO possibly rename SpecializedTx to something else, since a corresponding tx is no longer always required.
                    dao.createOrder((Order) specializedTx);
                } else if (specializedTx.isCCToken()) {
                    dao.createTokenTx((CCToken) specializedTx);
                }  else if (specializedTx.isCard()) {
                    dao.createCardTx((Card) specializedTx);
                }else {
                    throw new AccountingMakerException(format("Specialized Tx is of an unrecognized type, for txId: %s", txId));
                }
            }
        }

        // journalTx
        dao.createJournalTx(journalId, txId);

        // journalLines
        journalLines.forEach(jl -> jl.setJournalId(journalId));
        dao.createJournalLinesBulk(journalLines);

        return JournalDb.builder()
                .journalId(journalId)
                .txId(txId)
                .build();
    }

    private long createBatch(int registerTypeId, long accountId) {
        Batch batch = Batch.builder()
                .accountId(accountId)
                .registerTypeId(registerTypeId)
                .batchNumber(1) // TODO temp hardcoded
                .periodBeginDate(PERIOD_BEGIN_DEFAULT)
                .build();

        return dao.createBatch(batch);
    }

    private Batch getAvailableBatch(int registerTypeId, long accountId) {
        return dao.getAvailableBatch(registerTypeId, accountId);
    }

    public long getOrCreateAvailableBatch(int registerTypeId, long accountId) {
        Batch batch = getAvailableBatch(registerTypeId, accountId);
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
