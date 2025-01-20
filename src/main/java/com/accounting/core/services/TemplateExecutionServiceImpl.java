package com.accounting.core.services;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.conf.KafkaConfig;
import com.accounting.commons.entities.Account;
import com.accounting.commons.exceptions.WebValidationException;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.commons.utils.MappingUtils;
import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.processing.TemplateCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Slf4j
@Singleton
public class TemplateExecutionServiceImpl implements TemplateExecutionService {

    private static final List<String> ALLOWED_TYPES = List.of(Message.EventType.PAYMENT_RECEIVED.toString(),
            Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString(), Message.EventType.CLIENT_FEE_PAYOUT.toString());
    private static final String ALLOWED_TYPES_STRING = "\"" + String.join("\", \"", ALLOWED_TYPES) + "\"";


    private KafkaMessageProducer<String, String> kafkaMessageProducer;
    private final TemplateCache templateCache;
    private final AccountRepoIf accountRepo;
    private final CurrencyRepoIf currencyRepo;

    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    private final boolean isEnabled;
    private final String defaultQueueTopic;

    @Inject
    public TemplateExecutionServiceImpl(
            AWSConfig awsConfig,
            KafkaConfig kafkaConfig,
            TemplateCache templateCache,
            KafkaMessageProducer<String, String> kafkaMessageProducer,
            RepositoryHelper repositoryHelper
    ) {
        this.templateCache = templateCache;
        this.currencyRepo = repositoryHelper.getCurrencyRepo();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
        this.isEnabled = awsConfig.isQueueEnabled();
        if (isEnabled) {
            this.kafkaMessageProducer = kafkaMessageProducer;
        } else {
            log.info("SQS queue disabled in config.");
        }
    }


    public void executeTemplate(QueueMessageDTO message) {
        log.debug("executeTemplate(): Start: {}", message);
        validate(message);
        try {
            sendMessageToQueue(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to send message to SQS", e);
            throw new WebApplicationException("Failed to send message to SQS", e);
        }
        log.debug("executeTemplate(): Success");
    }

    private void sendMessageToQueue(QueueMessageDTO message) throws JsonProcessingException {
        if (!isEnabled) {
            return;
        }
        log.debug("Message to send: {}", message);
        var m = OBJECT_MAPPER.writeValueAsString(message.toBuilder()
                        .timestamp(System.currentTimeMillis())
                        .build());

        var kafkaMessage = KafkaMessage.builder().key("MAIN").value(m).build();
        var serializedMessage = MappingUtils.OBJECT_MAPPER.writeValueAsString(kafkaMessage);
        var response = kafkaMessageProducer.send(defaultQueueTopic, "MAIN", serializedMessage);
        log.debug("Message sent: {}", response);
    }


    private WebValidationException validationError(String fieldName, String validationMessage) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(fieldName)) {
            sb.append("Field message.").append(fieldName).append(" is invalid! ");
        }
        sb.append(validationMessage);

