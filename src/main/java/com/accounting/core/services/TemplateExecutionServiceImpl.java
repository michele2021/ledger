package com.accounting.core.services;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.conf.KafkaConfig;
import com.accounting.commons.entities.Account;
import com.accounting.commons.exceptions.AccountingEventException;
import com.accounting.commons.exceptions.WebValidationException;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.objectstorage.ObjectStorage;
import com.accounting.commons.objectstorage.StoredFile;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.commons.utils.AccountingUtils;
import com.accounting.core.dto.JournalInfoDTO;
import com.accounting.core.dto.JournalPostingDTO;
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
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Slf4j
@Singleton
public class TemplateExecutionServiceImpl implements TemplateExecutionService {

    private static final List<String> SUPPORTED_MESSAGE_TYPES = List.of(Message.EventType.PAYMENT_MATCH.toString(),
            Message.EventType.PAYMENT_RECEIVED_CORRECTION.toString(), Message.EventType.CLIENT_FEE_PAYOUT.toString(),
            Message.EventType.JOURNAL_REVERSE.toString(), Message.EventType.PAYMENT_RECEIVED.toString());
    private static final String ALLOWED_TYPES_STRING = "\"" + String.join("\", \"", SUPPORTED_MESSAGE_TYPES) + "\"";
    private KafkaMessageProducer<String, String> kafkaMessageProducer;
    private final TemplateCache templateCache;
    private final AccountRepoIf accountRepo;
    private final CurrencyRepoIf currencyRepo;
    private final boolean isEnabled;
    private final String defaultQueueTopic;
    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    private final ObjectStorage objectStorage;
    private static final long MAX_SIZE = 10 * 1024 * 1024;   // 10 MB, tweak

    @Inject
    public TemplateExecutionServiceImpl(
            KafkaConfig kafkaConfig,
            TemplateCache templateCache,
            ObjectStorage objectStorage,
            KafkaMessageProducer<String, String> kafkaMessageProducer,
            RepositoryHelper repositoryHelper
    ) {
        this.templateCache = templateCache;
        this.objectStorage = objectStorage;
        this.currencyRepo = repositoryHelper.getCurrencyRepo();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
        this.isEnabled = kafkaConfig.isQueueWriteEnabled();
        if (isEnabled) {
            this.kafkaMessageProducer = kafkaMessageProducer;
        } else {
            log.info("Queue disabled in config.");
        }
        // this.gson = SerializationUtil.getGson();
    }

