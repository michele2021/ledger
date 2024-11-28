package com.accounting.core.jobs.datafix;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.AccountType;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.AccountTypeRepoIf;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.commons.repository.TxTypeRepoIf;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.db.DataFixDAO;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.entities.CardBackFill;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.processing.templates.CardCreate;
import com.accounting.core.processing.templates.JournalTemplateIf;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

@Slf4j
public class BackFillCardAccountExecutor {
    private final Jdbi jdbi;
    private final RepositoryHelper repositoryHelper;
    private final JournalMaker journalMaker;
    private final AccountRepoIf accountRepo;
    private final TxTypeRepoIf txTypeRepo;
    private final JournalTypeRepoIf journalTypeRepo;
    private final AccountTypeRepoIf accountTypeRepo;
    private final AccountWriteDAO accountWriteDAO;

    @Inject
    public BackFillCardAccountExecutor(Jdbi jdbi, RepositoryHelper repositoryHelper, JournalMaker journalMaker) {
        this.jdbi = jdbi;
        this.repositoryHelper = repositoryHelper;
        this.journalMaker = journalMaker;
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.txTypeRepo = repositoryHelper.getTxTypeRepo();
        this.journalTypeRepo = repositoryHelper.getJournalTypeRepo();
        this.accountTypeRepo = repositoryHelper.getAccountTypeRepo();
        this.accountWriteDAO = repositoryHelper.getAccountWriteDAO();
    }

    public void execute() throws JobProcessingException {
        try (Handle handle = jdbi.open()) {
            DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);
            List<CardBackFill> cardsToBackFill = dataFixDAO.getCardsToBackFill();
            for (CardBackFill card : cardsToBackFill) {
                String prepaidCardExternalId = card.getPrepaidCardExternalId();
                log.info("Starting backfill for card with external id: [{}]", prepaidCardExternalId);
                Account clientAccount = accountRepo.getByCodeAndType(card.getAccountCode(), card.getAccountTypeCode());
                Account cardAccount = createCardAccount(prepaidCardExternalId);
                JournalType jtCreated = journalTypeRepo.get(Constants.JournalType.Created.toString());
                TxType txtCard = txTypeRepo.get(Constants.TxType.Card.toString());
                JsonObject additionalData = buildAdditionalData(prepaidCardExternalId);
                JournalTemplateIf journalTemplate = new CardCreate(journalMaker,
                        cardAccount,
                        clientAccount,
                        card.getAmount(),
                        jtCreated,
                        txtCard,
                        card.getTxId(),
                        additionalData,
                        repositoryHelper);
                journalTemplate.process();
                dataFixDAO.markCardAsBackFilled(prepaidCardExternalId);
                log.info("Finish backfill for card with external id: [{}]", prepaidCardExternalId);
            }
        }  catch (Exception e) {
            log.error("Exception while processing the data fix: ", e);
            throw new JobProcessingException("Exception while processing the data fix: ", e);
        }
    }

    private JsonObject buildAdditionalData( String prepaidCardExternalId ) {
        JsonObject additionalData = new JsonObject();
//        String bookingDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//        additionalData.addProperty(Message.AdditionalData.bookingDate.toString(), bookingDate);
        additionalData.addProperty(Message.AdditionalData.prepaidCardExternalId.toString(), prepaidCardExternalId);
        return additionalData;
    }

    private Account createCardAccount(String prepaidCardExternalId) throws AccountingMakerException {
        Account maybeExist = accountRepo.getByCodeAndType(prepaidCardExternalId, Constants.AccountType.CardAccount.name());
        if (maybeExist != null) {
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
}
