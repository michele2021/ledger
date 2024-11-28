package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalDTO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.JournalLineDTO;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static java.lang.String.format;

@Slf4j
public class TokenCancel extends AbstractTemplate {

    private TxStatus oriTx;
    private final OffsetDateTime bookingDateTime;

    public TokenCancel(JournalMaker journalMaker,
                       Account account,
                       Amount amount,
                       JournalType journalType,
                       TxType txType,
                       QueueMessage message,
                       RepositoryHelper repositoryHelper) throws AccountingTemplateException, AccountingMakerException {
        // message.getTxReference() is the "cardTokenId".
        super(txType, account, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        validateBookingDateInThePast(bookingDateTime);
    }


    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = getOriginalTxWithStatusByTxReference();
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));

        // throws unchecked EventAlreadyProcessedException exception if the current status is already 'Cancelled'.
        // throws AccountingMakerException exception if the current status is not compatible with the 'cancel' message.
        checkCurrentStatus(tx);

        // check the consistency with the original Tx.
        validateAmount(tx);

        amount = tx.getAmount();
        oriTx = tx;
        journalMaker.createJournal(this);
    }

    private TxStatus getOriginalTxWithStatusByTxReference() {
        if (StringUtils.isNumeric(txReference)) {
            // we do not know if "txReference" is cardTokenId or serialNumber.
            // Although normally we expect the cardTokenId to be UUID (therefore not numeric) there is no such guarantee.
            return journalDAO.getTxStatusByReferenceOrTokenSerialNumber(txReference, account.getAccountId());
        }
        // If the txReference is not numeric, we know it is a cardTokenId, since the serialNumber is always numeric.
        return journalDAO.getTxStatusByReferenceAndAccountId(txReference, account.getAccountId());
    }

    private void checkCurrentStatus(TxStatus tx) throws AccountingMakerException {
        String journalTypeCode = tx.getJournalTypeCode();
        // Since idempotency is not supported at the API layer we have to be resilient in case the same message is sent twice
        // (we do not have a message identifier, so the best we can do is check the status)
        if (Constants.JournalType.Cancelled.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'Cancel' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // Check the compatibility with the current state (Created/Activated/Deactivated --> Cancelled)
        List<String> allowedStatuses = Arrays.asList(Constants.JournalType.Created.toString(), Constants.JournalType.Activated.toString(), Constants.JournalType.Deactivated.toString());
        if (!allowedStatuses.contains(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'Cancel' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "' cannot be cancelled.");
        }
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> jls = new ArrayList<>();

        // N.B: A Token can be cancelled only if it was in status Activated, Created, Deactivated.

        JournalDTO journal = journalDAO.getLatestJournalByTxReferenceAndJournalTypeCode(oriTx.getTxReference(), oriTx.getJournalTypeCode());
        List<String> registerTypeCodes;
        if (Constants.JournalType.Created.toString().equals(oriTx.getJournalTypeCode())) {
            registerTypeCodes = List.of(Constants.RegisterType.PhysicalTokenPurchases.toString());
        } else {
            // sce
            registerTypeCodes = List.of(Constants.RegisterType.PhysicalTokenPurchases.toString(), Constants.RegisterType.TokenCreated.toString());
        }

        JournalLineDTO journalLineSwitch;
        // search the line to replace (either PhysicalTokenPurchases or TokenCreated)
        List<JournalLineDTO> journalLines = journal.getJournalLines()
                .stream()
                .filter(e -> (registerTypeCodes.contains(e.getRegisterTypeCode())))
                .collect(Collectors.toList());
        if(journalLines.size() != 1) {
            throw new AccountingMakerException("For journal " + journal.getJournalId() + ", " + journalLines.size() + " line(s) found with register types " + registerTypeCodes + ", but exactly 1 was expected.");
        }
        journalLineSwitch = journalLines.get(0);

        // If the previous state is 'Deactivated', we debit the TokenCancelled and credit either the 'Created' or the 'PhysicalTokenPurchases'
        if(Constants.JournalType.Deactivated.toString().equals(oriTx.getJournalTypeCode())) {
            long accountId = journalLineSwitch.getAccountId();
            addDebitLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.TokenCancelled), accountId, journalLineSwitch.getAmount());
            addCreditLine(journalId, jls, journalLineSwitch.getRegisterTypeId(), accountId, journalLineSwitch.getAmount());
            return jls;
        }

        // If the previous state was 'Activated' or 'Created` we revert what was booked in the previous journal, but replacing one line with TokenCancelled.
        for (JournalLineDTO line :  journal.getJournalLines()) {
            int registerTypeId = line.getRegisterTypeId();
            // switch TokenCreated/PhysicalTokenPurchases with TokenCancelled
            if (journalLineSwitch.getJournalLineId() == line.getJournalLineId()) {
                registerTypeId = getRegisterTypeId(Constants.RegisterType.TokenCancelled);
            }
            if(line.isDebit()) {
                addCreditLine(journalId, jls, registerTypeId, line.getAccountId(), line.getAmount());
            } else {
                addDebitLine(journalId, jls, registerTypeId, line.getAccountId(), line.getAmount());
            }
        }
        return jls;
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
