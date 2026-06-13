package dev.gois.tools.depviz.graph;

public record GraphEdge(
    String id,
    String source,
    String target,
    String scope,
    boolean optional,
    int depth
) {
}
