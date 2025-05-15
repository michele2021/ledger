package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static com.accounting.commons.Message.AdditionalData.expiryDate;
import static java.lang.String.format;

@Slf4j
public class TokenActivate extends AbstractTemplate {

    private Tx oriTx;
    private Account issuerAccount;
    private final OffsetDateTime bookingDateTime;

    public TokenActivate(JournalMaker journalMaker,
                         Account account,
                         Amount amount,
                         JournalType journalType,
                         TxType txType,
                         QueueMessage message,
                         RepositoryHelper repositoryHelper) throws AccountingTemplateException, AccountingMakerException {
        // message.getTxReference() is the "cardTokenId".
        super(txType, account, message.txReference(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        validateBookingDateInThePast(bookingDateTime);
    }

    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = journalDAO.getTxStatusByReferenceAndAccountId(txReference, account.getAccountId());
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));

        // throw unchecked EventAlreadyProcessedException if status Activated
        checkCurrentStatus(tx);

        // check the consistency with the original Tx.
        validateAmount(tx);

        amount = tx.getAmount();
        oriTx = tx;
        // throws exception if not found
        issuerAccount = getIssuerAccount();

        super.process();
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public Long linkTx() {
        return oriTx.getTxId();
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    private void checkCurrentStatus(TxStatus tx) throws AccountingMakerException {
        String journalTypeCode = tx.getJournalTypeCode();
        // check if the same message was already processed
        if (Constants.JournalType.Activated.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'Activate' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // check the compatibility with the current state (Created/Deactivated --> Activated)
        if (!Constants.JournalType.Created.toString().equals(journalTypeCode) && !Constants.JournalType.Deactivated.toString().equals(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'Activate' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "' cannot be activated.");
        }
    }

    @Override
    public void updateSpecializedTx() throws AccountingMakerException {
        // we already verified before that the mandatory additional data exists.
        LocalDate expiryLD = LocalDate.parse(getAdditionalData(expiryDate).getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
        String alr = "getAdditionalData";
        String atz = "activationTimezone";
        String att = "getAdditionalData";
        OffsetDateTime ats = null;
        if (Objects.nonNull(att) && Objects.nonNull(atz)) {
            Instant instant = Instant.parse(att);
            ats = OffsetDateTime.ofInstant(instant, ZoneId.of(atz));
        }

        // journalDAO.updateTokenTx(oriTx.getTxId(), null, alr, expiryLD, ats, atz, null);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();
        long feeQuantity = 0;

        long accountId = account.getAccountId();
        long issuerAccountId = issuerAccount.getAccountId();

        // Credit/Debit 1
        addCreditLine(journalId, journalLines, Constants.RegisterType.TokenCreated, accountId, amount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.TokenActivated, accountId, amount);

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
