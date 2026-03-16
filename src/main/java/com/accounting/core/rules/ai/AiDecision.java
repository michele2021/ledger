package com.accounting.core.rules.ai;

import java.util.Map;

public class AiDecision {
    private final String outcomeKey;
    private final double confidence;
    private final String model;
    private final String explanation;
    private final Map<String, Object> metadata;

    public AiDecision(String outcomeKey,
                      double confidence,
                      String model,
                      String explanation,
                      Map<String, Object> metadata) {
        this.outcomeKey = outcomeKey;
        this.confidence = confidence;
        this.model = model;
        this.explanation = explanation;
        this.metadata = metadata;
    }

    public String outcomeKey() {
        return outcomeKey;
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
