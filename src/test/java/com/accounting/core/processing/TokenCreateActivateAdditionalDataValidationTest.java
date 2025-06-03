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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class TokenCreateActivateAdditionalDataValidationTest {


    @SuppressWarnings("unused")
    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public MessageProcessor messageProcessor;


    @Test
    public void shouldValidate() throws Exception {
        //given
        QueueMessage message = TestQueueMessage.tokenCreateActivate();
        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        messageProcessor.validateAdditionalData(message, journalTemplate);

        //then no exception means is valid

    }

    @Test
    public void shouldFailIfNoAdditionalData() {
        //given
        QueueMessage message = TestQueueMessage.tokenCreateActivate(false);
        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).isEqualTo("No additionalData found in message.");

    }

    @Test
    public void shouldFailIfActivatingLocationRefIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"activatingLocationRef\" : \"POS-LOC2\",", "\"activatingLocationRef\" : 458745,"));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.activatingLocationRef: integer found, string expected");

    }

    @Test
    public void shouldFailIfIssuerIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"issuer\" : \"Monavate\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.issuer: is missing but it is required");

    }

    @Test
    public void shouldFailIfIssuerIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"issuer\" : \"Monavate\",", "\"issuer\" : 458745,"));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.issuer: integer found, string expected");

    }

    @Test
    public void shouldFailIfExpiryDateIsMissing() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"expiryDate\" : \"2023-12-15\",", ""));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.expiryDate: is missing but it is required");

    }

    @Test
    public void shouldFailIfExpiryDateIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"expiryDate\" : \"2023-12-15\",", "\"expiryDate\" : 1231654,"));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.expiryDate: integer found, string expected");

    }

    @Test
    public void shouldFailIfExpiryDateIsEmptyString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"expiryDate\" : \"2023-12-15\",", "\"expiryDate\" : \"\","));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.expiryDate:  is an invalid date");

    }

    @SuppressWarnings("unused")
    static Stream<Arguments> shouldFailIfExpiryDateIsInvalidDate() {
        return Stream.of(
                Arguments.of("2022"),
                Arguments.of("2022-02"),
                Arguments.of("2022-02-3"),
                Arguments.of("2022-2-03"),
                Arguments.of("22-02-03")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void shouldFailIfExpiryDateIsInvalidDate(String malformedDate) {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"expiryDate\" : \"2023-12-15\",", "\"expiryDate\" : \"" + malformedDate + "\","));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.expiryDate: " + malformedDate + " is an invalid date");

    }

    @Test
    public void shouldFailIfActivationTimestampIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"activationTimestamp\" : \"2021-12-15T11:38:32Z\",", "\"activationTimestamp\" : 1231654,"));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.activationTimestamp: integer found, string expected");

    }

    @SuppressWarnings("unused")
    static Stream<Arguments> shouldFailIfActivationTimestampIsInvalidDateTime() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("2022"),
                Arguments.of("2022-02"),
                Arguments.of("2022-02-3"),
                Arguments.of("2022-2-03"),
                Arguments.of("22-02-03"),
                Arguments.of("2022-02-03T11:38:32"),
                Arguments.of("2022-02-03 11:38:32Z"),
                Arguments.of("2022-02-03T11:38:32.Z"),
                Arguments.of("2022-02-03T11:38Z"),
                Arguments.of("2022-02-03T1:38Z"),
                Arguments.of("2022-02-03T11:3Z")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void shouldFailIfActivationTimestampIsInvalidDateTime(String malformedDateTime) {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"activationTimestamp\" : \"2021-12-15T11:38:32Z\",", "\"activationTimestamp\" : \"" + malformedDateTime + "\","));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.activationTimestamp: does not match the regex pattern ^\\d{4}-\\d{2}-\\d{2}T\\d{2}\\:\\d{2}\\:\\d{2}(.\\d{1,9})?Z$");

    }

    @Test
    public void shouldFailIfActivationTimezoneIsNotString() {
        //given
        QueueMessage message = TestQueueMessage.fromJson(TestQueueMessage.tokenCreateActivateJson(),
                json -> json.replace("\"activationTimezone\" : \"Europe/Warsaw\"", "\"activationTimezone\" : 1231654"));

        JournalTemplate journalTemplate = TestJournalTemplate.orderReceived();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> messageProcessor.validateAdditionalData(message, journalTemplate));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData:\n")
                .contains("$.activationTimezone: integer found, string expected");

    }
}