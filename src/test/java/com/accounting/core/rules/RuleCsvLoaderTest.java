package com.accounting.core.rules;

import com.accounting.core.rules.io.RuleCsvLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuleCsvLoaderTest {

    @Test
    void loadsUserRulesCsv() throws Exception {
        Path csvPath = Path.of("scripts", "rules", "user_rules.dsl.csv");
        RuleGraph graph = new RuleCsvLoader().load(csvPath, "[D_6]");

        assertThat(graph.nodes()).isNotEmpty();
        assertThat(graph.rootId()).isEqualTo("[D_6]");
    }
}
