package io.github.lucasgois1.depviz.graph;

import java.util.List;

public record GraphPath(String target, List<String> nodeIds) {
    public GraphPath {
        nodeIds = List.copyOf(nodeIds);
    }
}
