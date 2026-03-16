package com.accounting.core.rules;

import com.accounting.core.rules.expr.ExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionEvaluatorTest {

    @Test
    void evaluatesBooleanExpressions() {
        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of(
                        "warehouseCountry", "DE",
                        "arrivalCountry", "DE",
                        "isArrivingInSpecialTerritoryEX", false
                ))
                .build();

        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        boolean result = evaluator.evaluateBoolean(
                "warehouseCountry == \"DE\" AND arrivalCountry == \"DE\" AND isArrivingInSpecialTerritoryEX == false",
                context
        );

        assertThat(result).isTrue();
    }

    @Test
    void evaluatesFunctionExpressions() {
        RuleEvaluationContext context = RuleEvaluationContext.builder()
                .withFacts(Map.of("taxDate", LocalDate.parse("2020-12-31")))
                .build();

        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        boolean result = evaluator.evaluateBoolean("dateBefore(taxDate, \"2021-01-01\")", context);

        assertThat(result).isTrue();
    }
}
