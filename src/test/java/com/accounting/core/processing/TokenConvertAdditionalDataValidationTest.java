package com.accounting.core.processing;

import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.testUtils.TestJournalTemplate;
import com.accounting.core.testUtils.TestQueueMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class TokenConvertAdditionalDataValidationTest {


    @SuppressWarnings("unused")
    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public MessageProcessor messageProcessor;


    @Test
    public void shouldValidate() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenConvert();
        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        messageProcessor.validateAdditionalData(message, journalTemplate);

        //then no exception means is valid

    }

    @Test
    public void shouldFailIfNoAdditionalData() {
        //given
        QueueMessage message = TestQueueMessage.tokenConvert();
        message.setAdditionalData(null);
        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");

    }

    @Test
    public void shouldFailIfPrepaidCardExternalIdIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenConvertJson(),
                json -> json.replace("\"prepaidCardExternalId\" : \"ExternalId\"", ""),
                json -> json.replace("\"issuer\" : \"Monavate\",", "\"issuer\" : \"Monavate\""));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.prepaidCardExternalId: is missing but it is required");

    }

    @Test
    public void shouldFailIfPrepaidCardExternalIdIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenConvertJson(),
                json -> json.replace("\"prepaidCardExternalId\" : \"ExternalId\"", "\"prepaidCardExternalId\" : 123123"));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.prepaidCardExternalId: integer found, string expected");

    }

    @Test
    public void shouldFailIfIssuerIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenConvertJson(),
                json -> json.replace("\"issuer\" : \"Monavate\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.issuer: is missing but it is required");

    }

    @Test
    public void shouldFailIfIssuerIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenConvertJson(),
                json -> json.replace("\"issuer\" : \"Monavate\"", "\"issuer\" : 458745"));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenConvert();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.issuer: integer found, string expected");

    }

}