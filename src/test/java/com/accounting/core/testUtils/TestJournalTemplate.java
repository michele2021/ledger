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
    public static JournalTemplate tokenCreate() {
        return templateCache.get(TOKEN_CREATE);
    }

    @SneakyThrows
    public static JournalTemplate tokenActivate() {
        return templateCache.get(TOKEN_ACTIVATE);
    }

    @SneakyThrows
    public static JournalTemplate tokenCreateActivate() {
        return templateCache.get(TOKEN_CREATE_ACTIVE);
    }

    @SneakyThrows
    public static JournalTemplate tokenCancel() {
        return templateCache.get(TOKEN_CANCEL);
    }

    @SneakyThrows
    public static JournalTemplate tokenConvert() {
        return templateCache.get(TOKEN_CONVERSION);
    }

    @SneakyThrows
    public static JournalTemplate clientOrderCancelled() {
        return templateCache.get(CLIENT_ORDER_CANCEL);
    }

    @SneakyThrows
    public static JournalTemplate clientOrderReceived() {
        return templateCache.get(CLIENT_ORDER_RECEIVE);
    }


}
