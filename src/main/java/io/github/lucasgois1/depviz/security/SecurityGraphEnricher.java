package io.github.lucasgois1.depviz.security;

import io.github.lucasgois1.depviz.graph.DiagnosticEntry;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.graph.GraphNode;
import io.github.lucasgois1.depviz.graph.GraphPath;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityGraphEnricher {
    public GraphDocument enrich(GraphDocument document, SecurityCheckResult result) {
        if (result == null || !result.enabled()) {
            return document;
        }

        Map<String, List<SecurityFinding>> findingsByNode = mapFindings(document, result.findings());
        Set<SecurityFinding> mappedFindings = findingsByNode.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SecurityFinding> unmapped = result.findings().stream()
            .filter(finding -> !mappedFindings.contains(finding))
            .toList();
        Map<String, List<SecurityFinding>> moduleFindings = aggregateModules(document, findingsByNode);

        List<GraphNode> nodes = document.nodes().stream()
            .map(node -> withSecurity(node, findingsFor(node, findingsByNode, moduleFindings)))
            .toList();
        List<DiagnosticEntry> diagnostics = new ArrayList<>(document.diagnostics());
        diagnostics.addAll(result.diagnostics());
        if (!unmapped.isEmpty()) {
            diagnostics.add(new DiagnosticEntry("warning", "snyk-unmapped-findings", unmappedMessage(unmapped), null));
        }

        SecuritySummary summary = new SecuritySummary(
            true,
            "snyk",
            result.checked(),
            findingsByNode.size(),
            moduleFindings.size(),
            countSeverity(mappedFindings, SecuritySeverity.CRITICAL),
            countSeverity(mappedFindings, SecuritySeverity.HIGH),
            countSeverity(mappedFindings, SecuritySeverity.MEDIUM),
            countSeverity(mappedFindings, SecuritySeverity.LOW),
            unmapped.size()
        );

        return new GraphDocument(
            document.schemaVersion(),
            document.generatedAt(),
            document.project(),
            document.summary(),
            document.viewerConfig(),
            nodes,
            document.edges(),
            document.paths(),
            document.versionSummary(),
            summary,
            diagnostics
        );
    }

    private static Map<String, List<SecurityFinding>> mapFindings(GraphDocument document, List<SecurityFinding> findings) {
        Map<String, List<SecurityFinding>> byNode = new LinkedHashMap<>();
        for (GraphNode node : document.nodes()) {
            if (node.root() || node.moduleRoot()) {
                continue;
            }
            String packageName = node.groupId() + ":" + node.artifactId();
            List<SecurityFinding> matches = findings.stream()
                .filter(finding -> packageName.equals(finding.packageName()))
                .filter(finding -> node.version().equals(finding.version()))
                .toList();
            if (!matches.isEmpty()) {
                byNode.put(node.id(), matches);
            }
        }
        return byNode;
    }

    private static Map<String, List<SecurityFinding>> aggregateModules(
        GraphDocument document,
        Map<String, List<SecurityFinding>> findingsByNode
    ) {
        Map<String, GraphNode> nodesById = document.nodes().stream()
            .collect(Collectors.toMap(GraphNode::id, node -> node, (first, ignored) -> first, LinkedHashMap::new));
        Map<String, List<SecurityFinding>> moduleFindings = new LinkedHashMap<>();
        for (GraphPath path : document.paths()) {
            List<SecurityFinding> findings = findingsByNode.get(path.target());
            if (findings == null || findings.isEmpty()) {
                continue;
            }
            for (String nodeId : path.nodeIds()) {
                GraphNode node = nodesById.get(nodeId);
                if (node != null && node.moduleRoot()) {
                    moduleFindings.computeIfAbsent(node.id(), ignored -> new ArrayList<>()).addAll(findings);
                }
            }
        }
        return moduleFindings;
    }

    private static List<SecurityFinding> findingsFor(
        GraphNode node,
        Map<String, List<SecurityFinding>> findingsByNode,
        Map<String, List<SecurityFinding>> moduleFindings
    ) {
        if (node.moduleRoot()) {
            return moduleFindings.getOrDefault(node.id(), List.of()).stream().distinct().toList();
        }
        return findingsByNode.getOrDefault(node.id(), List.of());
    }

    private static GraphNode withSecurity(GraphNode node, List<SecurityFinding> findings) {
        SecurityInsight insight = findings.isEmpty() ? null : SecurityInsight.vulnerable(findings);
        return new GraphNode(
            node.id(),
            node.groupId(),
            node.artifactId(),
            node.version(),
            node.type(),
            node.classifier(),
            node.scope(),
            node.optional(),
            node.depth(),
            node.root(),
            node.moduleRoot(),
            node.label(),
            node.coordinate(),
            node.groupColorKey(),
            node.versionInsight(),
            insight
        );
    }

    private static int countSeverity(Set<SecurityFinding> findings, SecuritySeverity severity) {
        return (int) findings.stream().filter(finding -> finding.severity() == severity).count();
    }

    private static String unmappedMessage(List<SecurityFinding> unmapped) {
        return "Snyk findings were not mapped to graph nodes: " + unmapped.stream()
            .map(SecurityFinding::id)
            .collect(Collectors.joining(", "));
    }
}
