package io.github.lucasgois1.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.security.SecurityFinding;
import io.github.lucasgois1.depviz.security.SecurityInsight;
import io.github.lucasgois1.depviz.security.SecuritySeverity;
import io.github.lucasgois1.depviz.security.SecuritySummary;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import io.github.lucasgois1.depviz.version.VersionInsight;
import io.github.lucasgois1.depviz.version.VersionSummary;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraphDocumentBuilderTest {
    private final DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, Path.of("target/depviz"));

    @Test
    void createsRootNode() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0");

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.nodes()).hasSize(1);
        assertThat(document.nodes().get(0).root()).isTrue();
        assertThat(document.nodes().get(0).scope()).isEqualTo("root");
        assertThat(document.nodes().get(0).id()).isEqualTo("com.acme:app:jar::1.0.0");
        assertThat(document.summary().nodesByScope()).containsEntry("root", 1);
    }

    @Test
    void existingBuilderOverloadUsesEmptyVersionSummary() {
        DepvizConfig disabledUpdates = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, "false", Path.of("target/depviz"));
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0");

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), disabledUpdates);

        assertThat(document.versionSummary()).isEqualTo(VersionSummary.disabled());
        assertThat(document.nodes())
            .extracting(GraphNode::versionInsight)
            .containsExactly((VersionInsight) null);
    }

    @Test
    void graphDocumentAcceptsOptionalSecuritySummaryAndInsight() {
        SecurityFinding finding = new SecurityFinding(
            "SNYK-JAVA-DEMO-1",
            SecuritySeverity.HIGH,
            "Demo vulnerability",
            "org.example:lib",
            "1.0.0",
            List.of("1.0.1"),
            "https://security.snyk.io/vuln/SNYK-JAVA-DEMO-1"
        );
        SecurityInsight insight = SecurityInsight.vulnerable(List.of(finding));
        GraphNode node = new GraphNode(
            "org.example:lib:jar::1.0.0",
            "org.example",
            "lib",
            "1.0.0",
            "jar",
            "",
            "compile",
            false,
            1,
            false,
            false,
            "org.example:lib",
            "org.example:lib:jar:1.0.0",
            "org.example",
            null,
            insight
        );

        GraphDocument document = new GraphDocument(
            GraphDocumentBuilder.SCHEMA_VERSION,
            Instant.EPOCH,
            projectInfo(),
            new GraphSummary(1, 0, Map.of("compile", 1), Map.of("org.example", 1)),
            new ViewerConfig("breadthfirst", 500, "artifact"),
            List.of(node),
            List.of(),
            List.of(),
            VersionSummary.disabled(),
            new SecuritySummary(true, "snyk", true, 1, 0, 0, 1, 0, 0, 0),
            List.of()
        );

        assertThat(document.securitySummary().high()).isEqualTo(1);
        assertThat(document.nodes().get(0).securityInsight().maxSeverity()).isEqualTo(SecuritySeverity.HIGH);
    }

    @Test
    void existingBuilderOverloadValidatesNullConfigWithExplicitMessage() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0");

        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new GraphDocumentBuilder().build(root, projectInfo(), null))
            .withMessage("config is required.");
    }

    @Test
    void attachesVersionInsightAndSummaryWhenVersionCheckIsSupplied() {
        ExtractedDependencyNode dependency = node("org.example", "lib", "1.0.0");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", dependency);
        VersionInsight insight = new VersionInsight("1.0.0", "1.0.1", "patch", "outdated", true, "Patch update available.");
        VersionSummary summary = new VersionSummary(true, 1, 0, 1, 1, 0, 0, 0, 0);
        VersionCheckResult versionCheck = new VersionCheckResult(
            Map.of("org.example:lib:jar::1.0.0", insight),
            summary,
            List.of()
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config, versionCheck);

        GraphNode lib = document.nodes().stream()
            .filter(node -> node.id().equals("org.example:lib:jar::1.0.0"))
            .findFirst()
            .orElseThrow();
        assertThat(lib.versionInsight()).isEqualTo(insight);
        assertThat(document.versionSummary()).isEqualTo(summary);
    }

    @Test
    void deduplicatesSameArtifactAndPreservesMultipleIncomingEdges() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0",
            node("com.acme", "feature-a", "1.0.0", shared),
            node("com.acme", "feature-b", "1.0.0", shared)
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.nodes())
            .extracting(GraphNode::id)
            .containsOnlyOnce("org.slf4j:slf4j-api:jar::2.0.13");
        assertThat(document.edges())
            .filteredOn(edge -> edge.target().equals("org.slf4j:slf4j-api:jar::2.0.13"))
            .hasSize(2);
    }

    @Test
    void aggregateRootKeepsModuleRootsAndDeduplicatesSharedDependencies() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode aggregate = new ExtractedDependencyNode(
            new ArtifactCoordinate("com.acme", "platform-reactor", "reactor", "", "1.0.0"),
            "root",
            false,
            List.of(
                new ExtractedDependencyNode(new ArtifactCoordinate("com.acme", "api", "jar", "", "1.0.0"), "module", false, List.of(shared), List.of()),
                new ExtractedDependencyNode(new ArtifactCoordinate("com.acme", "worker", "jar", "", "1.0.0"), "module", false, List.of(shared), List.of())
            ),
            List.of()
        );

        GraphDocument document = new GraphDocumentBuilder().build(aggregate, projectInfo(), config);

        assertThat(document.nodes()).filteredOn(GraphNode::root).extracting(GraphNode::id).containsExactly("com.acme:platform-reactor:reactor::1.0.0");
        assertThat(document.nodes()).filteredOn(GraphNode::moduleRoot).extracting(GraphNode::artifactId).containsExactlyInAnyOrder("api", "worker");
        assertThat(document.nodes()).extracting(GraphNode::id).containsOnlyOnce("org.slf4j:slf4j-api:jar::2.0.13");
        assertThat(document.edges()).filteredOn(edge -> edge.target().equals("org.slf4j:slf4j-api:jar::2.0.13")).hasSize(2);
    }

    @Test
    void preventsDuplicateEdges() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", shared, shared);

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.edges()).hasSize(1);
    }

    @Test
    void preservesSameSourceAndTargetEdgesWithDifferentMetadata() {
        ExtractedDependencyNode compileShared = node("org.slf4j", "slf4j-api", "2.0.13", "compile", false);
        ExtractedDependencyNode runtimeShared = node("org.slf4j", "slf4j-api", "2.0.13", "runtime", false);
        ExtractedDependencyNode optionalShared = node("org.slf4j", "slf4j-api", "2.0.13", "compile", true);
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", compileShared, runtimeShared, optionalShared);

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.edges())
            .filteredOn(edge -> edge.target().equals("org.slf4j:slf4j-api:jar::2.0.13"))
            .extracting(edge -> edge.scope() + ":" + edge.optional())
            .containsExactlyInAnyOrder("compile:false", "runtime:false", "compile:true");
    }

    @Test
    void computesShortestDepth() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0",
            shared,
            node("com.acme", "feature", "1.0.0", shared)
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        GraphNode slf4j = document.nodes().stream()
            .filter(node -> node.id().equals("org.slf4j:slf4j-api:jar::2.0.13"))
            .findFirst()
            .orElseThrow();
        assertThat(slf4j.depth()).isEqualTo(1);
    }

    @Test
    void recordsAllPathsToSharedNode() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0",
            node("com.acme", "feature-a", "1.0.0", shared),
            node("com.acme", "feature-b", "1.0.0", shared)
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.paths())
            .filteredOn(path -> path.target().equals("org.slf4j:slf4j-api:jar::2.0.13"))
            .extracting(GraphPath::nodeIds)
            .containsExactlyInAnyOrder(
                List.of(
                    "com.acme:app:jar::1.0.0",
                    "com.acme:feature-a:jar::1.0.0",
                    "org.slf4j:slf4j-api:jar::2.0.13"
                ),
                List.of(
                    "com.acme:app:jar::1.0.0",
                    "com.acme:feature-b:jar::1.0.0",
                    "org.slf4j:slf4j-api:jar::2.0.13"
                )
            );
    }

    @Test
    void stopsTraversalWhenCoordinateCycleReentersActivePath() {
        ExtractedDependencyNode cycleRootOccurrence = node(
            "com.acme",
            "app",
            "1.0.0",
            node("com.acme", "after-cycle", "1.0.0")
        );
        ExtractedDependencyNode lib = node("com.acme", "lib", "1.0.0", cycleRootOccurrence);
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", lib);

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.edges())
            .anySatisfy(edge -> {
                assertThat(edge.source()).isEqualTo("com.acme:lib:jar::1.0.0");
                assertThat(edge.target()).isEqualTo("com.acme:app:jar::1.0.0");
            });
        assertThat(document.paths())
            .anySatisfy(path -> assertThat(path.nodeIds()).containsExactly(
                "com.acme:app:jar::1.0.0",
                "com.acme:lib:jar::1.0.0",
                "com.acme:app:jar::1.0.0"
            ));
        assertThat(document.nodes())
            .extracting(GraphNode::id)
            .doesNotContain("com.acme:after-cycle:jar::1.0.0");
    }

    @Test
    void mapsViewerConfigFromDepvizConfig() {
        DepvizConfig customConfig = DepvizConfig.fromRaw(null, "false", null, "force", null, "42", null, Path.of("target/depviz"));
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0");

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), customConfig);

        assertThat(document.viewerConfig()).isEqualTo(new ViewerConfig("force", 42, "artifact"));
    }

    @Test
    void summarizesNodesEdgesGroupsAndScopes() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0",
            node("com.acme", "feature", "1.0.0"),
            node("org.slf4j", "slf4j-api", "2.0.13", "runtime", false)
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.summary().nodeCount()).isEqualTo(3);
        assertThat(document.summary().edgeCount()).isEqualTo(2);
        assertThat(document.summary().nodesByScope()).containsEntry("root", 1).containsEntry("compile", 1).containsEntry("runtime", 1);
        assertThat(document.summary().nodesByGroupId()).containsEntry("com.acme", 2).containsEntry("org.slf4j", 1);
    }

    @Test
    void carriesDiagnosticsIntoDocument() {
        ExtractedDependencyNode root = node(
            "com.acme",
            "app",
            "1.0.0",
            "compile",
            false,
            List.of(new DiagnosticEntry("warning", "version", "Version was inferred.", null))
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.diagnostics())
            .containsExactly(new DiagnosticEntry("warning", "version", "Version was inferred.", "com.acme:app:jar::1.0.0"));
    }

    @Test
    void appendsVersionDiagnosticsToExistingDiagnostics() {
        ExtractedDependencyNode root = node(
            "com.acme",
            "app",
            "1.0.0",
            "compile",
            false,
            List.of(new DiagnosticEntry("warning", "version", "Version was inferred.", null))
        );
        VersionCheckResult versionCheck = new VersionCheckResult(
            Map.of(),
            VersionSummary.disabled(),
            List.of(new DiagnosticEntry("warning", "version-update-unavailable", "metadata failed", "com.acme:app:jar::1.0.0"))
        );

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config, versionCheck);

        assertThat(document.diagnostics()).containsExactly(
            new DiagnosticEntry("warning", "version", "Version was inferred.", "com.acme:app:jar::1.0.0"),
            new DiagnosticEntry("warning", "version-update-unavailable", "metadata failed", "com.acme:app:jar::1.0.0")
        );
    }

    @Test
    void exposesImmutableCollections() {
        List<ExtractedDependencyNode> children = new ArrayList<>();
        children.add(node("com.acme", "lib", "1.0.0"));
        ExtractedDependencyNode root = new ExtractedDependencyNode(
            new ArtifactCoordinate("com.acme", "app", "jar", "", "1.0.0"),
            "compile",
            false,
            children,
            List.of()
        );
        children.add(node("com.acme", "late", "1.0.0"));

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.nodes())
            .extracting(GraphNode::id)
            .doesNotContain("com.acme:late:jar::1.0.0");
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> document.nodes().add(document.nodes().get(0)));
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> document.paths().get(0).nodeIds().add("other"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> document.summary().nodesByScope().put("other", 1));
    }

    private static ProjectInfo projectInfo() {
        return new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", ".", false, List.of());
    }

    private static ExtractedDependencyNode node(String groupId, String artifactId, String version, ExtractedDependencyNode... children) {
        return node(groupId, artifactId, version, "compile", false, List.of(), children);
    }

    private static ExtractedDependencyNode node(String groupId, String artifactId, String version, String scope, boolean optional, ExtractedDependencyNode... children) {
        return node(groupId, artifactId, version, scope, optional, List.of(), children);
    }

    private static ExtractedDependencyNode node(
        String groupId,
        String artifactId,
        String version,
        String scope,
        boolean optional,
        List<DiagnosticEntry> diagnostics,
        ExtractedDependencyNode... children
    ) {
        return new ExtractedDependencyNode(
            new ArtifactCoordinate(groupId, artifactId, "jar", "", version),
            scope,
            optional,
            List.of(children),
            diagnostics
        );
    }
}
