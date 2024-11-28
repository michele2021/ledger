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
class TokenCreateAdditionalDataValidationTest {


    @SuppressWarnings("unused")
    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public MessageProcessor messageProcessor;


    @Test
    public void shouldValidate() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate();
        JournalTemplate journalTemplate = TestJournalTemplate.tokenCreate();

        //when
        messageProcessor.validateAdditionalData(message, journalTemplate);

        //then no exception means is valid

    }

    @Test
    public void shouldFailIfNoAdditionalData() {
        //given
        QueueMessage message = TestQueueMessage.tokenCreate();
        message.setAdditionalData(null);
        JournalTemplate journalTemplate = TestJournalTemplate.tokenCreate();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");

    }

    @Test
    public void shouldFailIfActivatingLocationRefIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateJson(),
                json -> json.replace("\"activatingLocationRef\" : \"POS-LOC2\"", "\"activatingLocationRef\" : 458745"));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenCreate();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.activatingLocationRef: integer found, string expected");

    }

    @Test
    public void shouldNotFailIfActivatingLocationRefIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateJson(),
                json -> json.replace("\"activatingLocationRef\" : \"POS-LOC2\"", ""),
                json -> json.replace("\"publishableId\" : \"4587458\",", "\"publishableId\" : \"4587458\""));

        JournalTemplate journalTemplate = TestJournalTemplate.tokenCreate();

        //when
        Assertions.assertDoesNotThrow(() -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then no exception means is valid
    }
}