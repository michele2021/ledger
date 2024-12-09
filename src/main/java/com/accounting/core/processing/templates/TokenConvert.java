package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.AccountTypeRepoIf;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.commons.repository.TxTypeRepoIf;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.commons.Constants.RegisterType;
import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static java.lang.String.format;

@Slf4j
public class TokenConvert extends AbstractTemplate {

    private Tx oriTx;
    private Account issuerAccount;
    private final OffsetDateTime bookingDateTime;
    private final String prepaidCardExternalId;
    private final TxTypeRepoIf txTypeRepo;
    private final JournalTypeRepoIf journalTypeRepo;
    private final AccountTypeRepoIf accountTypeRepo;
    private final AccountWriteDAO accountWriteDAO;

    public TokenConvert(JournalMaker journalMaker,
                        Account account,
                        Amount amount,
                        JournalType journalType,
                        TxType txType,
                        QueueMessage message,
                        RepositoryHelper repositoryHelper) throws AccountingTemplateException, AccountingMakerException {
        // message.getTxReference() is the "cardTokenId".
        super(txType, account, message.getTxReference(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateIfTxReferencePresent();
        txTypeRepo = repositoryHelper.getTxTypeRepo();
        journalTypeRepo = repositoryHelper.getJournalTypeRepo();
        accountTypeRepo = repositoryHelper.getAccountTypeRepo();
        accountWriteDAO = repositoryHelper.getAccountWriteDAO();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        prepaidCardExternalId = getAdditionalData(Message.AdditionalData.prepaidCardExternalId).getAsString();
        validateBookingDateInThePast(bookingDateTime);
    }

    @Override
    public void updateSpecializedTx() {
        journalDAO.updateTokenTx(oriTx.getTxId(), null, null, null, null, null, prepaidCardExternalId);
    }

    @Override
    public void process() throws AccountingMakerException {
        TxStatus tx = journalDAO.getTxStatusByReferenceAndAccountId(txReference, account.getAccountId());
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s and accountId: %s", txReference, account.getAccountId()));
        validateAmount(tx);

        // throws unchecked EventAlreadyProcessedException exception if the current status is already 'Converted'.
        // throws AccountingMakerException exception if the current status is not compatible with the 'convert' message.
        checkCurrentStatus(tx);

        amount = tx.getAmount();
        oriTx = tx;

        // throws exception if not found
        issuerAccount = getIssuerAccount();
        journalMaker.createJournal(this);

        // create the card
        Account cardAccount = createCardAccount(prepaidCardExternalId);
        JournalType jtCreated = journalTypeRepo.get(Constants.JournalType.Created.toString());
        TxType txtCard = txTypeRepo.get(Constants.TxType.Card.toString());
        JournalTemplateIf journalTemplate = new CardCreate(journalMaker,
                cardAccount,
                account,
                amount,
                jtCreated,
                txtCard,
                oriTx.getTxId(),
                additionalData,
                repositoryHelper);
        journalTemplate.process();
    }

    private Account createCardAccount(String prepaidCardExternalId) throws AccountingMakerException {
        Account cardAccount1 = accountRepo.getByCodeAndType(prepaidCardExternalId, Constants.AccountType.CardAccount.name());
        if (cardAccount1 != null) {
            throw new AccountingMakerException("Card Account with id " + prepaidCardExternalId + " already exists!");
        }
        AccountType accountType = accountTypeRepo.get(Constants.AccountType.CardAccount.toString());
        Account cardAccount = Account.builder()
                .accountCode(prepaidCardExternalId)
                .accountTypeId(accountType.getAccountTypeId())
                .description("Card Account for cardId " + prepaidCardExternalId)
                .build();
        log.info("Creating card account for cardId [{}]", prepaidCardExternalId);
        long cardAccountId = accountWriteDAO.createAccount(cardAccount, false, null);
        // refresh caches
        repositoryHelper.refreshAccountCaches(cardAccountId);
        //
        return accountRepo.get(cardAccountId);
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public long getTxId() {
        return oriTx.getTxId();
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    private void checkCurrentStatus(TxStatus tx) throws AccountingMakerException {
        String journalTypeCode = tx.getJournalTypeCode();
        // check if the same message was already processed
        if (Constants.JournalType.Converted.toString().equals(journalTypeCode)) {
            throw new EventAlreadyProcessedException(
                    format("Processing 'Convert' message. Tx %s is in status %s (journalId: %s). Skipping the message without processing.",
                            tx.getTxId(), journalTypeCode, tx.getJournalId()));
        }
        // check the compatibility with the current state (Activated --> Converted)
        if (!Constants.JournalType.Activated.toString().equals(journalTypeCode)) {
            throw new AccountingMakerException("Processing 'Convert' message. Tx " + tx.getTxId() + " is in status '" + journalTypeCode + "' cannot be converted.");
        }
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> jl = new ArrayList<>();
        long accountId = account.getAccountId();
        long issuerAccountId = issuerAccount.getAccountId();

        // Credit/Debit 1
        addCreditLine(journalId, jl, RegisterType.TokenActivated, accountId, amount);
        addDebitLine(journalId, jl, RegisterType.TokenConverted, accountId, amount);

        // Credit/Debit 2
        addCreditLine(journalId, jl, RegisterType.CardsPayablePending, issuerAccountId, amount);
        addDebitLine(journalId, jl, RegisterType.CardsPayable, issuerAccountId, amount);

        return jl;
    }
}