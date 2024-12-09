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

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Slf4j
public class TokenDeactivate extends AbstractTemplate {

    private TxStatus oriTx;

    public TokenDeactivate(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        // message.getTxReference() is the "cardTokenId".
        super(txType, account, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
    }


    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = getOriginalTxWithStatusByTxReference();
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));

        // throws unchecked EventAlreadyProcessedException exception if the current status is already 'Deactivated'.
        // throws AccountingMakerException exception if the current status is not compatible with the 'deactivate' message.
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
        if (Constants.JournalType.Deactivated.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'Deactivate' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // Check the compatibility with the current state (Activated --> Deactivated)
        if (!Constants.JournalType.Activated.toString().equals(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'Deactivate' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "' cannot be deactivated.");
        }
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> jls = new ArrayList<>();

        // N.B: A Token can be deactivated only if it was in status Activated.
        // We revert what was booked in the previous journal.

        JournalDTO journal = journalDAO.getLatestJournalByTxReferenceAndJournalTypeCode(oriTx.getTxReference(), oriTx.getJournalTypeCode());
        for (JournalLineDTO line : journal.getJournalLines()) {
            if (line.isDebit()) {
                addCreditLine(journalId, jls, line.getRegisterTypeId(), line.getAccountId(), line.getAmount());
            } else {
                addDebitLine(journalId, jls, line.getRegisterTypeId(), line.getAccountId(), line.getAmount());
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
