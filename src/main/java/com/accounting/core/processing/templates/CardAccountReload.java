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
import com.accounting.core.entities.Tx;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static java.lang.String.format;

public class CardAccountReload extends AbstractTemplate {
    private Tx oriTx;
    private final Account issuerAccount;
    private final OffsetDateTime bookingDateTime;
    private final String prepaidCardExternalId;

    public CardAccountReload(JournalMaker journalMaker,
                             Account account,
                             Amount amount,
                             JournalType journalType,
                             TxType txType,
                             QueueMessage message,
                             RepositoryHelper repositoryHelper) throws AccountingMakerException, AccountingTemplateException {
        // message.getUuid() is the identifier of the "reload" entity in productsDB(funding_transactions->referenceNumber).
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        prepaidCardExternalId = getAdditionalData(Message.AdditionalData.prepaidCardExternalId).getAsString();
        validateBookingDateInThePast(bookingDateTime);
        issuerAccount = getIssuerAccount();
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = journalDAO.getTxStatusByReference(prepaidCardExternalId);
        throwExceptionIfNull(tx, format("Tx not found for reload with prepaid card id: %s", prepaidCardExternalId));

        oriTx = tx;
        journalMaker.createJournal(this);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> jl = new ArrayList<>();
        long accountId = account.getAccountId();
        long issuerAccountId = issuerAccount.getAccountId();

        // Credit/Debit 1
        addCreditLine(journalId, jl, Constants.RegisterType.OpenBalance, accountId, amount);
        addDebitLine(journalId, jl, Constants.RegisterType.CardAccountBalance, oriTx.getAccountId(), amount);

        // Credit/Debit 2
        addCreditLine(journalId, jl, Constants.RegisterType.IssuerPrefundAccount, issuerAccountId, amount);
        addDebitLine(journalId, jl, Constants.RegisterType.CardsPayable, issuerAccountId, amount);

        return jl;
    }

    @Override
    public long getTxId() {
        return oriTx.getTxId();
    }

    @Override
    public boolean addNewTx() {
        return false;
    }
}