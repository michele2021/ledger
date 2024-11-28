package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.accounting.commons.Constants.DUMMY_CARDHOLDER_CODE;

@Slf4j
public class CardDefundTemplate extends AbstractTemplate {

    public CardDefundTemplate(JournalMaker journalMaker,
                              Account cardProgramAccount,
                              Amount amount,
                              JournalType journalType,
                              TxType txType,
                              QueueMessage message,
                              RepositoryHelper repositoryHelper) {

        // message.getTxReference() is the "cardTokenId", unique identifier of the card token.
        super(txType, cardProgramAccount, message.getTxReference(), journalType, journalMaker,
                amount, message.getAdditionalData(), repositoryHelper);
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();

        // Debit
        addDebitLine(journalId, journalLines, Constants.RegisterType.CardProgram, account.getAccountId(), amount);

        // Credit
        // TODO temporarily we use a dummy account.
        Account dummyAccount = accountRepo.getByCodeAndType(DUMMY_CARDHOLDER_CODE, Constants.AccountType.CardHolder.toString());
        addCreditLine(journalId, journalLines, Constants.RegisterType.CardHolder, dummyAccount.getAccountId(), amount);

        return journalLines;
    }
}
