package com.accounting.core.processing;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Currency;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingEventException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.templates.JournalTemplateFactory;
import com.accounting.core.processing.templates.JournalTemplateIf;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.testUtils.TestJournalTemplate;
import com.accounting.core.testUtils.TestQueueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ErrorMessageType;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageProcessorTest {

    @Mock
    public TemplateCache templateCache;

    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @Mock
    public JsonSchemaFactory jsonSchemaFactory;

    @Mock
    public JsonSchema jsonSchema;

    @Mock
    public JournalTemplateIf journalTemplateIf;

    @Mock
    public JournalTemplateFactory journalTemplateFactory;

    @Mock
    Account originAccount;

    @Mock
    Account testAccount;

    @InjectMocks
    public MessageProcessor subject;

    public static class TestErrorMessageType implements ErrorMessageType {

        @Override
        public String getErrorCode() {
            return "Test error code";
        }

        public MessageFormat getMessageFormat() {
            return new MessageFormat(
                    "Test message \"{1}\" .");
        }
    }


    @SuppressWarnings("unused")
    static Stream<Arguments> shouldProcessTemplate() {
        return Stream.of(
                Arguments.of(TestQueueMessage.tokenCreate(), TestJournalTemplate.orderReceived()),
                Arguments.of(TestQueueMessage.tokenActivate(), TestJournalTemplate.paymentReceived())
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldProcessTemplate(QueueMessage message, JournalTemplate journalTemplate) throws Exception {
        //given
        final Account account = new Account();
        final Currency currency = new Currency();
        currency.setCurrencyId(456);
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();

        when(templateCache.get(any())).thenReturn(journalTemplate);

        when(repositoryHelper.getAccountRepo().getByCodeAndType(anyString(), anyString())).thenReturn(account);
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);
        when(repositoryHelper.getJournalTypeRepo().get(anyString())).thenReturn(journalType);
        when(repositoryHelper.getTxTypeRepo().get(anyString())).thenReturn(txType);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

        Field journalTemplateFactoryField = subject.getClass().getDeclaredField("journalTemplateFactory");
        journalTemplateFactoryField.setAccessible(true);
        journalTemplateFactoryField.set(subject, journalTemplateFactory);

        when(journalTemplateFactory.makeJournalTemplateInstance(any(), any(), any(), any(), any(), any())).thenReturn(journalTemplateIf);


        //when
        subject.process(message);


        //then
        verify(templateCache).get(eq(journalTemplate.getEventType()));

        //validateAdditionalData()
        if (Objects.isNull(message.additionalData())) {
            verify(jsonSchemaFactory, never()).getSchema(anyString());
        } else {
            verify(jsonSchemaFactory).getSchema(eq(journalTemplate.getAdditionalDataSchema()));
            verify(jsonSchema).validate(any(JsonNode.class));
        }

        //validateAccount
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq(message.accountCode()), eq(journalTemplate.getAccountTypes().getFirst().toString()));

        //validateAmount
        if (Objects.isNull(message.amount())) {
            verify(repositoryHelper.getCurrencyRepo(), never()).get(anyLong());
        } else {
            verify(repositoryHelper.getCurrencyRepo()).get(eq(message.amount().getCurrencyCode()));
            assertThat(message.amount().getCurrencyId()).isEqualTo(currency.getCurrencyId());
        }

        //validateJournalType
        verify(repositoryHelper.getJournalTypeRepo()).get(eq(journalTemplate.getJournalType().toString()));

        //validateTxType
        verify(repositoryHelper.getTxTypeRepo()).get(eq(journalTemplate.getTxType().toString()));

        verify(journalTemplateFactory).makeJournalTemplateInstance(
                eq(message),
                eq(journalTemplate),
                eq(account),
                eq(message.amount()),
                eq(journalType),
                eq(txType)
        );

        verify(journalTemplateIf).process();
    }


    @Test
    void shouldFailIfUnknownMessageSource() {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate("EPAY", "#$%^&*(UNKNOWN)", "TOKEN_CREATE");


        //when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("No enum constant com.accounting.commons.Message.Source.#$%^&*(UNKNOWN)");
    }

    @Test
    void shouldFailIfUnknownEventType() {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate("EPAY", "LEDGER_API", "\"#$%^&*(UNKNOWN)\"");

        //when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("No enum constant com.accounting.commons.Message.EventType.#$%^&*(UNKNOWN)");
    }

    @Test
    void shouldFailIfTemplateCacheFails() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate();

        when(templateCache.get(any())).thenAnswer(i -> {
            throw new AccountingEventException("#$%^&*(test)");
        });


        //when
        AccountingEventException exception = assertThrows(AccountingEventException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("#$%^&*(test)");
    }

    @Test
    void shouldFailOnInvalidAdditionalData() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateJson(),
                json -> json.replace("publishableId", "NOT_publishableId"));

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());


        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

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
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat("Invalid message.additionalData:\n" +
                "Test message \"{1}\" .").isEqualTo(exception.getMessage());
    }

    @Test
    void shouldFailOnInvalidAdditionalDataSchema() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate();

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());


        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

        when(jsonSchema.validate(any())).thenAnswer(i -> {
            throw new JsonProcessingException("test") {
            };
        });

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getCause()).isInstanceOf(JsonProcessingException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("test");
        assertThat(exception.getMessage()).isEqualTo("Failed to validate message.additionalData: Exception: com.accounting.core.processing.MessageProcessorTest$1: test");
    }

    @Test
    void shouldFailIfNoAdditionalDataButSchemaExists() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate(false);

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());


        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");
    }

    @Test
    void shouldFailIfOnNullAccountCode() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate(null, "LEDGER_API", "TOKEN_CREATE");

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("Missing mandatory field 'accountCode' in queue message.");
    }

    @Test
    void shouldFailOnInvalidAccountCode() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate();

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());

        when(repositoryHelper.getAccountRepo().getByCodeAndType(anyString(), anyString())).thenReturn(null);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("Account in queue message not recognized: CMPZERO/[Company, Client]");
    }

    @Test
    void shouldFailOnNullCurrency() throws Exception {
        //given
        final Account account = new Account();

        QueueMessage message = TestQueueMessage.tokenCreate();
        message.amount().setCurrencyCode(null);

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());

        when(repositoryHelper.getAccountRepo().getByCodeAndType(anyString(), anyString())).thenReturn(account);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("Currency code not specified!");
    }

    @Test
    void shouldFailOnInvalidCurrency() throws Exception {
        //given
        final Account account = new Account();

        QueueMessage message = TestQueueMessage.tokenCreate();

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());

        when(repositoryHelper.getAccountRepo().getByCodeAndType(anyString(), anyString())).thenReturn(account);
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(null);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("Unrecognized currency EUR");
    }

    @Test
    void shouldFailIfTemplateFactoryFail() throws Exception {
        //given
        final Account account = new Account();
        final Currency currency = new Currency();
        currency.setCurrencyId(456);
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();

        QueueMessage message = TestQueueMessage.tokenCreate();

        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());

        when(repositoryHelper.getAccountRepo().getByCodeAndType(anyString(), anyString())).thenReturn(account);
        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);
        when(repositoryHelper.getJournalTypeRepo().get(anyString())).thenReturn(journalType);
        when(repositoryHelper.getTxTypeRepo().get(anyString())).thenReturn(txType);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

        Field journalTemplateFactoryField = subject.getClass().getDeclaredField("journalTemplateFactory");
        journalTemplateFactoryField.setAccessible(true);
        journalTemplateFactoryField.set(subject, journalTemplateFactory);

        when(journalTemplateFactory.makeJournalTemplateInstance(any(), any(), any(), any(), any(), any())).thenAnswer(i -> {
            throw new AccountingTemplateException("@#%$#$%(TEST)");
        });


        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));

        //then
        assertThat(exception.getMessage()).isEqualTo("@#%$#$%(TEST)");
    }

    @Test
    void shouldSwitchToTestAccount() throws Exception {
        //given
        String originAccountCode = "CMPZERO";
        String originAccountTypeCode = "Company";
        String testAccountCode = "CMPZERO_TEST";
        final Currency currency = new Currency();
        currency.setCurrencyId(456);
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();
        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        QueueMessage message = TestQueueMessage.tokenCreateWithTestFlag(true);
        // message.setAccountCode("CMPZERO");

        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);
        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());
        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(originAccountCode), anyString())).thenReturn(originAccount);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(testAccountCode), anyString())).thenReturn(testAccount);
        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(repositoryHelper.getJournalTypeRepo().get(anyString())).thenReturn(journalType);
        when(repositoryHelper.getTxTypeRepo().get(anyString())).thenReturn(txType);
        when(originAccount.isTest()).thenReturn(false);
        when(originAccount.getAccountTypeCode()).thenReturn(originAccountTypeCode);
        when(testAccount.isTest()).thenReturn(true);


        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

        Field journalTemplateFactoryField = subject.getClass().getDeclaredField("journalTemplateFactory");
        journalTemplateFactoryField.setAccessible(true);
        journalTemplateFactoryField.set(subject, journalTemplateFactory);

        when(journalTemplateFactory.makeJournalTemplateInstance(any(), any(), any(), any(), any(), any())).thenReturn(journalTemplateIf);

        //when
        subject.process(message);

        verify(journalTemplateFactory).makeJournalTemplateInstance(
                eq(message),
                eq(journalTemplate),
                eq(testAccount),
                eq(message.amount()),
                eq(journalType),
                eq(txType)
        );

        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq(message.accountCode()), anyString());
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq(testAccountCode), anyString());

    }

