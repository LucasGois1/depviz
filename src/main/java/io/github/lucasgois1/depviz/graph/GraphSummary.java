package io.github.lucasgois1.depviz.graph;

import java.util.Map;

public record GraphSummary(
    int nodeCount,
    int edgeCount,
    Map<String, Integer> nodesByScope,
    Map<String, Integer> nodesByGroupId
) {
    public GraphSummary {
        nodesByScope = Map.copyOf(nodesByScope);
        nodesByGroupId = Map.copyOf(nodesByGroupId);
    }
}
