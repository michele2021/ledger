package com.accounting.core.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record RuleNode(String ruleId, String section, String description, RuleType ruleType, String expression,
                       Map<String, String> outcomes) {
    public static final String TRUE_KEY = "true";
    public static final String FALSE_KEY = "false";
    public static final String AI_FALLBACK_KEY = "fallback";

    public RuleNode(String ruleId,
                    String section,
                    String description,
                    RuleType ruleType,
                    String expression,
                    Map<String, String> outcomes) {
        this.ruleId = require(ruleId, "ruleId");
        this.section = section;
        this.description = description;
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
        this.expression = expression;
        this.outcomes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(outcomes, "outcomes")));
    }

    public static RuleNode booleanRule(String ruleId,
                                       String section,
                                       String description,
                                       String expression,
                                       String trueOutcome,
                                       String falseOutcome) {
        Map<String, String> outcomes = new LinkedHashMap<>();
        outcomes.put(TRUE_KEY, trueOutcome);
        outcomes.put(FALSE_KEY, falseOutcome);
        return new RuleNode(ruleId, section, description, RuleType.BOOLEAN, expression, outcomes);
    }

    public String outcomeForKey(String key) {
        return outcomes.get(key);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be set");
        }
        return value;
    }
}
