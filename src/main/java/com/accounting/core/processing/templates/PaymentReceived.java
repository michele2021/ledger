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
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class PaymentReceived extends AbstractTemplate {
    private Tx oriTx;
    private final String oriTxReference;

    public PaymentReceived(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, account, message.uuid(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        this.oriTxReference = message.txReference();
        validateIfTxReferencePresent();
    }

    @Override
    public void process() throws AccountingMakerException {
        oriTx = journalDAO.getTxByReference(oriTxReference);
        throwExceptionIfNull(oriTx, format("Tx not found with oriTxReference '%s'.", oriTxReference));

        journalMaker.createJournal(this);
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx();
    }

    @Override
    public Long linkTx() {
        return oriTx.getTxId();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        var journalLines = new ArrayList<JournalLine>();

        // Debit
        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.Clearing, accountId, amount);

        // Credit
        addCreditLine(journalId, journalLines, Constants.RegisterType.Receivable, accountId, amount);
        return journalLines;
    }
}
