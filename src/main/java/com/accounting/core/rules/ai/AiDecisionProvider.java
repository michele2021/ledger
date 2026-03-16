package com.accounting.core.rules.ai;

import com.accounting.core.rules.RuleEvaluationContext;
import com.accounting.core.rules.RuleNode;

@FunctionalInterface
public interface AiDecisionProvider {
    AiDecision decide(RuleNode node, RuleEvaluationContext context);
}
