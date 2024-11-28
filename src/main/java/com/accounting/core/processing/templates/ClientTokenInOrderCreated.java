package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.AccountTypeRepoIf;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.commons.repository.TxVariantRepoIf;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.entities.JournalDb;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClientTokenInOrderCreated implements JournalTemplateIf {

    private AccountBase tokenHolderAccount;

    @Getter
    private final JournalType journalType;

    @Getter
    private final JournalMaker journalMaker;

    private final TxType txType;
    private final TokenOriginal token;
    private final Account clientAccount;
    private final AccountWriteDAO accountWriteDAO;
    private final JournalDb orderJournalDb;
    private final TxVariantRepoIf txVariantRepo;
    private final AccountTypeRepoIf accountTypeRepo;
    private final RegisterTypeRepoIf registerTypeRepo;

    public ClientTokenInOrderCreated(JournalMaker journalMaker,
                                     JournalDb orderJournalDb,
                                     Account clientAccount,
                                     TokenOriginal token,
                                     JournalType journalType,
                                     TxType txType,
                                     RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.orderJournalDb = orderJournalDb;
        this.clientAccount = clientAccount;
        this.journalType = journalType;
        this.txType = txType;
        this.token = token;
        this.registerTypeRepo = repositoryHelper.getRegisterTypeRepo();
        this.accountTypeRepo = repositoryHelper.getAccountTypeRepo();
        this.txVariantRepo = repositoryHelper.getTxVariantRepo();
        this.accountWriteDAO = repositoryHelper.getAccountWriteDAO();
    }


    @Override
    public void process() throws AccountingMakerException {
        createTokenHolderAccountIfNecessary(token);
        journalMaker.createJournal(this);
    }

    // TODO decide if the token holder account is identified by email .
    // The token must be linked to the card, but possibly no necessity of a token holder account to be created (could use a fictitious one).
    private void createTokenHolderAccountIfNecessary(TokenOriginal token) {
        String email = token.getRecipientEmail();
        // TODO this can be used to move the AccountCache considering the accountCode for accountType=tokenHolder is the email.
        tokenHolderAccount = accountWriteDAO.getTokenHolderAccount(email);
        if (tokenHolderAccount == null) {
            log.info("Account not found for email {}. A new account will be created.", email);
            tokenHolderAccount = createTokenHolderAccount(token);
        } else {
            log.info("Token holder account corresponding to email {} is already existent.", email);
        }
    }

    private AccountBase createTokenHolderAccount(TokenOriginal token) {
        String accountCode = token.getRecipientEmail();
        AccountType atTokenHolder = accountTypeRepo.get(Constants.AccountType.TokenHolder.toString());
        return accountWriteDAO.createTokenHolderAccount(accountCode, null, atTokenHolder.getAccountTypeId(), token);
    }

    @Override
    public Tx buildTx() {
        int txVariantId = txVariantRepo.get(Constants.TxVariant.BASE.toString()).getTxVariantId();
        Amount amount = Amount.of(token.getQuantity(), token.getCurrencyId());
        return Tx.builder()
                .txTypeId(txType.getTxTypeId())
                .accountId(tokenHolderAccount.getAccountId())
                .amount(amount)
                .creationDate(OffsetDateTime.now())
                .parentTxId(orderJournalDb.getTxId())
                .txVariantId(txVariantId)
                .build();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> journalLines = new ArrayList<>();

        Amount amount = Amount.of(token.getQuantity(), token.getCurrencyId());

        // Debit (this register is the balance of the issued tokens, probably is going to have a different name)
        RegisterType rtDebit = registerTypeRepo.get(Constants.RegisterType.ClientToken.toString());
        long batchId = journalMaker.getOrCreateAvailableBatch(rtDebit.getRegisterTypeId(), clientAccount.getAccountId());
        JournalLine jl = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .debit(amount).build();
        journalLines.add(jl);

        // Credit
        RegisterType rtCredit = registerTypeRepo.get(Constants.RegisterType.TokenCreated.toString());
        batchId = journalMaker.getOrCreateAvailableBatch(rtCredit.getRegisterTypeId(), tokenHolderAccount.getAccountId());
        jl = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .credit(amount).build();
        journalLines.add(jl);
        return journalLines;
    }

    @Override
    public boolean isTestAccount() {
        return clientAccount.isTest();
    }
}
