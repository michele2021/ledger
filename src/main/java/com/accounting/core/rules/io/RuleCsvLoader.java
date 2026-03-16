package com.accounting.core.rules.io;

import com.accounting.core.rules.RuleEngineException;
import com.accounting.core.rules.RuleGraph;
import com.accounting.core.rules.RuleNode;
import com.accounting.core.rules.RuleType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleCsvLoader {
    private static final Gson GSON = new Gson();

    public RuleGraph load(Path path, String rootId) throws IOException {
        if (path == null) {
            throw new RuleEngineException("CSV path must be provided.");
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuleEngineException("Empty CSV file: " + path);
            }
            List<String> headers = CsvUtil.parseLine(headerLine);
            Map<String, Integer> index = indexHeaders(headers);

            RuleGraph.Builder builder = RuleGraph.builder().root(rootId);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = CsvUtil.parseLine(line);
                String ruleId = getField(fields, index, "rule_id");
                if (ruleId == null || ruleId.isBlank()) {
                    continue;
                }
                RuleType ruleType = parseRuleType(getField(fields, index, "rule_type"), lineNumber);
                String section = getField(fields, index, "section");
                String description = getField(fields, index, "description");
                String expression = getField(fields, index, "expression");

                Map<String, String> outcomes = new HashMap<>();
                if (ruleType == RuleType.BOOLEAN) {
                    outcomes.put(RuleNode.TRUE_KEY, getField(fields, index, "true_outcome"));
                    outcomes.put(RuleNode.FALSE_KEY, getField(fields, index, "false_outcome"));
                } else {
                    String outcomeMap = getField(fields, index, "outcome_map");
                    if (outcomeMap == null || outcomeMap.isBlank()) {
                        throw new RuleEngineException("Missing outcome_map for rule " + ruleId + " at line " + lineNumber);
                    }
                    outcomes.putAll(parseOutcomeMap(outcomeMap, lineNumber));
                }
                RuleNode node = new RuleNode(ruleId, section, description, ruleType, expression, outcomes);
                builder.addNode(node);
            }
            return builder.build();
        }
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim().toLowerCase(), i);
        }
        return index;
    }

    private String getField(List<String> fields, Map<String, Integer> index, String key) {
        Integer position = index.get(key);
        if (position == null || position >= fields.size()) {
            return null;
        }
        String value = fields.get(position);
        return value == null ? null : value.trim();
    }

    private RuleType parseRuleType(String raw, int lineNumber) {
        if (raw == null || raw.isBlank()) {
            throw new RuleEngineException("Missing rule_type at line " + lineNumber);
        }
        try {
            return RuleType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleEngineException("Unknown rule_type '" + raw + "' at line " + lineNumber, e);
        }
    }

    private Map<String, String> parseOutcomeMap(String raw, int lineNumber) {
        try {
            Map<?, ?> map = GSON.fromJson(raw, Map.class);
            Map<String, String> result = new HashMap<>();
            if (map != null) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new RuleEngineException("Invalid outcome_map JSON at line " + lineNumber, e);
        }
    }
}
