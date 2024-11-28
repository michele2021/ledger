package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

public class ClientFeePayout extends AbstractTemplate {

    public ClientFeePayout(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) {
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
    }

    @Override
    public Tx buildTx() {
        if (isNull(this.txReference)) {
            String uuid = UUID.randomUUID().toString();
            this.txReference = txType.getCode().substring(0, 3).toUpperCase().concat("_").concat(uuid);
        }
        return buildBaseTx();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();

        long accountId = account.getAccountId();
        // Credit/Debit
        addDebitLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, amount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.Fee, accountId, amount);

        return journalLines;
    }

}