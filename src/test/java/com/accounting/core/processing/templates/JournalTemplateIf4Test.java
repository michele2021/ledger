package com.accounting.core.processing.templates;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.Getter;

import java.util.List;

public class JournalTemplateIf4Test implements JournalTemplateIf {

    @Getter
    public final JournalType journalType;

    @Getter
    public final JournalMaker journalMaker;

    public final Account account;
    public final Amount amount;
    public final TxType txType;
    public final QueueMessage message;
    public final RepositoryHelper repositoryHelper;

    public JournalTemplateIf4Test(JournalMaker journalMaker,
                                  Account clientAccount,
                                  Amount amount,
                                  JournalType journalType,
                                  TxType txType,
                                  QueueMessage message,
                                  RepositoryHelper repositoryHelper
    ) {
        this.journalMaker = journalMaker;
        this.account = clientAccount;
        this.amount = amount;
        this.journalType = journalType;
        this.txType = txType;
        this.message = message;
        this.repositoryHelper = repositoryHelper;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        return null;
    }

    @Override
    public boolean isTestAccount() {
        return account.isTest();
    }
}