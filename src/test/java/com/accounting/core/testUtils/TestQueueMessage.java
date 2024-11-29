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
        for(Function<String, String> i : alterations) {
            json = i.apply(json);
        }
        return gson.fromJson(json, QueueMessage.class);
    }

    public static String tokenCreateJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_CREATE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"publishableId\" : \"4587458\",\n" +
                "    \"activatingLocationRef\" : \"POS-LOC2\"\n" +
                "  },\n" +
                "  \"uuid\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static String tokenCreateWithTestFlagTrueJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_CREATE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"publishableId\" : \"4587458\",\n" +
                "    \"test\" : true,\n" +
                "    \"activatingLocationRef\" : \"POS-LOC2\"\n" +
                "  },\n" +
                "  \"uuid\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static String tokenCreateWithTestFlagFalseJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_CREATE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"publishableId\" : \"4587458\",\n" +
                "    \"test\" : false,\n" +
                "    \"activatingLocationRef\" : \"POS-LOC2\"\n" +
                "  },\n" +
                "  \"uuid\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static QueueMessage tokenCreate() {
        return fromJson(tokenCreateJson());
    }

    public static QueueMessage tokenCreateWithTestFlag(boolean test) {
        return test ? fromJson(tokenCreateWithTestFlagTrueJson()) : fromJson(tokenCreateWithTestFlagFalseJson());
    }

    public static String tokenActivateJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_ACTIVATE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"activatingLocationRef\" : \"POS-LOC2\",\n" +
                "    \"expiryDate\" : \"2023-12-15\",\n" +
                "    \"issuer\" : \"Monavate\",\n" +
                "    \"activationTimestamp\" : \"2021-12-15T11:38:32Z\",\n" +
                "    \"activationTimezone\" : \"Europe/Warsaw\"\n" +
                "  },\n" +
                "  \"txReference\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static QueueMessage tokenActivate() {
        return fromJson(tokenActivateJson());
    }

    public static String tokenCreateActivateJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_CREATE_ACTIVE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"publishableId\" : \"4587458\",\n" +
                "    \"orderItemId\" : \"0123456789\",\n" +
                "    \"activatingLocationRef\" : \"POS-LOC2\",\n" +
                "    \"expiryDate\" : \"2023-12-15\",\n" +
                "    \"issuer\" : \"Monavate\",\n" +
                "    \"activationTimestamp\" : \"2021-12-15T11:38:32Z\",\n" +
                "    \"activationTimezone\" : \"Europe/Warsaw\"\n" +
                "  },\n" +
                "  \"uuid\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static QueueMessage tokenCreateActivate() {
        return fromJson(tokenCreateActivateJson());
    }

    public static String tokenCancelJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_CANCEL\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"txReference\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static QueueMessage tokenCancel() {
        return fromJson(tokenCancelJson());
    }

    public static String tokenConvertJson() {
        return "{\n" +
                "  \"type\" :\"TOKEN_ACTIVATE\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
                "  \"additionalData\" : {  \n" +
                "    \"issuer\" : \"Monavate\",\n" +
                "    \"prepaidCardExternalId\" : \"ExternalId\"\n" +
                "  },\n" +
                "  \"txReference\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n" +
                "}";
    }

    public static QueueMessage tokenConvert() {
        return fromJson(tokenConvertJson());
    }


    public static String clientOrderCancelledJson() {
        return "{\n" +
                "  \"type\" :\"CLIENT_ORDER_CANCEL\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"txReference\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565\n," +
                "  \"additionalData\" : {  \n" +
                "    \"orderId\" : 5487545\n" +
                "  }\n" +
                "}";
    }

    public static QueueMessage clientOrderCancelled() {
        return fromJson(clientOrderCancelledJson());
    }

    public static String clientOrderReceivedJson() {
        return "{\n" +
                "  \"type\" :\"CLIENT_ORDER_CANCEL\", \n" +
                "  \"accountCode\" : \"EPAY\", \n" +
                "  \"txReference\" : \"abc875ca-db99-42bd-bca1-ef7c1ec67ba5\",\n" +
                "  \"source\" : \"LEDGER_API\",\n" +
                "  \"timestamp\" : 315664565,\n" +
                "  \"additionalData\" : {  \n" +
                "    \"sku\" : \"dfsdf234\",\n" +
                "    \"cardProgram\" : \"EPAY\",\n" +
                "    \"orderTitle\" : \"some order\",\n" +
                "    \"orderId\" : 5487545,\n" +
                "    \"tokens\" : [ " +
                "      {" +
                "        \"quantity\" : 1," +
                "        \"name\" : \"a name\"," +
                "        \"email\" : \"someone@somewhere.com\"" +
                "      }," +
                "      {" +
                "        \"quantity\" : 3," +
                "        \"name\" : \"other name\"," +
                "        \"email\" : \"noone@somewhere.com\"" +
                "      }" +
                "    ]\n" +
                "  }\n" +
                "}";
    }

    public static String clientOrderReceivedAdditionalDataNoTokensJson() {
        return "{\n" +
                "    \"sku\" : \"dfsdf234\",\n" +
                "    \"cardProgram\" : \"EPAY\",\n" +
                "    \"orderTitle\" : \"some order\",\n" +
                "    \"orderId\" : 5487545\n" +
                "}";
    }

    public static String clientOrderReceivedAdditionalDataTokensEmptyJson() {
        return "{\n" +
                "    \"sku\" : \"dfsdf234\",\n" +
                "    \"cardProgram\" : \"EPAY\",\n" +
                "    \"orderTitle\" : \"some order\",\n" +
                "    \"orderId\" : 5487545,\n" +
                "    \"tokens\" : []\n" +
                "}";
    }

    public static QueueMessage clientOrderReceived() {
        return fromJson(clientOrderReceivedJson());
    }



}
