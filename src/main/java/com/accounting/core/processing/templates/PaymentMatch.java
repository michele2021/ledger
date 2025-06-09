package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;

public class PaymentMatch extends AbstractTemplate {
    private final long orderTxId;
    private final long paymentTxId;
    private Amount paymentAmount;

    public PaymentMatch(JournalMaker journalMaker,
                        Account account,
                        Amount amount,
                        JournalType journalType,
                        TxType txType,
                        QueueMessage message,
                        RepositoryHelper repositoryHelper) throws AccountingMakerException {
        super(txType, account, message.uuid(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        orderTxId = getMandatoryAdditionalData(Message.AdditionalData.orderTxId).getAsLong();
        paymentTxId = getMandatoryAdditionalData(Message.AdditionalData.paymentTxId).getAsLong();
    }

    @Override
    public void process() throws AccountingMakerException {
        var payment = journalDAO.getTxByIdAndType(paymentTxId, Constants.TxType.Payment);
        if(payment == null) {
            throw new AccountingMakerException("payment not found with id " + paymentTxId);
        }
        // TODO possibly we should validate that also the txId is valid
        paymentAmount = payment.getAmount();
        journalMaker.createJournal(this);
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public List<Long> linkTxs() {
        return List.of(orderTxId, paymentTxId);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        var journalLines = new ArrayList<JournalLine>();

        long accountId = account.getAccountId();
        addCreditLine(journalId, journalLines, Constants.RegisterType.Receivable, accountId, paymentAmount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.UnappliedCash, accountId, paymentAmount);
        return journalLines;
    }
}