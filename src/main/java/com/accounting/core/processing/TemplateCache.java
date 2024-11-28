package com.accounting.core.processing;

import com.accounting.commons.Message;
import com.accounting.commons.exceptions.AccountingEventException;
import com.accounting.core.entities.JournalTemplate;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Slf4j
public class TemplateCache {

    // TODO put this either in a config file or in a constants file
    public static final String FILE_INPUT_DIR_PATH = "./templates/";
    private static final String JSON_FILTER = "json";

    private final Map<Message.EventType, JournalTemplate> templates = new HashMap<>();

    public void load() throws RuntimeException {
        File inputDir = new File(FILE_INPUT_DIR_PATH);
        FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(JSON_FILTER);
        File[] files = inputDir.listFiles(filter);

        if (files == null) {
            throw new RuntimeException(format("No json files found in %s", inputDir.getAbsolutePath()));
        }
        final Gson gson = new Gson();
        for (File file : files) {
            log.info("Parsing file: {}", file);
            JournalTemplate.JournalTemplateBuilder templateBuilder;
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(file.toPath()))) {
                templateBuilder = gson.fromJson(reader, JournalTemplate.JournalTemplateBuilder.class);
            } catch (JsonParseException e) {
                throw new RuntimeException("Exception while trying to parse json event template file " + file.getName(), e);
            } catch (IOException e) {
                throw new RuntimeException("Exception while trying to read json event template file " + file.getName(), e);
            }
            if (templateBuilder == null) {
                throw new RuntimeException("Empty template file " + file.getName());
            }
            templateBuilder.fileName(file.getName());
            JournalTemplate template = templateBuilder.build();
            templates.put(template.getEventType(), template);
        }
    }

    public JournalTemplate get(Message.EventType eventType) throws AccountingEventException {
        JournalTemplate event = templates.get(eventType);
        if (event == null) {
            throw new AccountingEventException(format("Event with type '%s' not found.", eventType));
        }
        return event;
    }
}
