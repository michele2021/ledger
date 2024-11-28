package com.accounting.core.processing.templates;

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

import java.time.OffsetDateTime;
import java.util.List;

public class AccountAddFundsCorrection extends AccountAddFunds {

    public AccountAddFundsCorrection(JournalMaker journalMaker, Account account, Amount amount, JournalType journalType,
                                     TxType txType, QueueMessage message, RepositoryHelper repositoryHelper) throws AccountingMakerException {
        super(journalMaker, account, amount, journalType, txType, message, repositoryHelper);
    }

    @Override
    public Tx buildTx() {
        Tx tx = super.buildTx();
        // we want a negative amount for Payment corrections
        Amount amount = Amount.of(-tx.getAmount().getQuantity(), tx.getAmount().getCurrencyId());
        tx.setAmount(amount);

        return tx;
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    // we do the opposite (Credit/Debit) bookings of "PAYMENT_RECEIVED"
    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        return buildJournalLinesCorrection(journalId);
    }
}