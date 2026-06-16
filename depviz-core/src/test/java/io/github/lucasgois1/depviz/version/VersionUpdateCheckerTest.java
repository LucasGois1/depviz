package io.github.lucasgois1.depviz.version;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import io.github.lucasgois1.depviz.graph.DiagnosticEntry;
import io.github.lucasgois1.depviz.graph.ExtractedDependencyNode;
import io.github.lucasgois1.depviz.util.Coordinates;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VersionUpdateCheckerTest {
    @Test
    void marksSharedArtifactsOnceAndAppliesSameInsightToRepeatedGraphNodes() {
        ArtifactCoordinate firstCoordinate = coordinate("org.example", "shared", "1.0.0");
        ArtifactCoordinate repeatedCoordinate = coordinate("org.example", "shared", "1.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "shared"), List.of("1.0.0", "1.0.1")),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            root(node(firstCoordinate), node(repeatedCoordinate)),
            true
        );

        assertThat(lookup.calls()).isEqualTo(1);
        assertThat(result.insightsByNodeId())
            .containsOnlyKeys(Coordinates.stableId(firstCoordinate));
        VersionInsight insight = result.insightsByNodeId().get(Coordinates.stableId(firstCoordinate));
        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("patch");
        assertThat(result.summary().checked()).isEqualTo(2);
        assertThat(result.summary().patch()).isEqualTo(2);
    }

    @Test
    void reusesArtifactLookupButClassifiesEachNodeVersionIndependently() {
        ArtifactCoordinate oldVersion = coordinate("org.example", "shared", "1.0.0");
        ArtifactCoordinate latestVersion = coordinate("org.example", "shared", "2.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "shared"), List.of("1.0.0", "2.0.0")),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            root(node(oldVersion), node(latestVersion)),
            true
        );

        assertThat(lookup.calls()).isEqualTo(1);
        assertThat(result.insightsByNodeId())
            .containsKeys(Coordinates.stableId(oldVersion), Coordinates.stableId(latestVersion));
        VersionInsight oldInsight = result.insightsByNodeId().get(Coordinates.stableId(oldVersion));
        VersionInsight latestInsight = result.insightsByNodeId().get(Coordinates.stableId(latestVersion));
        assertThat(oldInsight.currentVersion()).isEqualTo("1.0.0");
        assertThat(oldInsight.status()).isEqualTo("outdated");
        assertThat(latestInsight.currentVersion()).isEqualTo("2.0.0");
        assertThat(latestInsight.status()).isEqualTo("current");
        assertThat(result.summary()).isEqualTo(new VersionSummary(true, 2, 1, 1, 0, 0, 1, 0, 0));
    }

    @Test
    void countsCurrentPatchMinorMajorUnknownAndUnavailableSummaries() {
        ArtifactCoordinate current = coordinate("org.example", "current", "1.0.0");
        ArtifactCoordinate patch = coordinate("org.example", "patch", "1.0.0");
        ArtifactCoordinate minor = coordinate("org.example", "minor", "1.0.0");
        ArtifactCoordinate major = coordinate("org.example", "major", "1.0.0");
        ArtifactCoordinate unknown = coordinate("org.example", "unknown", "1.0.0");
        ArtifactCoordinate unavailable = coordinate("org.example", "unavailable", "1.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(
                new ArtifactVersionKey("org.example", "current"), List.of("1.0.0"),
                new ArtifactVersionKey("org.example", "patch"), List.of("1.0.1"),
                new ArtifactVersionKey("org.example", "minor"), List.of("1.1.0"),
                new ArtifactVersionKey("org.example", "major"), List.of("2.0.0"),
                new ArtifactVersionKey("org.example", "unknown"), List.of("1.0.1.Final"),
                new ArtifactVersionKey("org.example", "unavailable"), List.of("2.0.0-RC1")
            ),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            root(
                node(current),
                node(patch),
                node(minor),
                node(major),
                node(unknown),
                node(unavailable)
            ),
            true
        );

        assertThat(result.summary()).isEqualTo(new VersionSummary(true, 6, 1, 4, 1, 1, 1, 1, 1));
        assertThat(result.insightsByNodeId().get(Coordinates.stableId(unavailable)).status())
            .isEqualTo("unavailable");
        assertThat(result.insightsByNodeId().get(Coordinates.stableId(unavailable)).updateType())
            .isEqualTo("unknown");
    }

    @Test
    void skipsTheRootProjectNode() {
        ArtifactCoordinate rootCoordinate = coordinate("org.example", "root", "1.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "root"), List.of("2.0.0")),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            new ExtractedDependencyNode(rootCoordinate, null, false, List.of(), List.of()),
            true
        );

        assertThat(lookup.calls()).isZero();
        assertThat(result.insightsByNodeId()).isEmpty();
        assertThat(result.summary()).isEqualTo(new VersionSummary(true, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void skipsAggregateRootAndModuleNodesButChecksModuleDependencies() {
        ArtifactCoordinate module = coordinate("org.example", "api", "1.0.0");
        ArtifactCoordinate dependency = coordinate("org.example", "dependency", "1.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "dependency"), List.of("1.0.1")),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            new ExtractedDependencyNode(
                new ArtifactCoordinate("org.example", "platform-reactor", "reactor", "", "1.0.0"),
                "root",
                false,
                List.of(new ExtractedDependencyNode(module, "module", false, List.of(node(dependency)), List.of())),
                List.of()
            ),
            true
        );

        assertThat(result.summary().checked()).isEqualTo(1);
        assertThat(result.insightsByNodeId()).doesNotContainKey(Coordinates.stableId(module));
        assertThat(result.insightsByNodeId()).containsOnlyKeys(Coordinates.stableId(dependency));
    }

    @Test
    void returnsDisabledSummaryNoInsightsAndDoesNotCallLookupWhenDisabled() {
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "dependency"), List.of("2.0.0")),
            Set.of()
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            root(node(coordinate("org.example", "dependency", "1.0.0"))),
            false
        );

        assertThat(lookup.calls()).isZero();
        assertThat(result.summary()).isEqualTo(VersionSummary.disabled());
        assertThat(result.insightsByNodeId()).isEmpty();
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void convertsLookupExceptionsForOneArtifactIntoUnavailable() {
        ArtifactCoordinate failing = coordinate("org.example", "failing", "1.0.0");
        ArtifactCoordinate patch = coordinate("org.example", "patch", "1.0.0");
        FakeVersionLookup lookup = new FakeVersionLookup(
            Map.of(new ArtifactVersionKey("org.example", "patch"), List.of("1.0.1")),
            Set.of(new ArtifactVersionKey("org.example", "failing"))
        );

        VersionCheckResult result = new VersionUpdateChecker(lookup).check(
            root(node(failing), node(patch)),
            true
        );

        VersionInsight failingInsight = result.insightsByNodeId().get(Coordinates.stableId(failing));
        assertThat(failingInsight).isEqualTo(
            new VersionInsight("1.0.0", null, "unknown", "unavailable", false, "metadata failed")
        );
        assertThat(result.summary()).isEqualTo(new VersionSummary(true, 2, 0, 1, 1, 0, 0, 0, 1));
        assertThat(result.diagnostics()).containsExactly(
            new DiagnosticEntry(
                "warning",
                "version-update-unavailable",
                "metadata failed",
                Coordinates.stableId(failing)
            )
        );
    }

    private static ExtractedDependencyNode root(ExtractedDependencyNode... children) {
        return new ExtractedDependencyNode(
            coordinate("org.example", "root", "1.0.0"),
            null,
            false,
            List.of(children),
            List.of()
        );
    }

    private static ExtractedDependencyNode node(ArtifactCoordinate coordinate) {
        return new ExtractedDependencyNode(coordinate, "compile", false, List.of(), List.of());
    }

    private static ArtifactCoordinate coordinate(String groupId, String artifactId, String version) {
        return new ArtifactCoordinate(groupId, artifactId, "jar", "", version);
    }

    private static final class FakeVersionLookup implements VersionLookup {
        private final Map<ArtifactVersionKey, List<String>> versions;
        private final Set<ArtifactVersionKey> failingKeys;
        private int calls;

        private FakeVersionLookup(Map<ArtifactVersionKey, List<String>> versions, Set<ArtifactVersionKey> failingKeys) {
            this.versions = versions;
            this.failingKeys = failingKeys;
        }

        @Override
        public List<String> availableVersions(ArtifactVersionKey key, String currentVersion) {
            calls++;
            if (failingKeys.contains(key)) {
                throw new RuntimeException("metadata failed");
            }
            return versions.getOrDefault(key, List.of(currentVersion));
        }

        private int calls() {
            return calls;
        }
    }
}
