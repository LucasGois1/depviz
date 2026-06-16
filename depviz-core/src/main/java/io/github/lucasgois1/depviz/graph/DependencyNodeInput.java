package io.github.lucasgois1.depviz.graph;

import java.util.List;
import java.util.Objects;

public record DependencyNodeInput(
    ArtifactCoordinate coordinate,
    String scope,
    boolean optional,
    boolean moduleRoot,
    List<DependencyNodeInput> children,
    List<DiagnosticEntry> diagnostics
) {
    public DependencyNodeInput {
        coordinate = Objects.requireNonNull(coordinate, "coordinate is required.");
        scope = scope == null || scope.isBlank() ? "runtime" : scope.trim();
        children = children == null ? List.of() : List.copyOf(children);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
