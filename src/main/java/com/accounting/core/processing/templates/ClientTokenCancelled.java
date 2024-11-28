package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.RegisterType;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClientTokenCancelled implements JournalTemplateIf {


    @Getter
    private final JournalType journalType;

    @Getter
    private final JournalMaker journalMaker;

    private final Tx tx;
    private final RegisterTypeRepoIf registerTypeRepo;
    private final Account account;

    public ClientTokenCancelled(Account account,
                                Tx tx,
                                JournalType journalType,
                                JournalMaker journalMaker,
                                RepositoryHelper repositoryHelper) {
        this.tx = tx;
        this.account = account;
        this.journalType = journalType;
        this.journalMaker = journalMaker;
        this.registerTypeRepo = repositoryHelper.getRegisterTypeRepo();
    }

    public boolean addNewTx() {
        return false;
    }

    public long getTxId() {
        return tx.getTxId();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();
        Amount amount = tx.getAmount();
        // Debit (this register is the balance of the issued tokens, probably is going to have a different name)
        RegisterType rtDebit = registerTypeRepo.get(Constants.RegisterType.TokenCreated.toString());
        long batchId = journalMaker.getOrCreateAvailableBatch(rtDebit.getRegisterTypeId(), tx.getAccountId());
        JournalLine jl = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .debit(amount).build();
        journalLines.add(jl);

        // Credit
        RegisterType rtCredit = registerTypeRepo.get(Constants.RegisterType.TokenCancelled.toString());
        batchId = journalMaker.getOrCreateAvailableBatch(rtCredit.getRegisterTypeId(), tx.getAccountId());
        jl = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .credit(amount).build();
        journalLines.add(jl);
        return journalLines;
    }

    @Override
    public boolean isTestAccount() {
        return account.isTest();
    }
}
