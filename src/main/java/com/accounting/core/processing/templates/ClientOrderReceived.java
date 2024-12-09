package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.commons.repository.TxTypeRepoIf;
import com.accounting.core.entities.*;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.commons.Message.AdditionalData.*;

/**
 * N.B. This class is currently not used.
 * There is no longer the possibility to create a record in the "orders" table associated with a tx
 * (this functionality can be reintroduced if necessary).
 */
@Slf4j
public class ClientOrderReceived extends AbstractTemplate {

    private static final Type TOKENS_LIST_TYPE = new TypeToken<ArrayList<TokenOriginal>>() {
    }.getType();

    private long parentTxId;
    private final Gson gson;
    private final TxTypeRepoIf txTypeRepo;
    private final JournalTypeRepoIf journalTypeRepo;

    public ClientOrderReceived(JournalMaker journalMaker,
                               Account clientAccount,
                               Amount amount,
                               JournalType journalType,
                               TxType txType,
                               QueueMessage message,
                               RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, clientAccount, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        this.gson = new Gson();
        this.txTypeRepo = repositoryHelper.getTxTypeRepo();
        this.journalTypeRepo = repositoryHelper.getJournalTypeRepo();
    }

    @Override
    public boolean addSpecializedTx() {
        return true;
    }

    @Override
    public void process() throws AccountingMakerException {
        List<TokenOriginal> tokens = getListOfTokens(gson);
        log.info("number of tokens: {} total amount: {}", tokens.size(), amount.getQuantity());

        // check the consistency with the original Tx.
        Tx parentTx = getTxByReference();
        validateAmount(parentTx);
        validateAccount(parentTx);

        parentTxId = parentTx.getTxId();

        JournalDb orderJournal = journalMaker.createJournal(this);
        TxType txtToken = txTypeRepo.get(Constants.TxType.Token.toString());
        JournalType jtCreated = journalTypeRepo.get(Constants.JournalType.Created.toString());
        int currencyId = amount.getCurrencyId();
        for (TokenOriginal token : tokens) {
            token.setCurrencyId(currencyId);
            log.info("processing TokenTemplate for recipient {}", token.getRecipientEmail());
            JournalTemplateIf tokenTemplate = new ClientTokenInOrderCreated(journalMaker,
                    orderJournal,
                    account,
                    token,
                    jtCreated,
                    txtToken,
                    repositoryHelper);
            tokenTemplate.process();
        }
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx(parentTxId);
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) {
        SpecializedTx tx = Order.builder()
                .sku(additionalData.get(sku.toString()).getAsString())
                .cardProgram(additionalData.get(cardProgram.toString()).getAsString())
                .title(additionalData.get(orderTitle.toString()).getAsString())
                .externalId(additionalData.get(orderId.toString()).getAsLong()).build();
        return List.of(tx);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();

        // TODO possibly add another movement on the Client ledger: From "Reserved" to "Received"
        // Debit (this register is the balance of the issued tokens, probably is going to have a different name)
        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.OrderProcessed, accountId, amount);

        // Credit
        addCreditLine(journalId, journalLines, Constants.RegisterType.ClientReserved, accountId, amount);
        return journalLines;
    }

    List<TokenOriginal> getListOfTokens(Gson gson) throws AccountingMakerException {
        List<TokenOriginal> tokens = gson.fromJson(getAdditionalData(Message.AdditionalData.tokens), TOKENS_LIST_TYPE);
        validateTokens(tokens);
        return tokens;
    }

    void validateTokens(List<TokenOriginal> tokens) throws AccountingMakerException {
        Long totalQuantity = tokens.stream()
                .map(TokenOriginal::getQuantity)
                .reduce(0L, Long::sum);
        if (!amount.getQuantity().equals(totalQuantity)) {
            throw new AccountingMakerException("The total amount from the tokens " + totalQuantity + " is not equal to the total amount specified in the message " + amount);
        }
    }


}
