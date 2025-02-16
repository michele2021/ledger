package com.accounting.core.testUtils;

import com.accounting.commons.deserializers.AmountDeserializer;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.queue.QueueMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.function.Function;

public class TestQueueMessage {

    //make sure this is created same way like in the QueueProcessor.java
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Amount.class, new AmountDeserializer())
            .create();

    @SafeVarargs
    public static QueueMessage fromJson(String json, Function<String, String>... alterations) {
        for (Function<String, String> i : alterations) {
            json = i.apply(json);
        }
        return gson.fromJson(json, QueueMessage.class);
    }

    public static String tokenCreateJson(String accountCode, String source, String type, boolean includeAdditionalData) {
        String additionalData = includeAdditionalData ? """
                 "additionalData": {
                    "publishableId": "4587458",
                    "activatingLocationRef": "POS-LOC2"
                  },
                """ : "";

        return """
                {
                  "type": %s,
                  "accountCode": %s,
                  "amount": { "quantity": 150.00, "currencyCode": "EUR" },
                  %s
                  "uuid": "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source": %s,
                  "timestamp": 315664565
                }
                """.formatted(type, accountCode, additionalData, source);
    }

    public static String tokenCreateJson() {
        return tokenCreateJson("EPAY", "LEDGER_API","TOKEN_CREATE", true);
    }

        public static String tokenCreateWithTestFlagTrueJson() {
        return """
                {
                  "type" :"TOKEN_CREATE",\s
                  "accountCode" : "EPAY",\s
                  "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                  "additionalData" : { \s
                    "publishableId" : "4587458",
                    "test" : true,
                    "activatingLocationRef" : "POS-LOC2"
                  },
                  "uuid" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565
                }""";
    }

    public static String tokenCreateWithTestFlagFalseJson() {
        return """
                {
                  "type" :"TOKEN_CREATE",\s
                  "accountCode" : "EPAY",\s
                  "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                  "additionalData" : { \s
                    "publishableId" : "4587458",
                    "test" : false,
                    "activatingLocationRef" : "POS-LOC2"
                  },
                  "uuid" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565
                }""";
    }

    public static QueueMessage tokenCreate() {
        return fromJson(tokenCreateJson("EPAY", "LEDGER_API", "TOKEN_CREATE", true));
    }

    public static QueueMessage tokenCreate(String accountCode, String source, String type) {
        return fromJson(tokenCreateJson(accountCode, source, type, true));
    }

    public static QueueMessage tokenCreate(boolean includeAdditionalData) {
        return fromJson(tokenCreateJson("EPAY", "LEDGER_API", "TOKEN_CREATE", includeAdditionalData));
    }

    public static QueueMessage tokenCreateWithTestFlag(boolean test) {
        return test ? fromJson(tokenCreateWithTestFlagTrueJson()) : fromJson(tokenCreateWithTestFlagFalseJson());
    }

    public static String tokenActivateJson(boolean includeAdditionalData) {
        String additionalData = includeAdditionalData ? """
                ,
                  "additionalData": {
                    "activatingLocationRef": "POS-LOC2",
                    "expiryDate": "2023-12-15",
                    "issuer": "Monavate",
                    "activationTimestamp": "2021-12-15T11:38:32Z",
                    "activationTimezone": "Europe/Warsaw"
                  }
                """ : "";

        return """
                {
                  "type": "TOKEN_ACTIVATE",
                  "accountCode": "EPAY",
                  "amount": { "quantity": 150.00, "currencyCode": "EUR" }%s,
                  "txReference": "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source": "LEDGER_API",
                  "timestamp": 315664565
                }
                """.formatted(additionalData);
    }

    public static String tokenActivateJson() {
        return tokenActivateJson(true);
    }

    public static QueueMessage tokenActivate() {
        return fromJson(tokenActivateJson());
    }

    public static QueueMessage tokenActivate(boolean includeAdditionalData) {
        return fromJson(tokenActivateJson(includeAdditionalData));
    }

    public static String tokenCreateActivateJson(boolean includeAdditionalData) {
        String additionalData = includeAdditionalData ? """
                ,
                  "additionalData": {
                    "publishableId": "4587458",
                    "orderItemId": "0123456789",
                    "activatingLocationRef": "POS-LOC2",
                    "expiryDate": "2023-12-15",
                    "issuer": "Monavate",
                    "activationTimestamp": "2021-12-15T11:38:32Z",
                    "activationTimezone": "Europe/Warsaw"
                  }
                """ : "";

        return """
                {
                  "type": "TOKEN_CREATE_ACTIVE",
                  "accountCode": "EPAY",
                  "amount": { "quantity": 150.00, "currencyCode": "EUR" }%s,
                  "uuid": "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source": "LEDGER_API",
                  "timestamp": 315664565
                }
                """.formatted(additionalData);
    }

    public static String tokenCreateActivateJson() {
        return tokenCreateActivateJson(true);
    }

    public static QueueMessage tokenCreateActivate() {
        return fromJson(tokenCreateActivateJson(true));
    }

    public static QueueMessage tokenCreateActivate(boolean includeAdditionalData) {
        return fromJson(tokenCreateActivateJson(includeAdditionalData));
    }

    public static String tokenCancelJson() {
        return """
                {
                  "type" :"TOKEN_CANCEL",\s
                  "accountCode" : "EPAY",\s
                  "txReference" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565
                }""";
    }

    public static QueueMessage tokenCancel() {
        return fromJson(tokenCancelJson());
    }

    public static String tokenConvertJson() {
        return """
                {
                  "type" :"TOKEN_ACTIVATE",\s
                  "accountCode" : "EPAY",\s
                  "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                  "additionalData" : { \s
                    "issuer" : "Monavate",
                    "prepaidCardExternalId" : "ExternalId"
                  },
                  "txReference" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565
                }""";
    }

    public static QueueMessage tokenConvert() {
        return fromJson(tokenConvertJson());
    }


    public static String clientOrderCancelledJson() {
        return """
                {
                  "type" :"CLIENT_ORDER_CANCEL",\s
                  "accountCode" : "EPAY",\s
                  "txReference" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565
                ,\
                  "additionalData" : { \s
                    "orderId" : 5487545
                  }
                }""";
    }

    public static QueueMessage clientOrderCancelled() {
        return fromJson(clientOrderCancelledJson());
    }

    public static String clientOrderReceivedJson() {
        return """
                {
                  "type" :"CLIENT_ORDER_CANCEL",\s
                  "accountCode" : "EPAY",\s
                  "txReference" : "abc875ca-db99-42bd-bca1-ef7c1ec67ba5",
                  "source" : "LEDGER_API",
                  "timestamp" : 315664565,
                  "additionalData" : { \s
                    "sku" : "dfsdf234",
                    "cardProgram" : "EPAY",
                    "orderTitle" : "some order",
                    "orderId" : 5487545,
                    "tokens" : [ \
                      {\
                        "quantity" : 1,\
                        "name" : "a name",\
                        "email" : "someone@somewhere.com"\
                      },\
                      {\
                        "quantity" : 3,\
                        "name" : "other name",\
                        "email" : "noone@somewhere.com"\
                      }\
                    ]
                  }
                }""";
    }

    public static String clientOrderReceivedAdditionalDataNoTokensJson() {
        return """
                {
                    "sku" : "dfsdf234",
                    "cardProgram" : "EPAY",
                    "orderTitle" : "some order",
                    "orderId" : 5487545
                }""";
    }

    public static String clientOrderReceivedAdditionalDataTokensEmptyJson() {
        return """
                {
                    "sku" : "dfsdf234",
                    "cardProgram" : "EPAY",
                    "orderTitle" : "some order",
                    "orderId" : 5487545,
                    "tokens" : []
                }""";
    }

    public static QueueMessage clientOrderReceived() {
        return fromJson(clientOrderReceivedJson());
    }

}
