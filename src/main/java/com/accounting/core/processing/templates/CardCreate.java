package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.entities.Card;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.SpecializedTx;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static java.lang.String.format;

@Slf4j
public class CardCreate extends AbstractTemplate {

    private final String prepaidCardExternalId;
    private final OffsetDateTime bookingDateTime;
    private final Account clientAccount;
    private final long tokenTxId;

    public CardCreate(JournalMaker journalMaker,
                      Account cardAccount,
                      Account clientAccount,
                      Amount amount,
                      JournalType journalType,
                      TxType txType,
                      long tokenTxId,
                      JsonObject additionalData,
                      RepositoryHelper repositoryHelper) throws AccountingMakerException {
        // we use as txReference the prepaidCardExternalId
        super(txType, cardAccount, cardAccount.getCode(), journalType, journalMaker, amount, additionalData, repositoryHelper);
        this.clientAccount = clientAccount;
        this.tokenTxId = tokenTxId;
        this.prepaidCardExternalId = getAdditionalData(Message.AdditionalData.prepaidCardExternalId).getAsString();
        this.bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        validateBookingDateInThePast(bookingDateTime);
    }

    @Override
    public void process() throws AccountingMakerException {
        Tx tx = journalDAO.getTxByReference(txReference);
        throwExceptionIf(Objects.nonNull(tx), format("Tx with txReference '%s' is already existent.", txReference));

        journalMaker.createJournal(this);
    }

    @Override
    public Tx buildTx() {
        Tx baseTx = buildBaseTx();
        baseTx.setParentTxId(tokenTxId);
        return baseTx;
    }

    @Override
    public boolean addSpecializedTx() {
        return true;
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) {
        List<SpecializedTx> ret = new ArrayList<>();
        SpecializedTx card = Card.builder()
                .txId(txId)
                .prepaidCardExternalId(prepaidCardExternalId)
                .build();
        ret.add(card);

        return ret;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();
        long cardAccountId = account.getAccountId();

        // Debit/Credit 1
        addDebitLine(journalId, journalLines, Constants.RegisterType.CardAccountBalance, cardAccountId, amount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.CardAccountCreated, clientAccount.getAccountId(), amount);

        return journalLines;
    }
}