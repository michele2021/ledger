package com.accounting.core.rules;

import com.accounting.core.rules.ai.AiDecision;
import com.accounting.core.rules.ai.AiDecisionProvider;
import com.accounting.core.rules.expr.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;

public class RuleEngine {
    private final RuleEngineConfig config;
    private final ExpressionEvaluator evaluator;
    private final AiDecisionProvider aiDecisionProvider;

    public RuleEngine(RuleEngineConfig config, AiDecisionProvider aiDecisionProvider) {
        this.config = config == null ? RuleEngineConfig.defaults() : config;
        this.aiDecisionProvider = aiDecisionProvider;
        this.evaluator = new ExpressionEvaluator();
    }

    public RuleEvaluationResult evaluate(RuleGraph graph, RuleEvaluationContext context) {
        if (graph == null) {
            throw new RuleEngineException("Rule graph must be provided.");
        }
        String currentId = graph.rootId();
        List<RuleTraceEntry> trace = new ArrayList<>();
        for (int step = 0; step < config.maxSteps(); step++) {
            RuleNode node = graph.node(currentId);
            if (node == null) {
                throw new RuleEngineException("Unknown rule id: " + currentId);
            }

            RuleEvaluationStep stepResult = evaluateNode(node, context);
            trace.add(stepResult.traceEntry());

            String nextId = stepResult.nextId();
            if (nextId == null || nextId.isBlank()) {
                throw new RuleEngineException("Missing next node for rule: " + node.ruleId());
            }
            if (graph.isTerminal(nextId) || !graph.hasNode(nextId)) {
                return new RuleEvaluationResult(nextId, trace, context.usedFacts(), context.aiFactResults());
            }
            currentId = nextId;
        }
        throw new RuleEngineException("Rule evaluation exceeded max steps: " + config.maxSteps());
    }

    private RuleEvaluationStep evaluateNode(RuleNode node, RuleEvaluationContext context) {
        return switch (node.ruleType()) {
            case BOOLEAN -> evaluateBooleanNode(node, context);
            case SWITCH -> evaluateSwitchNode(node, context);
            case AI -> evaluateAiNode(node, context);
        };
    }

    private RuleEvaluationStep evaluateBooleanNode(RuleNode node, RuleEvaluationContext context) {
        boolean result = evaluator.evaluateBoolean(node.expression(), context);
        String outcomeKey = result ? RuleNode.TRUE_KEY : RuleNode.FALSE_KEY;
        String nextId = node.outcomeForKey(outcomeKey);
        RuleTraceEntry traceEntry = new RuleTraceEntry(
                node.ruleId(),
                node.ruleType(),
                node.expression(),
                result,
                outcomeKey,
                nextId,
                null
        );
        return new RuleEvaluationStep(nextId, traceEntry);
    }

    private RuleEvaluationStep evaluateSwitchNode(RuleNode node, RuleEvaluationContext context) {
        Object value = evaluator.evaluateValue(node.expression(), context);
        String outcomeKey = value == null ? "null" : value.toString();
        String nextId = node.outcomeForKey(outcomeKey);
        if (nextId == null) {
            throw new RuleEngineException("No switch outcome for key '" + outcomeKey + "' in rule " + node.ruleId());
        }
        RuleTraceEntry traceEntry = new RuleTraceEntry(
                node.ruleId(),
                node.ruleType(),
                node.expression(),
                value,
                outcomeKey,
                nextId,
                null
        );
        return new RuleEvaluationStep(nextId, traceEntry);
    }

    private RuleEvaluationStep evaluateAiNode(RuleNode node, RuleEvaluationContext context) {
        if (!config.allowAiDecisions()) {
            throw new RuleEngineException("AI decisions are disabled.");
        }
        if (aiDecisionProvider == null) {
            throw new RuleEngineException("AI decision provider is not configured.");
        }
        AiDecision decision = aiDecisionProvider.decide(node, context);
        if (decision == null) {
            throw new RuleEngineException("AI decision provider returned null for rule " + node.ruleId());
        }
        String outcomeKey = decision.outcomeKey();
        if (decision.confidence() < config.minAiDecisionConfidence()) {
            outcomeKey = RuleNode.AI_FALLBACK_KEY;
        }
        String nextId = node.outcomeForKey(outcomeKey);
        if (nextId == null) {
            throw new RuleEngineException("No AI outcome for key '" + outcomeKey + "' in rule " + node.ruleId());
        }
        RuleTraceEntry traceEntry = new RuleTraceEntry(
                node.ruleId(),
                node.ruleType(),
                node.expression(),
                decision,
                outcomeKey,
                nextId,
                decision
        );
        return new RuleEvaluationStep(nextId, traceEntry);
    }

    private record RuleEvaluationStep(String nextId, RuleTraceEntry traceEntry) {
    }
}
