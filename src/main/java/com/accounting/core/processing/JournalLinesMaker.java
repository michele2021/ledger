package com.accounting.core.processing;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.templates.JournalTemplateIf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class JournalLinesMaker implements JournalTemplateIf {

    @Getter
    protected final JournalMaker journalMaker;
    protected final RegisterTypeRepoIf registerTypeRepo;

    protected final void addDebitLine(long journalId, List<JournalLine> journalLines, Constants.RegisterType registerType, long accountId, Amount amount) {
        addDebitLine(journalId, journalLines, getRegisterTypeId(registerType), accountId, amount);
    }

    protected final void addDebitLine(long journalId, List<JournalLine> journalLines, int registerTypeId, long accountId, Amount amount) {
        long batchId = journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId);
        journalLines.add(buildDebitLine(journalId, batchId, amount));
    }

    protected final void addCreditLine(long journalId, List<JournalLine> journalLines, Constants.RegisterType registerType, long accountId, Amount amount) {
        addCreditLine(journalId, journalLines, getRegisterTypeId(registerType), accountId, amount);
    }

    protected final void addCreditLine(long journalId, List<JournalLine> journalLines, int registerTypeId, long accountId, Amount amount) {
        long batchId = journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId);
        journalLines.add(buildCreditLine(journalId, batchId, amount));
    }

    protected int getRegisterTypeId(Constants.RegisterType registerType) {
        return registerTypeRepo.get(registerType.toString()).getRegisterTypeId();
    }

    private JournalLine buildCreditLine(long journalId, long batchId, Amount amount) {
        return JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .credit(amount)
                .build();
    }

    private JournalLine buildDebitLine(long journalId, long batchId, Amount amount) {
        return JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .debit(amount)
                .build();
    }
}