    @Override
    public void executeTemplate(String accountCode, QueueMessageDTO qm) {
        var message = qm.toBuilder()
                .accountCode(accountCode)
                .build();
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

    @Override
    public void postJournal(long accountId, JournalPostingDTO journalPosting) {
        log.debug("postJournal: accountId {}, journal {}", accountId, journalPosting);
        var account = validateAccountAndGet(accountId);
        var message = prepareMessage(account, journalPosting);
        try {
            sendMessageToQueue(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to send message to SQS", e);
            throw new WebApplicationException("Failed to send message to SQS", e);
        }
        log.debug("postJournal(): Success");
    }

    @Override
    public void postJournalWithInfo(long accountId, JournalPostingDTO journalPostingDTO, List<FormDataBodyPart> files) {
        var account = validateAccountAndGet(accountId);
        var message = prepareMessage(account, journalPostingDTO);
        try {
            var journalInfoDTOS = persistJournalInfo(files, journalPostingDTO.description(), account.getAccountCode());
            message = message.toBuilder()
                    .journalInfo(journalInfoDTOS)
                    .build();
            sendMessageToQueue(message);
        } catch (IOException e) {
            log.error("Failed to process and send message to SQS", e);
            throw new WebApplicationException("Failed to process and send message to SQS", e);
        }
        log.debug("postJournalWithInfo(): Success");
    }

    private QueueMessageDTO prepareMessage(Account account, JournalPostingDTO journalPosting) {
        var timestamp = System.currentTimeMillis();
        var uuid = UUID.randomUUID().toString();
        var journalJson = OBJECT_MAPPER.valueToTree(journalPosting);
        var amount = QueueMessageAmountDTO.builder()
                .quantity(BigDecimal.ZERO)
                .currencyCode(Constants.Currencies.EUR.toString())
                .build();
        return QueueMessageDTO.builder()
                .timestamp(timestamp)
                .accountCode(account.getAccountCode())
                .txReference(null)
                .additionalData(journalJson)
                .uuid(uuid)
                .amount(amount)
                .source(Message.Source.FRONT_END_APP.toString())
                .type(Message.EventType.JOURNAL_POST.toString())
                .build();
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
        var serializedMessage = OBJECT_MAPPER.writeValueAsString(kafkaMessage);
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
        JournalTemplate journalTemplate = validateType(message.getType());
        validateSource(message.getSource());
        validateAccount(message.getAccountCode(), journalTemplate.getAccountTypes());
        validateAmount(message);

        validateUuid(message);
        validateTxReference(message);
        validateAdditionalData(message, journalTemplate);
    }

    private JournalTemplate validateType(String type) {
        if (!SUPPORTED_MESSAGE_TYPES.contains(type)) {
            throw validationError("type", "Field value must be one of (" + ALLOWED_TYPES_STRING + ").");
        }
        try {
            return templateCache.get(Message.EventType.valueOf(type));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(String.format("Invalid event type: %s", type), e);
        } catch (AccountingEventException e) {
            throw new WebApplicationException(String.format("Template for %s not found!", type), e);
        }
    }

    private void validateSource(String source) {
        if (!Message.Source.FRONT_END_APP.toString().equals(source)) {
            throw validationError("source", "Field value must be \"FRONT_END_APP\".");
        }
    }

    private void validateAccount(String accountCode, List<Constants.AccountType> accountTypes) {
        // validate account (mandatory)
        validateIsNotEmpty("accountCode", accountCode);

        Account account = null;
        for (Constants.AccountType accountType : accountTypes) {
            var accountTypeCode = accountType.toString();
            account = accountRepo.getByCodeAndType(accountCode, accountTypeCode);
            if (nonNull(account)) {
                break;
            }
        }
        if (account == null) {
            throw validationError("accountCode", format("Account not recognized: %s/%s", accountCode, accountTypes));
        }
    }

    private Account validateAccountAndGet(long accountId) {
        var account = accountRepo.get(accountId);
        if (account == null) {
            throw validationError("accountId", format("Account with id %s does not exists.", accountId));
        }
        return account;
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
        if (SUPPORTED_MESSAGE_TYPES.contains(message.getType())) {
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
            throw validationError("additionalData", "Failed to parse JSON");
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
    private <T> T getAdditionalData(QueueMessageDTO message,
                                    Message.AdditionalData additionalDataType,
                                    Function<JsonNode, T> converter) {
        return converter.apply(message.getAdditionalData().get(additionalDataType.toString()));
    }

    private List<JournalInfoDTO> persistJournalInfo(List<FormDataBodyPart> attachments,
                                                    String journalDescription,
                                                    String companyCode) throws IOException {
        if (StringUtils.isBlank(journalDescription) && AccountingUtils.isEmptyOrNull(attachments)) {
            return List.of();
        }

        var path = journalPath(companyCode);
        var journalInfoDTOs = new ArrayList<JournalInfoDTO>();
        if (StringUtils.isNotBlank(journalDescription)) {
            var ji = persistTextContent(journalDescription, path);
            journalInfoDTOs.add(ji);
        }
        if (attachments != null) {
            var jis = persistAttachments(attachments, path);
            journalInfoDTOs.addAll(jis);
        }
        return journalInfoDTOs;
    }

    private List<JournalInfoDTO> persistAttachments(List<FormDataBodyPart> attachments,
                                                    String path) throws IOException {
        var journalInfoDTOs = new ArrayList<JournalInfoDTO>();

        for (FormDataBodyPart attachment : attachments) {
            var filename = attachment.getContentDisposition().getFileName();
            if (filename == null) continue;

            var mime = attachment.getMediaType().toString();
            long size = attachment.getContentDisposition().getSize(); // -1 if unknown

            try (InputStream in = attachment.getEntityAs(InputStream.class)) {
                StoredFile stored = objectStorage.put(
                        path,
                        in,
                        mime,
                        MAX_SIZE);
                // ---- 3. Build journal info DTO --------------------------------
                var ji = JournalInfoDTO.builder()
                        .id(stored.id())               // UUID from storage
                        .type(resolveType(mime)) // image / pdf / text …
                        .filename(filename)
                        .contentType(mime)
                        .uri(stored.uri())             // e.g. s3://… or pgsql://…
                        .sha256(stored.sha256())
                        .size(size != -1 ? size : stored.size())
                        .build();
                journalInfoDTOs.add(ji);
            }
            // TODO exception handling
        }
        return journalInfoDTOs;
    }

    private JournalInfoDTO persistTextContent(String content, String path) {
        StoredFile stored = objectStorage.putText(path, content, MediaType.TEXT_PLAIN);
        return JournalInfoDTO.builder()
                .id(stored.id())
                .type(resolveType(MediaType.TEXT_PLAIN))
                .filename(null)
                .contentType(MediaType.TEXT_PLAIN)
                .uri(stored.uri())
                .sha256(stored.sha256())
                .size(stored.size())
                .textContent(content)
                .build();
    }

    private String resolveType(String mime) {
        return switch (mime) {
            case "image/png", "image/jpeg" -> "image";
            case "application/pdf" -> "pdf";
            case "text/plain" -> "text";
            default -> "doc";
        };
    }

    private String journalPath(String companyCode) {
        return "journals/" + companyCode;
    }

}
