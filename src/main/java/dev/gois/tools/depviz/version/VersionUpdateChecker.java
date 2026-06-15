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
import java.util.Objects;
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
        this.lookup = Objects.requireNonNull(lookup, "lookup is required.");
    }

    public VersionCheckResult check(ExtractedDependencyNode root, boolean enabled) {
        if (!enabled) {
            return VersionCheckResult.empty(false);
        }
        if (root == null) {
            return VersionCheckResult.empty(true);
        }

        Map<String, VersionInsight> insightsByNodeId = new LinkedHashMap<>();
        Map<ArtifactVersionKey, ArtifactLookupResult> lookupsByArtifact = new HashMap<>();
        List<DiagnosticEntry> diagnostics = new ArrayList<>();
        SummaryCounts counts = new SummaryCounts();

        ArrayDeque<ExtractedDependencyNode> nodes = new ArrayDeque<>(root.children());
        while (!nodes.isEmpty()) {
            ExtractedDependencyNode node = nodes.removeFirst();
            nodes.addAll(node.children());

            String nodeId = Coordinates.stableId(node.coordinate());
            ArtifactLookupResult lookupResult = lookupsByArtifact.computeIfAbsent(
                key(node.coordinate()),
                artifactKey -> lookupArtifact(artifactKey, node.coordinate().version())
            );
            VersionInsight insight = insightForNode(node, nodeId, lookupResult, diagnostics);
            insightsByNodeId.put(nodeId, insight);
            counts.add(insight);
        }

        return new VersionCheckResult(insightsByNodeId, counts.toSummary(), diagnostics);
    }

    private ArtifactLookupResult lookupArtifact(
        ArtifactVersionKey key,
        String currentVersion
    ) {
        try {
            Optional<String> latestStable = VersionClassifier.latestStable(
                lookup.availableVersions(key, currentVersion)
            );
            if (latestStable.isEmpty()) {
                return ArtifactLookupResult.unavailable("No stable versions available.", false);
            }
            return ArtifactLookupResult.available(latestStable.get());
        } catch (Exception exception) {
            String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
            return ArtifactLookupResult.unavailable(message, true);
        }
    }

    private VersionInsight insightForNode(
        ExtractedDependencyNode node,
        String nodeId,
        ArtifactLookupResult lookupResult,
        List<DiagnosticEntry> diagnostics
    ) {
        if (lookupResult.latestStableVersion() != null) {
            return VersionClassifier.classify(node.coordinate().version(), lookupResult.latestStableVersion());
        }

        if (lookupResult.lookupFailed()) {
            String message = lookupResult.unavailableMessage();
            diagnostics.add(new DiagnosticEntry("warning", "version-update-unavailable", message, nodeId));
        }

        return new VersionInsight(
            node.coordinate().version(),
            null,
            UPDATE_UNKNOWN,
            STATUS_UNAVAILABLE,
            false,
            lookupResult.unavailableMessage()
        );
    }

    private static ArtifactVersionKey key(ArtifactCoordinate coordinate) {
        return new ArtifactVersionKey(coordinate.groupId(), coordinate.artifactId());
    }

    private record ArtifactLookupResult(
        String latestStableVersion,
        String unavailableMessage,
        boolean lookupFailed
    ) {
        private static ArtifactLookupResult available(String latestStableVersion) {
            return new ArtifactLookupResult(latestStableVersion, null, false);
        }

        private static ArtifactLookupResult unavailable(String message, boolean lookupFailed) {
            return new ArtifactLookupResult(null, message, lookupFailed);
        }
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
