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
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

public class AccountFeeAdjustment extends AbstractTemplate {

    private final BigDecimal accountFeeAmountBD;

    public AccountFeeAdjustment(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingMakerException {
        super(txType, account, message.getUuid(), journalType, journalMaker, amount,
                message.getAdditionalData(), repositoryHelper);
        accountFeeAmountBD = getAdditionalData(Message.AdditionalData.accountFeeAmount).getAsBigDecimal();
    }

    @Override
    public Tx buildTx() {
        if (isNull(this.txReference)) {
            String uuid = UUID.randomUUID().toString();
            this.txReference = txType.getCode().substring(0, 3).toUpperCase().concat("_").concat(uuid);
        }
        return buildBaseTx();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();

        long accountId = account.getAccountId();

        String ccAccountCode = Constants.BusinessEntityAccount.CleverCards.toString();
        Account ccAccount = accountRepo.getByCodeAndType(ccAccountCode, Constants.AccountType.BusinessEntity.toString());
        throwExceptionIfNull(ccAccount, "Business Entity not found in the repository for account code: " + ccAccount);
        long ccAccountId = ccAccount.getAccountId();

        Constants.Currencies currency = Constants.Currencies.valueOf(amount.getCurrencyCode());
        Amount accountFeeAmount = Amount.of(Constants.toMinorUnits(currency, accountFeeAmountBD), amount.getCurrencyId());

        if(amount.getQuantity() <= 0) {
            throw new AccountingMakerException("Only positive amount is supported at the moment. Amount received: " + amount);
        }
        if(amount.getQuantity() < accountFeeAmount.getQuantity()) {
            throw new AccountingMakerException("The amount " + amount + " should be greater or equal to the accountFeeAmount: " + accountFeeAmount);
        }

        long ccFeeQuantity = amount.getQuantity() - accountFeeAmount.getQuantity();
        Amount ccFeeAmount = Amount.of(ccFeeQuantity, amount.getCurrencyId());

        // Credit/Debit
        addCreditLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, amount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.Fee, accountId, accountFeeAmount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.Fee, ccAccountId, ccFeeAmount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, accountFeeAmount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.FeeReceivable, accountId, accountFeeAmount);

        return journalLines;
    }
}
