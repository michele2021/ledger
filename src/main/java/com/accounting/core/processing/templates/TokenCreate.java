package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.*;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.accounting.commons.Message.AdditionalData.*;
import static java.lang.String.format;

@Slf4j
public class TokenCreate extends AbstractTemplate {

    private final String pubId;
    private final OffsetDateTime bookingDateTime;

    public TokenCreate(JournalMaker journalMaker,
                       Account account,
                       Amount amount,
                       JournalType journalType,
                       TxType txType,
                       QueueMessage message,
                       RepositoryHelper repositoryHelper) throws AccountingMakerException {
        // message.getUuid() is the "cardTokenId", unique identifier of the card token.
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        throwExceptionIfNull(message.getUuid(), "UUID is mandatory for TOKEN_CREATE");
        pubId = getPublishableIdAdditionalData(account);
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
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
        return buildBaseTx();
    }

    @Override
    public boolean addSpecializedTx() {
        return true;
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    private String getPublishableIdAdditionalData(Account account) throws AccountingMakerException {
        boolean mandatory = false;
        return mandatory ? getAdditionalData(publishableId).getAsString() : getAdditionalData(publishableId, JsonElement::getAsString);
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) {
        List<SpecializedTx> ret = new ArrayList<>();
        String lr = getAdditionalData(activatingLocationRef, JsonElement::getAsString); // we define "locationRef" as the "activatingLocationRef" passed in the TOKEN_CREATE message.
        String oiId = getAdditionalData(orderItemId, JsonElement::getAsString);
        // TODO oId should possibly be configured as mandatory in the JSON Schema
        Long oId = getAdditionalData(orderId, JsonElement::getAsLong);
        String oRef = getAdditionalData(orderRef, JsonElement::getAsString);
        String oType = getAdditionalData(orderType, JsonElement::getAsString);
        String clientMetadata = getAdditionalData(clientTokenMetadata, JsonElement::getAsString);
        String client = getAdditionalData(Message.AdditionalData.client, JsonElement::getAsString);

        SpecializedTx token = CCToken.builder()
                .txId(txId)
                .publishableId(pubId)
                .locationRef(lr)
                .orderItemId(oiId)
                .orderId(oId)
                .clientMetadata(clientMetadata)
                .client(client)
                .type(Constants.TokenType.POSA)
                .build();
        ret.add(token);
        if (oId != null) {
            SpecializedTx order = Order.builder()
                    .orderId(oId)
                    .orderRef(oRef)
                    .orderType(oType)
                    .build();
            ret.add(order);
        }
        return ret;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();
        long accountId = account.getAccountId();

        // Debit/Credit 1
        addDebitLine(journalId, journalLines, Constants.RegisterType.TokenCreated, accountId, amount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.Clearing, accountId, amount); // was physical token purchase

        return journalLines;
    }

}
