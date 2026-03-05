package com.accounting.core.services;

import com.accounting.commons.Constants;
import com.accounting.commons.conf.KafkaConfig;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Currency;
import com.accounting.commons.exceptions.WebValidationException;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.objectstorage.ObjectStorage;
import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.processing.MessageProcessorTest;
import com.accounting.core.processing.TemplateCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.accounting.commons.Message.EventType.PAYMENT_RECEIVED;
import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;
import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateExecutionServiceImplTest {

    static final String PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
                "properties": {
                  "bankAccountCode": {
                    "type": "string"
                  }
                },
                "required": [
                  "bankAccountCode"
                ]
              }""";

    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @Mock
    TemplateCache templateCache;

    @Mock
    KafkaConfig kafkaConfig;

    @Mock
    ObjectStorage objectStorage;

    @Mock
    @SuppressWarnings("rawtypes")
    KafkaMessageProducer kafkaMessageProducer;

    TemplateExecutionServiceImpl subject;

    ObjectMapper objectMapper = OBJECT_MAPPER;

    @Mock
    JournalTemplate journalTemplate;

    @Mock
    JsonSchemaFactory jsonSchemaFactory;

    @Mock
    JsonSchema jsonSchema;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        subject = new TemplateExecutionServiceImpl(kafkaConfig, templateCache, objectStorage, kafkaMessageProducer, repositoryHelper);
    }


    @Test
    @SuppressWarnings("unchecked")
    void shouldExecuteTemplate() throws Exception {
        //given
        when(kafkaConfig.isQueueWriteEnabled()).thenReturn(true);
        when(kafkaConfig.getDefaultQueueTopic()).thenReturn("test-topic");
        subject = new TemplateExecutionServiceImpl(kafkaConfig, templateCache, objectStorage, kafkaMessageProducer, repositoryHelper);

        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);

        when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        Set<ValidationMessage> errors = Collections.emptySet();
        when(jsonSchema.validate(any())).thenReturn(errors);

        Account bankAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("BankAccount"))).thenReturn(bankAccount);


        //when
        subject.executeTemplate("accountCode", message);


        //then
        verify(templateCache).get(eq(PAYMENT_RECEIVED));
        verify(journalTemplate).getAccountTypes();
        verify(journalTemplate).getAdditionalDataSchema();
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq("accountCode"), eq("Company"));
        verify(repositoryHelper.getCurrencyRepo()).get(eq("ABC"));
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq("bankAccountCode"), eq("BankAccount"));

        verify(kafkaMessageProducer).send(eq("test-topic"), eq("MAIN"), any(String.class));
    }

    static WebValidationException assertValidation(Executable executable) {
        return assertThrows(WebValidationException.class, executable);
    }

    @Test
    void shouldExecuteTemplateValidateMessageNotNull() {
        //given

        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", null));

        //then
        assertThat(validationError.getMessage()).contains("No message");


    }

    @Test
    void shouldExecuteTemplateValidateType() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("xxxxx")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.type")
                .contains("Field value must be one of ")
                .contains("PAYMENT_RECEIVED");

    }

    @Test
    void shouldExecuteTemplateValidateSource() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("xxxx")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);


        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.source")
                .contains("Field value must be \"FRONT_END_APP\".");

    }

    @Test
    void shouldExecuteTemplateValidateAccountCodeNotNull() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode(null)
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);

        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate(null, message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.accountCode")
                .contains("Field must have a value.");
    }

    @Test
    void shouldExecuteTemplateValidateAccountCodeNotEmpty() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);

        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.accountCode")
                .contains("Field must have a value.");
    }

    @Test
    void shouldExecuteTemplateValidateAccountExists() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(null);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.accountCode")
                .contains("Account not recognized: accountCode/[Company]");
    }


    @Test
    void shouldExecuteTemplateValidateAmountNotNull() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(null)
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.amount")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAmountNotNullIfMandatory() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(null)
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.amount")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAmountQuantityNotNullIfMandatory() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(null)
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.amount.quantity")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAmountCurrencyNotEmptyIfMandatory() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode(null)
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.amount.currencyCode")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAmountCurrencyExists() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(null);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.amount.currencyCode")
                .contains("Amount currency not recognized: ABC");

    }

    @Test
    void shouldExecuteTemplateValidateUuidIsNull() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid("uuid")
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.uuid")
                .contains("Field must have no value.");

    }

    @Test
    void shouldExecuteTemplateValidateTxReferenceIsNull() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference("txReference")
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.txReference")
                .contains("Field must have no value.");

    }


    @Test
    void shouldExecuteTemplateValidateAdditionalDataIsNullIfNotExpected() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(null);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData")
                .contains("Field must have no value.");

    }

    @Test
    void shouldExecuteTemplateValidateAdditionalDataIsNotNullIfExpected() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(null)
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAdditionalDataIsNotEmptyIfExpected() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("null"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateAdditionalDataValidateSchema() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);

        when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        Set<ValidationMessage> errors = new HashSet<>();
        MessageProcessorTest.TestErrorMessageType errorMessageType = new MessageProcessorTest.TestErrorMessageType();
        ValidationMessage validationMessage = ValidationMessage.builder()
                .type("type")
                .code(errorMessageType.getErrorCode())
                .format(errorMessageType.getMessageFormat())
                .build();
        errors.add(validationMessage);
        when(jsonSchema.validate(any())).thenReturn(errors);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData")
                .contains("AdditionalData does not conform to the schema.")
                .contains("Test message \"{1}\"");
    }


    @Test
    void shouldExecuteTemplateValidateBankAssountCodeIsNotEmpty() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);

        when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        Set<ValidationMessage> errors = Collections.emptySet();
        when(jsonSchema.validate(any())).thenReturn(errors);

        Account bankAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("BankAccount"))).thenReturn(bankAccount);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData.bankAccountCode")
                .contains("Field must have a value.");

    }

    @Test
    void shouldExecuteTemplateValidateBankAccountExists() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("PAYMENT_RECEIVED")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid(null)
                .txReference(null)
                .additionalData(objectMapper.readTree("{ \"bankAccountCode\" : \"bankAccountCode\" }"))
                .source("FRONT_END_APP")
                .build();

        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(journalTemplate.getAccountTypes()).thenReturn(List.of(Constants.AccountType.Company));
        when(journalTemplate.getAdditionalDataSchema()).thenReturn(PAYMENT_RECEIVED_ADDITIONAL_DATA_SCHEMA);

        Account cardAccount = Account.builder().build();
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("Company"))).thenReturn(cardAccount);

        Currency currency = Currency.builder().build();
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);

        when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        Set<ValidationMessage> errors = Collections.emptySet();
        when(jsonSchema.validate(any())).thenReturn(errors);

        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), eq("BankAccount"))).thenReturn(null);


        //when
        WebValidationException validationError = assertValidation(() -> subject.executeTemplate("accountCode", message));


        //then
        assertThat(validationError.getMessage())
                .contains("message.additionalData.bankAccountCode")
                .contains("Bank account code not recognized: bankAccountCode");

    }

}
