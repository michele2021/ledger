package com.accounting.core.entities;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.lang.reflect.Type;
import java.util.List;

//Shared class, should be immutable, thus @Value.
@Value
@Builder
@AllArgsConstructor
public class JournalTemplate {

    String clazz;
    String fileName;
    Constants.TxType txType;
    Message.EventType eventType;
    List<Constants.AccountType> accountTypes;
    Constants.JournalType journalType;
    String additionalDataSchema;

    //List<Constants.RegisterType> registerTypes; //unused

    @SuppressWarnings("unused")
    public static class JournalTemplateBuilder {
        @SerializedName("class")
        private String clazz;

        @JsonAdapter(JournalTemplate.AdditionalDataSchemaAdapter.class)
        String additionalDataSchema;
    }

    static class AdditionalDataSchemaAdapter implements JsonDeserializer<String> {
        @Override
        public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.toString();
        }
    }

}
