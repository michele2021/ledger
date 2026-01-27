package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.PlatformRepoIf;
import com.accounting.commons.utils.AccountingUtils;
import com.accounting.core.entities.*;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
public class OrderReceived extends AbstractTemplate {
    private final Order order;
    private final PlatformRepoIf platformRepo;

    public OrderReceived(JournalMaker journalMaker,
                               Account account,
                               Amount amount,
                               JournalType journalType,
                               TxType txType,
                               QueueMessage message,
                               RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, account, message.txReference(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);

        this.platformRepo = repositoryHelper.getPlatformRepo();
        // TODO store the order and the line items.
        this.order = gson.fromJson(additionalData, Order.class);
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
    public boolean addSpecializedTx() {
        return true;
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) {
        var platform = platformRepo.get(order.platform().toString());
        var orderDTO = order.orderDTO(txId, platform.getID());
        return List.of(orderDTO);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        var journalLines = new ArrayList<JournalLine>();

        long vatQuantity = getVatQuantity();
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

    // tax lines not mandatory for now
    private long getVatQuantity() {
        var taxLines = order.taxLines();
        if(AccountingUtils.isEmptyOrNull(taxLines)) {
            return 0L;
        }
        String currencyCode = amount.getCurrencyCode();
        return taxLines.stream()
                .mapToLong(taxLine -> Constants.toMinorUnits(currencyCode, new BigDecimal(taxLine.price())))
                .sum();
    }
}