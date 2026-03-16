package com.accounting.core.rules.ai;

import java.util.Map;

public class AiFactResult {
    private final Object value;
    private final double confidence;
    private final String model;
    private final String explanation;
    private final Map<String, Object> metadata;

    public AiFactResult(Object value,
                        double confidence,
                        String model,
                        String explanation,
                        Map<String, Object> metadata) {
        this.value = value;
        this.confidence = confidence;
        this.model = model;
        this.explanation = explanation;
        this.metadata = metadata;
    }

    public Object value() {
        return value;
    }

    public double confidence() {
        return confidence;
    }

    public String model() {
        return model;
    }

    public String explanation() {
        return explanation;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