        if (log.isTraceEnabled()) {
            log.trace("Validation error: {}", sb);
        }
        return new WebValidationException(sb.toString());
    }

    private void validateIsNotNull(String fieldName, Object fieldValue) {
        if (Objects.isNull(fieldValue)) {
            throw validationError(fieldName, "Field must have a value.");
        }
    }

    private void validateIsNotEmpty(String fieldName, String fieldValue) {
        if (StringUtils.isEmpty(fieldValue)) {
            throw validationError(fieldName, "Field must have a value.");
        }
    }

    private void validateIsNull(String fieldName, Object fieldValue) {
        if (!Objects.isNull(fieldValue)) {
            throw validationError(fieldName, "Field must have no value.");
        }
    }

    private void validate(QueueMessageDTO message) {
        if (Objects.isNull(message)) {
            throw validationError(null, "No message");
        }
        JournalTemplate journalTemplate = validateType(message);
        validateSource(message);
        validateAccount(message, journalTemplate);
        validateAmount(message);

        validateUuid(message);
        validateTxReference(message);
        validateAdditionalData(message, journalTemplate);

    }

    private JournalTemplate validateType(QueueMessageDTO message) {
        if (!ALLOWED_TYPES.contains(message.getType())) {
            throw validationError("type", "Field value must be one of (" + ALLOWED_TYPES_STRING + ").");
        }
        try {
            return templateCache.get(Message.EventType.valueOf(message.getType()));
        } catch (Exception e) {
            throw new WebApplicationException(String.format("Template for %s not found!", message.getType()));
        }
    }

    private void validateSource(QueueMessageDTO message) {
        if (!Message.Source.FRONT_END_APP.toString().equals(message.getSource())) {
            throw validationError("source", "Field value must be \"FRONT_END_APP\".");
        }
    }

    private void validateAccount(QueueMessageDTO message, JournalTemplate journalTemplate) {
        // validate account (mandatory)
        validateIsNotEmpty("accountCode", message.getAccountCode());

        Account account = null;
        for(Constants.AccountType accountType : journalTemplate.getAccountTypes()) {
            String accountTypeCode = accountType.toString();
            account = accountRepo.getByCodeAndType(message.getAccountCode(), accountTypeCode);
            if (nonNull(account)) {
                break;
            }
        }
        if (Objects.isNull(account)) {
            throw validationError("accountCode", format("Account not recognized: %s/%s", message.getAccountCode(), journalTemplate.getAccountTypes()));
        }
    }

    private void validateAmount(QueueMessageDTO message) {
        boolean amountIsMandatory = Message.EventType.PAYMENT_RECEIVED.toString().equals(message.getType()) ||
                Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString().equals(message.getType()) ||
                Message.EventType.CLIENT_FEE_PAYOUT.toString().equals(message.getType());

        QueueMessageAmountDTO amount = message.getAmount();
        if (amountIsMandatory) {
            validateIsNotNull("amount", amount);
        }

        if (!Objects.isNull(amount)) {
            if (amountIsMandatory) {
                validateIsNotNull("amount.quantity", amount.getQuantity());
                validateIsNotEmpty("amount.currencyCode", amount.getCurrencyCode());
            }

            if (Objects.isNull(currencyRepo.get(amount.getCurrencyCode()))) {
                throw validationError("amount.currencyCode", format("Amount currency not recognized: %s", amount.getCurrencyCode()));
            }
        }

    }

    private void validateUuid(QueueMessageDTO message) {
        if (Message.EventType.PAYMENT_RECEIVED.toString().equals(message.getType()) ||
                Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString().equals(message.getType()) ||
                Message.EventType.CLIENT_FEE_PAYOUT.toString().equals(message.getType())) {
            validateIsNull("uuid", message.getUuid());
        }
    }

    private void validateTxReference(QueueMessageDTO message) {
        if (Message.EventType.PAYMENT_RECEIVED.toString().equals(message.getType()) ||
                Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString().equals(message.getType()) ||
                Message.EventType.CLIENT_FEE_PAYOUT.toString().equals(message.getType())) {
            validateIsNull("txReference", message.getTxReference());
        }
    }

    private void validateAdditionalData(QueueMessageDTO message, JournalTemplate journalTemplate) {
        String schemaContent = journalTemplate.getAdditionalDataSchema();
        if (StringUtils.isEmpty(schemaContent)) {
            validateIsNull("additionalData", message.getAdditionalData());
            return;
        }

        validateIsNotNull("additionalData", message.getAdditionalData());
        if (Objects.isNull(message.getAdditionalData()) || message.getAdditionalData().isNull()) {
            throw validationError("additionalData", "Field must have a value.");
        }

        try {
            Set<ValidationMessage> errors = jsonSchemaFactory.getSchema(schemaContent)
                    .validate(OBJECT_MAPPER.readTree(message.getAdditionalData().toString()));
            if (!errors.isEmpty()) {
                throw validationError("additionalData",
                        format("AdditionalData does not conform to the schema. Validation errors:%n%s", errors.stream()
                                .map(ValidationMessage::getMessage)
                                .collect(Collectors.joining("%n")))
                );
            }
        } catch (JsonProcessingException e) {
            throw validationError("additionalData","Failed to parse JSON");
        }

        if (Message.EventType.PAYMENT_RECEIVED.toString().equals(message.getType()) ||
                Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString().equals(message.getType())) {
            //expect to have valid bank account
            String bankAccountCode = getAdditionalData(message, Message.AdditionalData.bankAccountCode, JsonNode::asText);
            validateIsNotEmpty("additionalData.bankAccountCode", bankAccountCode);
            if (Objects.isNull(accountRepo.getByCodeAndType(bankAccountCode, Constants.AccountType.BankAccount.toString()))) {
                throw validationError("additionalData.bankAccountCode", format("Bank account code not recognized: %s", bankAccountCode));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T getAdditionalData(QueueMessageDTO message, Message.AdditionalData additionalDataType, Function<JsonNode, T> converter) {
        return converter.apply(message.getAdditionalData().get(additionalDataType.toString()));
    }

}
