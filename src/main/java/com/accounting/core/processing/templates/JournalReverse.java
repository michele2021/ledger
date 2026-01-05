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
import com.accounting.core.entities.JournalLineWithTxDTO;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JournalReverse extends AbstractTemplate {

    private final long originalJournalId;
    private List<JournalLineWithTxDTO> originalJournalLines;

    public JournalReverse(JournalMaker journalMaker,
                          Account account,
                          Amount amount,
                          JournalType journalType,
                          TxType txType,
                          QueueMessage message,
                          RepositoryHelper repositoryHelper) throws AccountingMakerException {
        super(txType, account, message.uuid(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        this.originalJournalId = getMandatoryAdditionalData(Message.AdditionalData.journalId).getAsLong();
        validateData();
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public Tx buildTx() {
        // Get parent tx ID from the first original journal line
        long parentTxId = originalJournalLines.getFirst().getTxId();
        return buildBaseTx(parentTxId);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> reversedLines = new ArrayList<>();

        // Fetch original journal lines and swap credit and debit
        for (JournalLineWithTxDTO originalLine : originalJournalLines) {
            var currency = Constants.getCurrencyByIso(originalLine.getCurrencyId());
            var amount = Amount.of(Constants.toMinorUnits(currency,
                    BigDecimal.valueOf(originalLine.getQuantity())), originalLine.getCurrencyId());

            if (originalLine.isDebit()) {
                // Original was debit, so add as credit in reversed journal
                addCreditLine(originalJournalId, reversedLines, originalLine.getRegisterTypeId(), originalLine.getAccountId(), amount);
            } else {
                // Original was credit, so add as debit in reversed journal
                addDebitLine(originalJournalId, reversedLines, originalLine.getRegisterTypeId(), originalLine.getAccountId(), amount);
            }
        }
        return reversedLines;
    }

    private void validateData() throws AccountingMakerException {
        // Fetch the original journal lines with transaction ID based on journalId
        originalJournalLines = journalDAO.getJournalLinesWithTxByJournalId(originalJournalId);
        throwExceptionIf(originalJournalLines == null || originalJournalLines.isEmpty(),
                "Original journal lines not found for journalId: " + originalJournalId);
    }
}