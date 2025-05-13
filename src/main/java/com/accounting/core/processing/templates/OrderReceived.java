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
import com.accounting.core.entities.TaxLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
public class OrderReceived extends AbstractTemplate {

    private JsonArray lineItems;
    private final List<TaxLine> taxLines;
    private final Gson gson;

    public OrderReceived(JournalMaker journalMaker,
                               Account account,
                               Amount amount,
                               JournalType journalType,
                               TxType txType,
                               QueueMessage message,
                               RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, account, message.txReference(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);

        this.gson = new Gson();
        // TODO store the order and the line items.
        this.lineItems = getAdditionalData(Message.AdditionalData.lineItems, JsonElement::getAsJsonArray);
        var taxLinesAsJson = getAdditionalData(Message.AdditionalData.taxLines, JsonElement::getAsJsonArray);
        Type type = new TypeToken<List<TaxLine>>(){}.getType();
        this.taxLines = new Gson().fromJson(taxLinesAsJson, type);

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

        // tax lines not mandatory for now
        long vatQuantity = taxLines.stream()
                .map(TaxLine::price)
                .map(BigDecimal::new)
                .map(p -> Constants.toMinorUnits(amount.getCurrencyCode(), p))
                .reduce(Long::sum).orElse(0L);
        var revenueAmount = amount.subtract(vatQuantity);

        // Debit
        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.Receivable, accountId, amount);

        // Credit
        addCreditLine(journalId, journalLines, Constants.RegisterType.Revenue, accountId, revenueAmount);
        if(vatQuantity != 0) {
            var vatAmount = Amount.of(vatQuantity, amount.getCurrencyId());
            addCreditLine(journalId, journalLines, Constants.RegisterType.VAT_OUT, accountId, vatAmount);
        }

        return journalLines;
    }
}