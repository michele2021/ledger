package com.accounting.core.rules;

import com.accounting.core.rules.ai.AiDecision;
import com.accounting.core.rules.ai.AiDecisionProvider;
import com.accounting.core.rules.ai.AiFactResolver;
import com.accounting.core.rules.ai.AiFactResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    @Test
    void evaluatesBooleanGraph() {
        RuleNode root = RuleNode.booleanRule(
                "[ROOT_1]",
                "1. Root Evaluation",
                "Is the transaction from DE (Germany)?",
                "warehouseCountry == \"DE\"",
                "revenueImportGermanyZeroRateNetNode",
                "revenueExNotTaxableNetResultNode"
        );
        RuleGraph graph = RuleGraph.builder()
                .root("[ROOT_1]")
                .addNode(root)
                .addTerminal("revenueImportGermanyZeroRateNetNode")
                .addTerminal("revenueExNotTaxableNetResultNode")
                .build();

        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of("warehouseCountry", "DE"))
                .build();

        RuleEngine engine = new RuleEngine(RuleEngineConfig.defaults(), null);
        RuleEvaluationResult result = engine.evaluate(graph, context);

        assertThat(result.terminalId()).isEqualTo("revenueImportGermanyZeroRateNetNode");
        assertThat(result.trace()).hasSize(1);
    }

    @Test
    void evaluatesSwitchGraph() {
        RuleNode root = new RuleNode(
                "[D_8]",
                "2. Core German (DE) Routing",
                "Check rate type (Domestic DE)",
                RuleType.SWITCH,
                "taxRateType",
                Map.of(
                        "ZERO_RATE", "REVENUE_DOMESTIC_DE_ZERO_RATE",
                        "STANDARD_RATE", "standardVatRateNode"
                )
        );
        RuleGraph graph = RuleGraph.builder()
                .root("[D_8]")
                .addNode(root)
                .addTerminal("REVENUE_DOMESTIC_DE_ZERO_RATE")
                .addTerminal("standardVatRateNode")
                .build();

        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of("taxRateType", "ZERO_RATE"))
                .build();

        RuleEngine engine = new RuleEngine(RuleEngineConfig.defaults(), null);
        RuleEvaluationResult result = engine.evaluate(graph, context);

        assertThat(result.terminalId()).isEqualTo("REVENUE_DOMESTIC_DE_ZERO_RATE");
    }

    @Test
    void evaluatesAiDecisionWithFallback() {
        RuleNode root = new RuleNode(
                "[AI_1]",
                "AI Routing",
                "AI decision for marketplace routing",
                RuleType.AI,
                "marketplace_decision",
                Map.of(
                        "marketplace", "revenueNotEuVatNode",
                        RuleNode.AI_FALLBACK_KEY, "manualReviewNode"
                )
        );
        RuleGraph graph = RuleGraph.builder()
                .root("[AI_1]")
                .addNode(root)
                .addTerminal("revenueNotEuVatNode")
                .addTerminal("manualReviewNode")
                .build();

        AiDecisionProvider provider = (node, context) -> new AiDecision(
                "marketplace",
                0.4,
                "test-model",
                "low confidence",
                Map.of()
        );

        RuleEngineConfig config = new RuleEngineConfig(true, true, 0.85, 0.9, 10);
        RuleEngine engine = new RuleEngine(config, provider);
        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of("dummy", true))
                .build();

        RuleEvaluationResult result = engine.evaluate(graph, context);
        assertThat(result.terminalId()).isEqualTo("manualReviewNode");
    }

    @Test
    void resolvesAiFacts() {
        RuleNode root = RuleNode.booleanRule(
                "[AI_FACT]",
                "AI Facts",
                "Is sold via marketplace?",
                "isSoldViaMarketplace == true",
                "revenueNotEuVatNode",
                "vatRateNode"
        );
        RuleGraph graph = RuleGraph.builder()
                .root("[AI_FACT]")
                .addNode(root)
                .addTerminal("revenueNotEuVatNode")
                .addTerminal("vatRateNode")
                .build();

        AiFactResolver resolver = (factKey, context) -> {
            if ("isSoldViaMarketplace".equals(factKey)) {
                return Optional.of(new AiFactResult(true, 0.95, "test-model", "classified", Map.of()));
            }
            return Optional.empty();
        };

        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of())
                .withAiFactResolver(resolver)
                .allowAiFacts(true, 0.8)
                .build();

        RuleEngine engine = new RuleEngine(RuleEngineConfig.defaults(), null);
        RuleEvaluationResult result = engine.evaluate(graph, context);

        assertThat(result.terminalId()).isEqualTo("revenueNotEuVatNode");
        assertThat(result.aiFacts()).containsKey("isSoldViaMarketplace");
    }
}
