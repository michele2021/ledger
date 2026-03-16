package com.accounting.core.rules;

import com.accounting.core.rules.ai.AiFactResolver;
import com.accounting.core.rules.ai.AiFactResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RuleEvaluationContext {
    private final Map<String, Object> facts;
    private final Map<String, RuleFunction> functions;
    private final AiFactResolver aiFactResolver;
    private final boolean allowAiFacts;
    private final double minAiFactConfidence;
    private final Map<String, AiFactResult> aiFactResults = new HashMap<>();
    private final Set<String> usedFacts = new HashSet<>();

    @lombok.Builder(builderClassName = "Builder", setterPrefix = "set")
    private RuleEvaluationContext(
            Map<String, Object> facts,
            Map<String, RuleFunction> functions,
            AiFactResolver aiFactResolver,
            boolean allowAiFacts,
            Double minAiFactConfidence
    ) {
        this.facts = facts == null ? new HashMap<>() : new HashMap<>(facts);
        Map<String, RuleFunction> resolvedFunctions = new HashMap<>(DefaultRuleFunctions.defaultFunctions());
        if (functions != null) {
            resolvedFunctions.putAll(functions);
        }
        this.functions = resolvedFunctions;
        this.aiFactResolver = aiFactResolver;
        this.allowAiFacts = allowAiFacts;
        this.minAiFactConfidence = minAiFactConfidence == null ? 0.85 : minAiFactConfidence;
    }

    public Object resolveFact(String key) {
        usedFacts.add(key);
        if (facts.containsKey(key)) {
            return facts.get(key);
        }
        if (allowAiFacts && aiFactResolver != null) {
            Optional<AiFactResult> resolved = aiFactResolver.resolve(key, this);
            if (resolved.isPresent()) {
                AiFactResult result = resolved.get();
                if (result.confidence() < minAiFactConfidence) {
                    throw new RuleEngineException("AI fact '" + key + "' below confidence threshold: " + result.confidence());
                }
                aiFactResults.put(key, result);
                facts.put(key, result.value());
                return result.value();
            }
        }
        throw new RuleEngineException("Missing fact: " + key);
    }

    public Object invokeFunction(String name, java.util.List<Object> args) {
        RuleFunction fn = functions.get(name);
        if (fn == null) {
            throw new RuleEngineException("Unknown function: " + name);
        }
        return fn.apply(args, this);
    }

    public Map<String, AiFactResult> aiFactResults() {
        return Collections.unmodifiableMap(aiFactResults);
    }

    public Set<String> usedFacts() {
        return Collections.unmodifiableSet(usedFacts);
    }

    public static class Builder {
        public Builder withFacts(Map<String, Object> facts) {
            if (facts != null) {
                if (this.facts == null) {
                    this.facts = new HashMap<>();
                }
                this.facts.putAll(facts);
            }
            return this;
        }

        public Builder putFact(String key, Object value) {
            if (this.facts == null) {
                this.facts = new HashMap<>();
            }
            this.facts.put(key, value);
            return this;
        }

        public Builder withFunctions(Map<String, RuleFunction> functions) {
            if (functions != null) {
                if (this.functions == null) {
                    this.functions = new HashMap<>();
                }
                this.functions.putAll(functions);
            }
            return this;
        }

        public Builder withFunction(String name, RuleFunction function) {
            if (this.functions == null) {
                this.functions = new HashMap<>();
            }
            this.functions.put(name, function);
            return this;
        }

        public Builder withAiFactResolver(AiFactResolver resolver) {
            this.aiFactResolver = resolver;
            return this;
        }

        public Builder allowAiFacts(boolean allow, double minConfidence) {
            this.allowAiFacts = allow;
            this.minAiFactConfidence = minConfidence;
            return this;
        }
    }
}
