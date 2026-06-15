package dev.gois.tools.depviz.version;

import dev.gois.tools.depviz.graph.ArtifactCoordinate;
import dev.gois.tools.depviz.graph.DiagnosticEntry;
import dev.gois.tools.depviz.graph.ExtractedDependencyNode;
import dev.gois.tools.depviz.util.Coordinates;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VersionUpdateChecker {
    private static final String STATUS_CURRENT = "current";
    private static final String STATUS_OUTDATED = "outdated";
    private static final String STATUS_UNAVAILABLE = "unavailable";
    private static final String UPDATE_PATCH = "patch";
    private static final String UPDATE_MINOR = "minor";
    private static final String UPDATE_MAJOR = "major";
    private static final String UPDATE_UNKNOWN = "unknown";

    private final VersionLookup lookup;

    public VersionUpdateChecker(VersionLookup lookup) {
        this.lookup = lookup;
    }

    public VersionCheckResult check(ExtractedDependencyNode root, boolean enabled) {
        if (!enabled) {
            return VersionCheckResult.empty(false);
        }
        if (root == null) {
            return VersionCheckResult.empty(true);
        }

        Map<String, VersionInsight> insightsByNodeId = new LinkedHashMap<>();
        Map<ArtifactVersionKey, VersionInsight> insightsByArtifact = new HashMap<>();
        List<DiagnosticEntry> diagnostics = new ArrayList<>();
        SummaryCounts counts = new SummaryCounts();

        ArrayDeque<ExtractedDependencyNode> nodes = new ArrayDeque<>(root.children());
        while (!nodes.isEmpty()) {
            ExtractedDependencyNode node = nodes.removeFirst();
            nodes.addAll(node.children());

            String nodeId = Coordinates.stableId(node.coordinate());
            VersionInsight insight = insightsByArtifact.computeIfAbsent(
                key(node.coordinate()),
                artifactKey -> lookupInsight(artifactKey, node, nodeId, diagnostics)
            );
            insightsByNodeId.put(nodeId, insight);
            counts.add(insight);
        }

        return new VersionCheckResult(insightsByNodeId, counts.toSummary(), diagnostics);
    }

    private VersionInsight lookupInsight(
        ArtifactVersionKey key,
        ExtractedDependencyNode node,
        String nodeId,
        List<DiagnosticEntry> diagnostics
    ) {
        try {
            Optional<String> latestStable = VersionClassifier.latestStable(
                lookup.availableVersions(key, node.coordinate().version())
            );
            if (latestStable.isEmpty()) {
                return new VersionInsight(
                    node.coordinate().version(),
                    null,
                    UPDATE_UNKNOWN,
                    STATUS_UNAVAILABLE,
                    false,
                    "No stable versions available."
                );
            }
            return VersionClassifier.classify(node.coordinate().version(), latestStable.get());
        } catch (Exception exception) {
            String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
            diagnostics.add(new DiagnosticEntry("warning", "version-update-unavailable", message, nodeId));
            return new VersionInsight(
                node.coordinate().version(),
                null,
                UPDATE_UNKNOWN,
                STATUS_UNAVAILABLE,
                false,
                message
            );
        }
    }

    private static ArtifactVersionKey key(ArtifactCoordinate coordinate) {
        return new ArtifactVersionKey(coordinate.groupId(), coordinate.artifactId());
    }

    private static final class SummaryCounts {
        private int checked;
        private int current;
        private int outdated;
        private int patch;
        private int minor;
        private int major;
        private int unknown;
        private int unavailable;

        private void add(VersionInsight insight) {
            checked++;
            if (STATUS_CURRENT.equals(insight.status())) {
                current++;
                return;
            }
            if (STATUS_OUTDATED.equals(insight.status())) {
                outdated++;
                addOutdatedUpdateType(insight.updateType());
                return;
            }
            if (STATUS_UNAVAILABLE.equals(insight.status())) {
                unavailable++;
            }
        }

        private void addOutdatedUpdateType(String updateType) {
            if (UPDATE_PATCH.equals(updateType)) {
                patch++;
                return;
            }
            if (UPDATE_MINOR.equals(updateType)) {
                minor++;
                return;
            }
            if (UPDATE_MAJOR.equals(updateType)) {
                major++;
                return;
            }
            if (UPDATE_UNKNOWN.equals(updateType)) {
                unknown++;
            }
        }

        private VersionSummary toSummary() {
            return new VersionSummary(true, checked, current, outdated, patch, minor, major, unknown, unavailable);
        }
    }
}
