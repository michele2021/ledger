package com.accounting.core.rules.ai;

import com.accounting.core.rules.RuleEvaluationContext;

import java.util.Optional;

@FunctionalInterface
public interface AiFactResolver {
    Optional<AiFactResult> resolve(String factKey, RuleEvaluationContext context);
}