// TODO reenable once it is possible to set the account code at the moment the message is created
//    @Test
//    void shouldFailIfCorrespondingAccountIsNotTestOne() throws Exception {
//        //given
//        String originAccountCode = "EPAY";
//        String originAccountTypeCode = "Distributor";
//        String testAccountCode = "EPAY_TEST";
//        final Currency currency = new Currency();
//        currency.setCurrencyId(456);
//        final JournalType journalType = new JournalType();
//        final TxType txType = new TxType();
//        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();
//
//        QueueMessage message = TestQueueMessage.tokenCreateWithTestFlag(true);
//        message.setAccountCode("EPAY");
//
//        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);
//        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());
//        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(originAccountCode), anyString())).thenReturn(originAccount);
//        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(testAccountCode), anyString())).thenReturn(testAccount);
//        when(templateCache.get(any())).thenReturn(journalTemplate);
//        when(repositoryHelper.getJournalTypeRepo().get(anyString())).thenReturn(journalType);
//        when(repositoryHelper.getTxTypeRepo().get(anyString())).thenReturn(txType);
//        when(originAccount.isTest()).thenReturn(false);
//        when(originAccount.getAccountTypeCode()).thenReturn(originAccountTypeCode);
//        when(testAccount.isTest()).thenReturn(false);
//
//        //when
//        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> subject.process(message));
//
//        //then
//        assertThat(exception.getMessage()).isEqualTo(format("Corresponding account is not a test one: %s/%s", testAccountCode, journalTemplate.getAccountTypes()));
//
//    }

    //Origin account is already test one so there is no need to switch
    @Test
    void shouldNotSwitchToTestAccount() throws Exception {
        //given
        String originAccountCode = "EPAY";
        String testAccountCode = "EPAY_TEST";
        final Currency currency = new Currency();
        currency.setCurrencyId(456);
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();
        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        QueueMessage message = TestQueueMessage.tokenCreateWithTestFlag(true);
        // message.setAccountCode("EPAY");

        when(repositoryHelper.getCurrencyRepo().get(anyString())).thenReturn(currency);
        when(templateCache.get(any())).thenReturn(TestJournalTemplate.orderReceived());
        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(originAccountCode), anyString())).thenReturn(originAccount);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(eq(testAccountCode), anyString())).thenReturn(testAccount);
        when(templateCache.get(any())).thenReturn(journalTemplate);
        when(repositoryHelper.getJournalTypeRepo().get(anyString())).thenReturn(journalType);
        when(repositoryHelper.getTxTypeRepo().get(anyString())).thenReturn(txType);
        when(originAccount.isTest()).thenReturn(true);

        Field jsonSchemaFactoryField = subject.getClass().getDeclaredField("jsonSchemaFactory");
        jsonSchemaFactoryField.setAccessible(true);
        jsonSchemaFactoryField.set(subject, jsonSchemaFactory);
        if (Objects.nonNull(message.additionalData())) {
            when(jsonSchemaFactory.getSchema(anyString())).thenReturn(jsonSchema);
        }

        Field journalTemplateFactoryField = subject.getClass().getDeclaredField("journalTemplateFactory");
        journalTemplateFactoryField.setAccessible(true);
        journalTemplateFactoryField.set(subject, journalTemplateFactory);

        when(journalTemplateFactory.makeJournalTemplateInstance(any(), any(), any(), any(), any(), any())).thenReturn(journalTemplateIf);

        //when
        subject.process(message);

        verify(journalTemplateFactory).makeJournalTemplateInstance(
                eq(message),
                eq(journalTemplate),
                eq(originAccount),
                eq(message.amount()),
                eq(journalType),
                eq(txType)
        );

        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq(message.accountCode()), anyString());
        verifyNoMoreInteractions(repositoryHelper.getAccountRepo());

    }

}