package com.accounting.core.processing;

import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.testUtils.TestJournalTemplate;
import com.accounting.core.testUtils.TestQueueMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class ClientOrderReceivedAdditionalDataValidationTest {


    @SuppressWarnings("unused")
    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public MessageProcessor messageProcessor;


    @Test
    public void shouldValidate() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.clientOrderReceived();
        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        messageProcessor.validateAdditionalData(message, journalTemplate);

        //then no exception means is valid

    }

    @Test
    public void shouldFailIfNoAdditionalData() {
        //given
        QueueMessage message = TestQueueMessage.clientOrderReceived();
        message.setAdditionalData(null);
        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");

    }

    @Test
    public void shouldFailIfSkuIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"sku\" : \"dfsdf234\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.sku: is missing but it is required");

    }

    @Test
    public void shouldFailIfSkuIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"sku\" : \"dfsdf234\",", "\"sku\" : 5487545,"));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.sku: integer found, string expected");

    }

    @Test
    public void shouldFailIfCardProgramIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"cardProgram\" : \"EPAY\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.cardProgram: is missing but it is required");

    }

    @Test
    public void shouldFailICardProgramIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"cardProgram\" : \"EPAY\",", "\"cardProgram\" : 5487545,"));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.cardProgram: integer found, string expected");

    }

    @Test
    public void shouldFailIfOrderTitleIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"orderTitle\" : \"some order\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.orderTitle: is missing but it is required");

    }

    @Test
    public void shouldFailIfOrderTitleIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"orderTitle\" : \"some order\",", "\"orderTitle\" : 5487545,"));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.orderTitle: integer found, string expected");

    }

    @Test
    public void shouldFailIfOrderIdIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"orderId\" : 5487545,", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.orderId: is missing but it is required");

    }

    @Test
    public void shouldFailIfOrderIdIsNotInteger() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"orderId\" : 5487545,", "\"orderId\" : \"5487545\","));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.orderId: string found, integer expected");

    }

    @Test
    public void shouldFailIfTokensIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.clientOrderReceived();
        message.setAdditionalData(new Gson().fromJson(TestQueueMessage.clientOrderReceivedAdditionalDataNoTokensJson(), JsonObject.class));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens: is missing but it is required");

    }

    @Test
    public void shouldFailIfTokensNotArray() {
        //given
        QueueMessage message = TestQueueMessage.clientOrderReceived();
        message.setAdditionalData(new Gson().fromJson(
                TestQueueMessage.clientOrderReceivedAdditionalDataTokensEmptyJson().replace("\"tokens\" : []", "\"tokens\" : 1234"),
                JsonObject.class));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens: integer found, array expected");

    }

    @Test
    public void shouldFailIfTokensEmpty() {
        //given
        QueueMessage message = TestQueueMessage.clientOrderReceived();
        message.setAdditionalData(new Gson().fromJson(TestQueueMessage.clientOrderReceivedAdditionalDataTokensEmptyJson(), JsonObject.class));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens: there must be a minimum of 1 items in the array");

    }

    @Test
    public void shouldFailIfTokenQuantityOrderIdIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"quantity\" : 1,", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].quantity: is missing but it is required");

    }

    @Test
    public void shouldFailIfTokenQuantityIsNotInteger() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"quantity\" : 1,", "\"quantity\" : \"1\","));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].quantity: string found, integer expected");

    }

    @Test
    public void shouldFailIfTokenNameOrderIdIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"name\" : \"a name\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].name: is missing but it is required");

    }

    @Test
    public void shouldFailIfTokenNameIdIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"name\" : \"a name\",", "\"name\" : 1,"));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].name: integer found, string expected");

    }


    @Test
    public void shouldFailIfTokenEmailIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"email\" : \"someone@somewhere.com\"", ""),
                json -> json.replace("\"a name\",", "\"a name\""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].email: is missing but it is required");

    }

    @Test
    public void shouldFailIfTokenEmailIdIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderReceivedJson(),
                json -> json.replace("\"email\" : \"someone@somewhere.com\"", "\"email\" : 1"));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage())
                .contains("Invalid message.additionalData:\n")
                .contains("$.tokens[0].email: integer found, string expected");

    }



}