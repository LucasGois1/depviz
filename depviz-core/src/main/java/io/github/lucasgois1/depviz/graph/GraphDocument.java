package io.github.lucasgois1.depviz.graph;

import io.github.lucasgois1.depviz.security.SecuritySummary;
import io.github.lucasgois1.depviz.version.VersionSummary;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GraphDocument(
    String schemaVersion,
    Instant generatedAt,
    ProjectInfo project,
    GraphSummary summary,
    ViewerConfig viewerConfig,
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    List<GraphPath> paths,
    VersionSummary versionSummary,
    SecuritySummary securitySummary,
    List<DiagnosticEntry> diagnostics
) {
    public GraphDocument {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        paths = List.copyOf(paths);
        versionSummary = Objects.requireNonNull(versionSummary, "versionSummary is required.");
        diagnostics = List.copyOf(diagnostics);
    }
}
