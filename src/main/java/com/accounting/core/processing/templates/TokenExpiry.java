package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

@Slf4j
public class TokenExpiry extends AbstractTemplate {

    private TxStatus oriTx;

    public TokenExpiry(JournalMaker journalMaker,
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
        TxStatus tx = journalDAO.getTxStatusWithIssuerAccountByReferenceAndAccountId(txReference, account.getAccountId());
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));
        throwExceptionIfNull(tx.getIssuerAccountId(), format("Issuer account not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));

        // throws unchecked EventAlreadyProcessedException exception if the current status is already 'Expired'.
        // throws AccountingMakerException exception if the current status is not compatible with the 'expiry' message.
        checkCurrentStatus(tx);

        // check the consistency with the original Tx.
        validateAmount(tx);

        amount = tx.getAmount();
        oriTx = tx;
        journalMaker.createJournal(this);
    }

    private void checkCurrentStatus(TxStatus tx) throws AccountingMakerException {
        String journalTypeCode = tx.getJournalTypeCode();
        // Since idempotency is not supported at the API layer we have to be resilient in case the same message is sent twice
        // (we do not have a message identifier, so the best we can do is check the status)
        if (Constants.JournalType.Expired.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'Expiry' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // Check the compatibility with the current state (Activated --> Expired)
        if (!Constants.JournalType.Activated.toString().equals(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'Expiry' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "' cannot be expired.");
        }
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> jls = new ArrayList<>();

        //1 Line: TokenExpired, oriTx.getAccountId(), Debit
        addDebitLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.TokenExpired), oriTx.getAccountId(), oriTx.getAmount());
        //2 Line: TokenActivated, oriTx.getAccountId(), Credit
        addCreditLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.TokenActivated), oriTx.getAccountId(), oriTx.getAmount());

        //3 Line: CardsPayablePending, oriTx.getIssuerAccountId(), Credit
        addCreditLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.CardsPayablePending), oriTx.getIssuerAccountId(), oriTx.getAmount());

        //4 Line: OpenBalance/TokenExpiryRevenue, Debit (depending on account property configuration)
        Optional<AccountProperty> accountProperty = account.getAccountPropertyByCode(Constants.AccountPropertyCode.ExpiryMode);
        if (accountProperty.isPresent() && accountProperty.get().getValue().equals("AccountBalance")) {
            // for specific accounts that have the account property "ExpiryMode" configured
            addDebitLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.OpenBalance), oriTx.getAccountId(), oriTx.getAmount());
        } else {
            // default behaviour
            String ccAccountCode = Constants.BusinessEntityAccount.CleverCards.toString();
            Account ccAccount = accountRepo.getByCodeAndType(ccAccountCode, Constants.AccountType.BusinessEntity.name());
            if (Objects.isNull(ccAccount)) {
                throw new AccountingMakerException("Account '" + ccAccountCode + "' of type 'BusinessEntity' not found.");
            }
            addDebitLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.TokenExpiryRevenue), ccAccount.getAccountId(), oriTx.getAmount());
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
