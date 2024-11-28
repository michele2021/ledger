package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.entities.composite.TxJournal;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class ClientOrderCancelled extends AbstractTemplate {

    private long orderTxId;
    private final JournalTypeRepoIf journalTypeRepo;

    public ClientOrderCancelled(JournalMaker journalMaker,
                                Account clientAccount,
                                Amount amount,
                                JournalType journalType,
                                TxType txType,
                                QueueMessage message,
                                RepositoryHelper repositoryHelper) {
        super(txType, clientAccount, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        this.journalTypeRepo = repositoryHelper.getJournalTypeRepo();
    }

    @Override
    public void process() throws AccountingMakerException {
        long orderId = getAdditionalData(Message.AdditionalData.orderId).getAsLong();

        TxJournal tx = journalDAO.getTxJournalByOrderId(orderId);
        throwExceptionIfNull(tx, "Tx not found for orderId: " + orderId);

        Constants.JournalType status = tx.getJournals().get(0).getJournalTypeCode();
        throwExceptionIf(status != Constants.JournalType.Received,
                "The order with Tx id " + tx.getTxId() + " has status '" + status.toString() + "', but 'Received' was expected." + this.getClass());

        validateAccount(tx);

        orderTxId = tx.getTxId();
        amount = tx.getAmount();

        // TODO use a transaction here, as was done in ClientOrderReleased. Probably do the same in clientOrderReceived
        //        jdbi.withHandle(handle ->
        //                handle.inTransaction(h -> {
        //                    journalMaker.createJournal(this);
        //                    String message = buildMsg(Message.EventType.CLIENT_ORDER_RELEASE_BALANCE, clientAccount, amount).toString();
        //
        //                    journalMaker.createJournal(this);

        List<Tx> txs = journalDAO.getTxsByParentTxId(tx.getTxId());
        JournalType jtCancelled = journalTypeRepo.get(Constants.JournalType.Cancelled.toString());
        // cancel the tokens as well
        for (Tx tokenTx : txs) {
            log.info("processing ClientTokenCancelled for token tx: {}", tx.getTxId());
            JournalTemplateIf tokenTemplate = new ClientTokenCancelled(account, tokenTx, jtCancelled, journalMaker, repositoryHelper);
            tokenTemplate.process();
        }
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public long getTxId() {
        return orderTxId;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();

        // TODO possibly add another movement on the Client ledger: From "Reserved" to "Received"
        // Debit (add back the money into the client register, since the order has been cancelled)
        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.Client, accountId, amount);

        // Credit (subtract money from the Client)
        addCreditLine(journalId, journalLines, Constants.RegisterType.ClientToken, accountId, amount);
        return journalLines;
    }
}
