package com.accounting.core.rules;

import com.accounting.core.rules.ai.AiFactResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RuleEvaluationResult(String terminalId, List<RuleTraceEntry> trace, Set<String> factsUsed,
                                   Map<String, AiFactResult> aiFacts) {
    public RuleEvaluationResult(String terminalId,
                                List<RuleTraceEntry> trace,
                                Set<String> factsUsed,
                                Map<String, AiFactResult> aiFacts) {
        this.terminalId = terminalId;
        this.trace = List.copyOf(trace);
        this.factsUsed = Set.copyOf(factsUsed);
        this.aiFacts = Map.copyOf(aiFacts);
    }

    @Override
    public List<RuleTraceEntry> trace() {
        return Collections.unmodifiableList(trace);
    }

    @Override
    public Set<String> factsUsed() {
        return Collections.unmodifiableSet(factsUsed);
    }

    @Override
    public Map<String, AiFactResult> aiFacts() {
        return Collections.unmodifiableMap(aiFacts);
    }
}
