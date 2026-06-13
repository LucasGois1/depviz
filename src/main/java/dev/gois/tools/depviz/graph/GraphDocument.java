package dev.gois.tools.depviz.graph;

import java.time.Instant;
import java.util.List;

public record GraphDocument(
    String schemaVersion,
    Instant generatedAt,
    ProjectInfo project,
    GraphSummary summary,
    ViewerConfig viewerConfig,
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    List<GraphPath> paths,
    List<DiagnosticEntry> diagnostics
) {
    public GraphDocument {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        paths = List.copyOf(paths);
        diagnostics = List.copyOf(diagnostics);
    }
}
