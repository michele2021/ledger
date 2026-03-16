package com.accounting.core.rules;

import java.util.List;

@FunctionalInterface
public interface RuleFunction {
    Object apply(List<Object> args, RuleEvaluationContext context);
}
