package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
public class OrderReceived extends AbstractTemplate {

    public OrderReceived(JournalMaker journalMaker,
                               Account account,
                               Amount amount,
                               JournalType journalType,
                               TxType txType,
                               QueueMessage message,
                               RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, account, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
    }

    @Override
    public void process() throws AccountingMakerException {
        var tx = journalDAO.getTxByReference(txReference);
        throwExceptionIf(Objects.nonNull(tx), format("Tx with txReference '%s' is already existent.", txReference));

        journalMaker.createJournal(this);
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        var journalLines = new ArrayList<JournalLine>();

        // Debit
        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.Receivable, accountId, amount);

        // Credit
        var amounts = amount.split(20);
        addCreditLine(journalId, journalLines, Constants.RegisterType.Revenue, accountId, amounts[0]);
        addCreditLine(journalId, journalLines, Constants.RegisterType.VAT, accountId, amounts[1]);
        return journalLines;
    }
}