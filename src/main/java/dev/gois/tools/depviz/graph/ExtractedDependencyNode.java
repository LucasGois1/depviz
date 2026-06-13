package dev.gois.tools.depviz.graph;

import java.util.List;

public record ExtractedDependencyNode(
    ArtifactCoordinate coordinate,
    String scope,
    boolean optional,
    List<ExtractedDependencyNode> children,
    List<DiagnosticEntry> diagnostics
) {
    public ExtractedDependencyNode {
        children = children == null ? List.of() : List.copyOf(children);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
