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
class ClientOrderCancelledAdditionalDataValidationTest {


    @SuppressWarnings("unused")
    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public MessageProcessor messageProcessor;


    @Test
    public void shouldValidate() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.clientOrderCancelled();
        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderCancelled();

        //when
        messageProcessor.validateAdditionalData(message, journalTemplate);

        //then no exception means is valid

    }

    @Test
    public void shouldFailIfNoAdditionalData() {
        //given
        QueueMessage message = TestQueueMessage.clientOrderCancelled();
        message.setAdditionalData(null);
        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderCancelled();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");

    }

    @Test
    public void shouldFailIfOrderIdIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderCancelledJson(),
                json -> json.replace("\"orderId\" : 5487545", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderCancelled();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.orderId: is missing but it is required");

    }

    @Test
    public void shouldFailIfOrderIdIsNotNumber() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.clientOrderCancelledJson(),
                json -> json.replace("\"orderId\" : 5487545", "\"orderId\" : \"5487545\""));

        JournalTemplate journalTemplate = TestJournalTemplate.clientOrderCancelled();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("Invalid message.additionalData:\n" +
                "$.orderId: string found, integer expected");

    }

}