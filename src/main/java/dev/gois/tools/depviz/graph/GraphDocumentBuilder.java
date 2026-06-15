package dev.gois.tools.depviz.graph;

import dev.gois.tools.depviz.config.DepvizConfig;
import dev.gois.tools.depviz.util.Coordinates;
import dev.gois.tools.depviz.version.VersionCheckResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GraphDocumentBuilder {
    public static final String SCHEMA_VERSION = "1.0";

    public GraphDocument build(ExtractedDependencyNode root, ProjectInfo project, DepvizConfig config) {
        Objects.requireNonNull(config, "config is required.");
        return build(root, project, config, VersionCheckResult.empty(config.checkUpdates()));
    }

    public GraphDocument build(
        ExtractedDependencyNode root,
        ProjectInfo project,
        DepvizConfig config,
        VersionCheckResult versionCheck
    ) {
        Objects.requireNonNull(root, "root is required.");
        Objects.requireNonNull(project, "project is required.");
        Objects.requireNonNull(config, "config is required.");
        Objects.requireNonNull(versionCheck, "versionCheck is required.");

        BuilderState state = new BuilderState();
        visit(root, null, 0, List.of(), new LinkedHashSet<>(), state, versionCheck);
        state.diagnostics.addAll(versionCheck.diagnostics());

        List<GraphNode> nodes = List.copyOf(state.nodes.values());
        List<GraphEdge> edges = List.copyOf(state.edges.values());
        return new GraphDocument(
            SCHEMA_VERSION,
            Instant.now(),
            project,
            summarize(nodes, edges),
            viewerConfig(config),
            nodes,
            edges,
            state.paths,
            versionCheck.summary(),
            state.diagnostics
        );
    }

    private void visit(
        ExtractedDependencyNode current,
        String parentId,
        int depth,
        List<String> parentPath,
        Set<String> activePath,
        BuilderState state,
        VersionCheckResult versionCheck
    ) {
        List<String> currentPath = recordOccurrence(current, parentId, depth, parentPath, state, versionCheck);
        Set<String> nextActivePath = new LinkedHashSet<>(activePath);
        nextActivePath.add(Coordinates.stableId(current.coordinate()));

        for (ExtractedDependencyNode child : current.children()) {
            String childId = Coordinates.stableId(child.coordinate());
            if (nextActivePath.contains(childId)) {
                recordOccurrence(child, Coordinates.stableId(current.coordinate()), depth + 1, currentPath, state, versionCheck);
                continue;
            }
            visit(child, Coordinates.stableId(current.coordinate()), depth + 1, currentPath, nextActivePath, state, versionCheck);
        }
    }

    private List<String> recordOccurrence(
        ExtractedDependencyNode current,
        String parentId,
        int depth,
        List<String> parentPath,
        BuilderState state,
        VersionCheckResult versionCheck
    ) {
        ArtifactCoordinate coordinate = current.coordinate();
        String currentId = Coordinates.stableId(coordinate);
        List<String> currentPath = append(parentPath, currentId);

        state.nodes.merge(
            currentId,
            toGraphNode(current, currentId, depth, versionCheck),
            (existing, candidate) -> existing.depth() <= candidate.depth() ? existing : candidate
        );
        state.paths.add(new GraphPath(currentId, currentPath));
        state.diagnostics.addAll(withNodeId(current.diagnostics(), currentId));

        if (parentId != null) {
            GraphEdge edge = new GraphEdge(
                edgeId(parentId, currentId, current.scope(), current.optional()),
                parentId,
                currentId,
                current.scope(),
                current.optional(),
                depth
            );
            state.edges.putIfAbsent(edge.id(), edge);
        }

        return currentPath;
    }

    private static GraphNode toGraphNode(
        ExtractedDependencyNode dependencyNode,
        String id,
        int depth,
        VersionCheckResult versionCheck
    ) {
        ArtifactCoordinate coordinate = dependencyNode.coordinate();
        return new GraphNode(
            id,
            coordinate.groupId(),
            coordinate.artifactId(),
            coordinate.version(),
            coordinate.type(),
            coordinate.classifier(),
            depth == 0 ? "root" : dependencyNode.scope(),
            dependencyNode.optional(),
            depth,
            depth == 0,
            false,
            Coordinates.label(coordinate),
            Coordinates.displayCoordinate(coordinate),
            coordinate.groupId(),
            versionCheck.insightsByNodeId().get(id)
        );
    }

    private static List<String> append(List<String> parentPath, String id) {
        List<String> path = new ArrayList<>(parentPath.size() + 1);
        path.addAll(parentPath);
        path.add(id);
        return path;
    }

    private static String edgeId(String source, String target, String scope, boolean optional) {
        return escape(source) + "->" + escape(target) + "[" + escape(scope) + "|" + optional + "]";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("|", "\\|")
            .replace("[", "\\[")
            .replace("]", "\\]");
    }

    private static List<DiagnosticEntry> withNodeId(List<DiagnosticEntry> diagnostics, String nodeId) {
        return diagnostics.stream()
            .map(diagnostic -> {
                if (diagnostic.nodeId() != null && !diagnostic.nodeId().isBlank()) {
                    return diagnostic;
                }
                return new DiagnosticEntry(diagnostic.severity(), diagnostic.type(), diagnostic.message(), nodeId);
            })
            .toList();
    }

    private static GraphSummary summarize(List<GraphNode> nodes, List<GraphEdge> edges) {
        Map<String, Integer> nodesByScope = new LinkedHashMap<>();
        Map<String, Integer> nodesByGroupId = new LinkedHashMap<>();

        for (GraphNode node : nodes) {
            nodesByScope.merge(node.scope(), 1, Integer::sum);
            nodesByGroupId.merge(node.groupId(), 1, Integer::sum);
        }

        return new GraphSummary(nodes.size(), edges.size(), nodesByScope, nodesByGroupId);
    }

    private static ViewerConfig viewerConfig(DepvizConfig config) {
        return new ViewerConfig(
            config.initialLayout().value(),
            config.maxInitialLabels(),
            config.nodeMode().value()
        );
    }

    private static final class BuilderState {
        private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
        private final Map<String, GraphEdge> edges = new LinkedHashMap<>();
        private final List<GraphPath> paths = new ArrayList<>();
        private final List<DiagnosticEntry> diagnostics = new ArrayList<>();
    }
}
