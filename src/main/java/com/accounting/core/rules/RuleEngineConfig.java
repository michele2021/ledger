package com.accounting.core.rules;

public record RuleEngineConfig(boolean allowAiFacts, boolean allowAiDecisions, double minAiFactConfidence,
                               double minAiDecisionConfidence, int maxSteps) {

    public static RuleEngineConfig defaults() {
        return new RuleEngineConfig(false, false, 0.85, 0.9, 500);
    }
}
