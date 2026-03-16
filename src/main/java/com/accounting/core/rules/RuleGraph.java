package com.accounting.core.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RuleGraph {
    private final Map<String, RuleNode> nodes;
    private final Set<String> terminalIds;
    private final String rootId;

    private RuleGraph(Map<String, RuleNode> nodes, Set<String> terminalIds, String rootId) {
        this.nodes = Map.copyOf(nodes);
        this.terminalIds = Set.copyOf(terminalIds);
        this.rootId = rootId;
    }

    public RuleNode node(String ruleId) {
        return nodes.get(ruleId);
    }

    public boolean hasNode(String ruleId) {
        return nodes.containsKey(ruleId);
    }

    public Set<String> terminalIds() {
        return terminalIds;
    }

    public String rootId() {
        return rootId;
    }

    public Map<String, RuleNode> nodes() {
        return nodes;
    }

    public boolean isTerminal(String id) {
        return terminalIds.contains(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, RuleNode> nodes = new HashMap<>();
        private final Set<String> terminalIds = new HashSet<>();
        private String rootId;

        public Builder addNode(RuleNode node) {
            Objects.requireNonNull(node, "node");
            nodes.put(node.ruleId(), node);
            return this;
        }

        public Builder addTerminal(String terminalId) {
            if (terminalId != null && !terminalId.isBlank()) {
                terminalIds.add(terminalId);
            }
            return this;
        }

        public Builder root(String rootId) {
            this.rootId = rootId;
            return this;
        }

        public RuleGraph build() {
            if (rootId == null || rootId.isBlank()) {
                throw new RuleEngineException("Rule graph root id must be provided.");
            }
            if (!nodes.containsKey(rootId)) {
                throw new RuleEngineException("Rule graph root id '" + rootId + "' not found in nodes.");
            }
            return new RuleGraph(nodes, terminalIds, rootId);
        }
    }
}
