package com.accounting.core.processing;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.services.RepositoryHelper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.accounting.commons.Constants.AccountType.Client;
import static com.accounting.commons.Constants.AccountType.Company;
import static com.accounting.commons.Message.EventType.BANK_PAYMENT;
import static com.accounting.commons.Message.EventType.PAYMENT_RECEIVED;
import static org.junit.jupiter.api.Assertions.*;

class TemplateCacheTest {

    JsonSchemaFactory jsonSchemaFactory;

    @Test
    void shouldLoadTemplatesFromResources() throws Exception {
        //given


        //when
        TemplateCache subject = new TemplateCache();
        subject.load();

        //then
        @SuppressWarnings("unchecked")
        Map<Message.EventType, JournalTemplate> templates = (Map<Message.EventType, JournalTemplate>) FieldUtils.readField(subject, "templates", true);
        assertNotNull(templates);
        assertEquals(17, templates.size());

        jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

        // TODO here add new templates!!
        assertTemplate(templates.get(PAYMENT_RECEIVED), "AccountAddFunds", "PaymentReceived.json", Constants.TxType.Payment, PAYMENT_RECEIVED, List.of(Client), Constants.JournalType.FundsReceived, true);
        assertTemplate(templates.get(BANK_PAYMENT), "BankPayment", "BankPayment.json", Constants.TxType.Payment, BANK_PAYMENT, List.of(Client, Company), Constants.JournalType.PaymentReceived, true);
    }

    private void assertTemplate(JournalTemplate journalTemplate,
                                String clazz,
                                String fileName,
                                Constants.TxType txType,
                                Message.EventType eventType,
                                List<Constants.AccountType> accountTypes,
                                Constants.JournalType journalType,
                                boolean haveAdditionalDataSchema) throws Exception {
        assertEquals(clazz, journalTemplate.getClazz());
        assertEquals(fileName, journalTemplate.getFileName());
        assertEquals(txType, journalTemplate.getTxType());
        assertEquals(eventType, journalTemplate.getEventType());
        assertEquals(accountTypes, journalTemplate.getAccountTypes());
        assertEquals(journalType, journalTemplate.getJournalType());

        //make sure class exists and have proper ctor
        //noinspection ResultOfMethodCallIgnored
        Class.forName("com.accounting.core.processing.templates." + journalTemplate.getClazz())
                .getConstructor(JournalMaker.class, Account.class, Amount.class, JournalType.class, TxType.class, QueueMessage.class, RepositoryHelper.class);

        if (haveAdditionalDataSchema) {
            assertNotNull(journalTemplate.getAdditionalDataSchema());
            Optional.of(journalTemplate.getAdditionalDataSchema())
                    .map(jsonSchemaFactory::getSchema)
                    .ifPresent(Assertions::assertNotNull);
        } else {
            assertNull(journalTemplate.getAdditionalDataSchema());
        }

    }
}