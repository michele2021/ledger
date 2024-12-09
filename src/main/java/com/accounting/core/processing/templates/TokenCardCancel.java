package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static java.lang.String.format;

@Slf4j
public class TokenCardCancel extends AbstractTemplate {

    private TxStatus oriTx;
    private final CurrencyRepoIf currencyRepo;
    private final BigDecimal remainingBalance;
    private final OffsetDateTime bookingDateTime;

    public TokenCardCancel(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingTemplateException, AccountingMakerException {
        // message.getTxReference() is the "cardTokenId".
        super(txType, account, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        remainingBalance = getAdditionalData(Message.AdditionalData.remainingBalance).getAsBigDecimal();
        currencyRepo = repositoryHelper.getCurrencyRepo();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        validateBookingDateInThePast(bookingDateTime);
    }

    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = journalDAO.getTxStatusWithIssuerAccountByReferenceAndAccountId(txReference, account.getAccountId());
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));
        throwExceptionIfNull(tx.getIssuerAccountId(), format("Issuer account not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));

        // throws unchecked EventAlreadyProcessedException exception if the current status is already 'CardCancelled'.
        // throws AccountingMakerException exception if the current status is not compatible with the 'TokenCardCancel' message.
        checkCurrentStatus(tx);

        // check the consistency with the original Tx.
        validateAmount(tx);

        amount = tx.getAmount();
        oriTx = tx;
        journalMaker.createJournal(this);
    }

    private void checkCurrentStatus(TxStatus tx) throws AccountingMakerException {
        String journalTypeCode = tx.getJournalTypeCode();
        // Since idempotency is not supported at the API layer we have to be resilient in case the same message is sent twice
        // (we do not have a message identifier, so the best we can do is check the status)
        if (Constants.JournalType.CardCancelled.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'CardCancel' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // Check the compatibility with the current state (Converted --> CardCancelled)
        List<String> allowedStatuses = Collections.singletonList(Constants.JournalType.Converted.toString());
        if (!allowedStatuses.contains(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'TokenCardCancel' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "', but the only compatible status is 'Converted'.");
        }
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();

        long accountId = account.getAccountId();
        long issuerAccountId = oriTx.getIssuerAccountId();
        // N.B. We get the currency of the remaining balance from the Token tx
        int currencyId =  oriTx.getAmount().getCurrencyId();
        Currency c = currencyRepo.get((long)currencyId);
        Amount rbAmount = Amount.of(Constants.toMinorUnits(c.getCode(), remainingBalance), currencyId);

        // Debit/Credit 1
        addDebitLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, rbAmount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.TokenCardCancelled, accountId, rbAmount);

        // Debit/Credit 2
        addDebitLine(journalId, journalLines, Constants.RegisterType.CardsPayablePending, issuerAccountId, rbAmount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.CardsPayable, issuerAccountId, rbAmount);

        return journalLines;
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public long getTxId() {
        return oriTx.getTxId();
    }
}
