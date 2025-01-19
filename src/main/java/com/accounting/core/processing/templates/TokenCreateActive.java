package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.*;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.accounting.commons.Message.AdditionalData.*;
import static java.lang.String.format;

public class TokenCreateActive extends AbstractTemplate {

    private final Account issuerAccount;
    private final String pubId;
    private final OffsetDateTime bookingDateTime;

    public TokenCreateActive(JournalMaker journalMaker,
                             Account account,
                             Amount amount,
                             JournalType journalType,
                             TxType txType,
                             QueueMessage message,
                             RepositoryHelper repositoryHelper) throws AccountingMakerException {
        // message.getUuid() is the "cardTokenId", unique identifier of the card token.
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        throwExceptionIfNull(message.getUuid(), "UUID is mandatory for TOKEN_CREATE_ACTIVE");
        // throws exception if not found
        issuerAccount = getIssuerAccount();
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
        boolean mandatory = (Constants.ClientAccount.EPAY_IT.toString().equals(account.getAccountCode()));
        return mandatory ? getAdditionalData(publishableId).getAsString() : getAdditionalData(publishableId, JsonElement::getAsString);
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) throws AccountingMakerException {
        List<SpecializedTx> ret = new ArrayList<>();

        LocalDate expiryLD = LocalDate.parse(getAdditionalData(expiryDate).getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
        String alr = getAdditionalData(activatingLocationRef, JsonElement::getAsString);
        String atz = getAdditionalData(activationTimezone, JsonElement::getAsString);
        String att = getAdditionalData(activationTimestamp, JsonElement::getAsString);
        OffsetDateTime ats = null;
        if(Objects.nonNull(att) && Objects.nonNull(atz)) {
            Instant instant = Instant.parse(att);
            ats = OffsetDateTime.ofInstant(instant, ZoneId.of(atz));
        }
        String oiId = getAdditionalData(orderItemId, JsonElement::getAsString);
        Long oId = getAdditionalData(orderId, JsonElement::getAsLong);
        String oRef = getAdditionalData(orderRef, JsonElement::getAsString);
        String oType = getAdditionalData(orderType, JsonElement::getAsString);
        String clientMetadata = getAdditionalData(clientTokenMetadata, JsonElement::getAsString);
        String client = getAdditionalData(Message.AdditionalData.client, JsonElement::getAsString);

        SpecializedTx token = CCToken.builder()
                .txId(txId)
                .publishableId(pubId)
                .expiryDate(expiryLD)
                .activatingLocationRef(alr)
                .activationTimestamp(ats)
                .activationTimezone(atz)
                .orderItemId(oiId)
                .orderId(oId)
                .clientMetadata(clientMetadata)
                .client(client)
                .type(Constants.TokenType.PoR)
                .build();
        ret.add(token);

        if(oId != null) {
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
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();
        long feeQuantity = 0;

        long accountId = account.getAccountId();
        long issuerAccountId = issuerAccount.getAccountId();

        // Debit/Credit 1
        addDebitLine(journalId, journalLines, Constants.RegisterType.TokenActivated, accountId, amount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.PhysicalTokenPurchases, accountId, amount);

        // check if there is any fee to process associated with this journal.
        FeeTier feeTier = getFeeTier();
        if(feeTier != null) {
            for (FeeTierDetail feeTierDetail : feeTier.getFeeTierDetails()) {
                // TODO cleanup how we deal with the currency.
                Amount feeDetailAmount = Amount.of(feeTierDetail.getTierDetailFeeQuantity(), amount.getCurrencyId());
                int feeAccountId = feeTierDetail.getTierDetailFeeAccountId();
                addDebitLine(journalId, journalLines, Constants.RegisterType.Fee, feeAccountId, feeDetailAmount);
                feeQuantity += feeDetailAmount.getQuantity();
            }
        }

        // TODO cleanup how we deal with the currency.
        Amount grossAmount = Amount.of(feeQuantity + amount.getQuantity(), amount.getCurrencyId());

        // Credit/Debit 2
        addCreditLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, grossAmount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.TokenReceivable, accountId, amount);

        // Credit/Debit 3
        //addCreditLine(journalId, journalLines, Constants.RegisterType.IssuerPrefundAccount, issuerAccountId, amount);
        //addDebitLine(journalId, journalLines, Constants.RegisterType.CardsPayablePending, issuerAccountId, amount);

        return journalLines;
    }

}
