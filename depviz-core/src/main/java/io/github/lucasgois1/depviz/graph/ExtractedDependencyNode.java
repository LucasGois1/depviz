package io.github.lucasgois1.depviz.graph;

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

    public DependencyNodeInput toInput(boolean moduleRoot) {
        return new DependencyNodeInput(
            coordinate,
            scope,
            optional,
            moduleRoot,
            children.stream()
                .map(child -> child.toInput("module".equals(child.scope())))
                .toList(),
            diagnostics
        );
    }
}
