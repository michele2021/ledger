package com.accounting.core.processing;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.conf.SystemConfig;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingEventException;
import com.accounting.commons.exceptions.AccountingException;
import com.accounting.commons.queue.MessageProcessorIf;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.commons.repository.TxTypeRepoIf;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.templates.JournalTemplateFactory;
import com.accounting.core.processing.templates.JournalTemplateFactoryImpl;
import com.accounting.core.processing.templates.JournalTemplateIf;
import com.accounting.core.services.RepositoryHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class MessageProcessor implements MessageProcessorIf {

    private final TemplateCache templateCache;
    private final JournalTypeRepoIf journalTypeRepo;
    private final AccountRepoIf accountRepo;
    private final CurrencyRepoIf currencyRepo;
    private final TxTypeRepoIf txTypeRepo;

    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JournalTemplateFactory journalTemplateFactory;

    @Inject
    public MessageProcessor(TemplateCache templateCache, JournalMaker journalMaker, RepositoryHelper repositoryHelper) {
        this.templateCache = templateCache;
        this.journalTypeRepo = repositoryHelper.getJournalTypeRepo();
        this.txTypeRepo = repositoryHelper.getTxTypeRepo();
        this.currencyRepo = repositoryHelper.getCurrencyRepo();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.journalTemplateFactory = new JournalTemplateFactoryImpl(journalMaker, repositoryHelper);
    }

    @Override
    public void process(QueueMessage message) throws AccountingException {
        JournalTemplate journalTemplate = validateMessage(message);
        JournalTemplateIf template = populateTemplate(message, journalTemplate);
        log.info("Processing message: {}", message);
        log.info("journalTemplate: {}", journalTemplate);
        template.process();
    }

    private JournalTemplate validateMessage(QueueMessage message) throws AccountingEventException {
        // N.B. Throws Illegal Argument Exception if enum not found.
        Message.Source.valueOf(message.getSource());
        Message.EventType type = Message.EventType.valueOf(message.getType());
        return templateCache.get(type);
    }

    private JournalTemplateIf populateTemplate(QueueMessage message, JournalTemplate journalTemplate) throws AccountingTemplateException {
        validateAdditionalData(message, journalTemplate);
        Account account = validateAndGetAccount(message, journalTemplate);
        Amount amount = validateAmount(message);
        JournalType jt = validateJournalType(journalTemplate);
        TxType txType = validateTxType(journalTemplate);

        return journalTemplateFactory.makeJournalTemplateInstance(message, journalTemplate, account, amount, jt, txType);
    }

    void validateAdditionalData(QueueMessage message, JournalTemplate journalTemplate) throws AccountingTemplateException {
        String schemaContent = journalTemplate.getAdditionalDataSchema();
        if (StringUtils.isEmpty(schemaContent)) {
            return;
        }

        if (isNull(message.getAdditionalData())) {
            throw new AccountingTemplateException("No additionalData found in message.");
        }

        try {
            Set<ValidationMessage> errors = jsonSchemaFactory.getSchema(schemaContent)
                    .validate(objectMapper.readTree(message.getAdditionalData().toString()));
            if (!errors.isEmpty()) {
                throw new AccountingTemplateException("Invalid message.additionalData:\n" + errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("\n")));
            }
        } catch (JsonProcessingException e) {
            throw new AccountingTemplateException("Failed to validate message.additionalData:", e);
        }
    }

    private Account getAccountByCodeAndTypes(String accountCode, List<Constants.AccountType> accountTypes){
        for(Constants.AccountType accountType : accountTypes) {
            Account account = accountRepo.getByCodeAndType(accountCode, accountType.toString());
            if (nonNull(account)) {
                return account;
            }
        }
        return null;
    }

    // The account in the message can be switched to the corresponding "test" account (if there is the "test" field in additionalData)
    private Account validateAndGetAccount(QueueMessage message, JournalTemplate journalTemplate) throws AccountingTemplateException {
        // validate account (mandatory)
        String accountCode = message.getAccountCode();
        throwExceptionIfNull(accountCode, "Missing mandatory field 'accountCode' in queue message.");

        List<Constants.AccountType> accountTypes = journalTemplate.getAccountTypes();
        Account account = getAccountByCodeAndTypes(accountCode, accountTypes);
        if (switchToTestAccount(message) && nonNull(account) && !account.isTest()) {
            accountCode = accountCode.concat("_TEST");
            account = accountRepo.getByCodeAndType(accountCode, account.getAccountTypeCode());
            if (nonNull(account) && !account.isTest()) {
                throw new AccountingTemplateException(format("Corresponding account is not a test one: %s/%s", accountCode, accountTypes));
            }
        }

        throwExceptionIfNull(account, format("Account in queue message not recognized: %s/%s", accountCode, accountTypes));
        assert account != null;
        log.info("Going to use account with code {} and type {}. Is test: {}.", account.getCode(), account.getAccountTypeCode(), account.isTest());
        return account;
    }

    // For the messages TOKEN_CREATE, TOKEN_CREATE_ACTIVE and TOKEN_CONVERSION a new boolean flag has been added in 'additionalData' called 'test'.
    // If it is TRUE then check if the account in the message is a 'test' account:
    //   yes -> do nothing
    //   no  -> internally switch the account '_TEST' suffix will be added to the account Code
    // We also switch to the corresponding test account if the txReference is in a pre-defined list.
    private boolean switchToTestAccount(QueueMessage message) {
        if (!message.getType().contains("TOKEN_")) {
            return false;
        }
        String txReference = message.getTxReference();
        if (nonNull(txReference)) {
            Set<String> testTokensEnv = Constants.testTokensAll.get(SystemConfig.getInstanceType());
            if (testTokensEnv != null && testTokensEnv.contains(txReference)) {
                log.info("Going to switch to the corresponding test account (the txReference is in the predefined list).");
                return true;
            }
        }
        JsonObject additionalData = message.getAdditionalData();
        JsonElement testFlag = nonNull(additionalData) ? additionalData.get(Message.AdditionalData.test.toString()) : null;
        if(nonNull(testFlag) && testFlag.getAsBoolean()) {
            log.info("Going to switch to the corresponding test account (received 'test: true' flag).");
            return true;
        }
        return false;
    }


    private Amount validateAmount(QueueMessage message) throws AccountingTemplateException {
        // validate amount (optional)
        Amount amount = message.getAmount();
        if (amount != null) {
            String currencyCode = amount.getCurrencyCode();
            throwExceptionIfNull(currencyCode, "Currency code not specified!");
            Currency currency = currencyRepo.get(currencyCode);
            throwExceptionIfNull(currency, format("Unrecognized currency %s", currencyCode));
            amount.setCurrencyId(currency.getCurrencyId());
        }
        return amount;
    }

    private JournalType validateJournalType(JournalTemplate journalTemplate) throws AccountingTemplateException {
        Constants.JournalType jtc = journalTemplate.getJournalType();
        String templateFileName = journalTemplate.getFileName();
        throwExceptionIfNull(jtc, format("JournalType not recognized in template %s.", templateFileName));
        return journalTypeRepo.get(jtc.toString());
    }

    private TxType validateTxType(JournalTemplate journalTemplate) throws AccountingTemplateException {
        String templateFileName = journalTemplate.getFileName();
        Constants.TxType txType = journalTemplate.getTxType();
        throwExceptionIfNull(txType, format("TxType not recognized in template %s.", templateFileName));
        return txTypeRepo.get(txType.toString());
    }

    private void throwExceptionIfNull(Object object, String s) throws AccountingTemplateException {
        if (isNull(object)) {
            throw new AccountingTemplateException(s);
        }
    }
}
