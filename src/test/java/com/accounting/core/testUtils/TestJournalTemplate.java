package com.accounting.core.testUtils;

import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.processing.TemplateCache;
import lombok.SneakyThrows;

import static com.accounting.commons.Message.EventType.*;
import static org.junit.jupiter.api.Assertions.fail;

public class TestJournalTemplate {

    public static final TemplateCache templateCache = new TemplateCache();

    static {
        try {
            templateCache.load();
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    @SneakyThrows
    public static JournalTemplate orderReceived() {
        return templateCache.get(ORDER_RECEIVED);
    }

    @SneakyThrows
    public static JournalTemplate paymentReceived() {
        return templateCache.get(PAYMENT_RECEIVED);
    }

    @SneakyThrows
    public static JournalTemplate paymentMatch() {
        return templateCache.get(PAYMENT_MATCH);
    }

}