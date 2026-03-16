package com.accounting.core.rules;

import com.accounting.core.rules.ai.AiDecision;

public record RuleTraceEntry(String ruleId, RuleType ruleType, String expression, Object evaluationResult,
                             String outcomeKey, String nextId, AiDecision aiDecision) {
}
