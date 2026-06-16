package io.github.lucasgois1.depviz.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.graph.GraphDocumentBuilder;
import io.github.lucasgois1.depviz.graph.GraphNode;
import io.github.lucasgois1.depviz.graph.GraphPath;
import io.github.lucasgois1.depviz.graph.GraphSummary;
import io.github.lucasgois1.depviz.graph.ProjectInfo;
import io.github.lucasgois1.depviz.graph.ViewerConfig;
import io.github.lucasgois1.depviz.version.VersionSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityGraphEnricherTest {
    @Test
    void mapsExactPackageVersionToDependencyAndAggregatesModuleImpact() {
        GraphDocument document = fixtureDocument();
        SecurityFinding finding = finding("SNYK-JAVA-SHARED-1", SecuritySeverity.HIGH, "org.shared:shared-lib", "2.0.0");

        GraphDocument enriched = new SecurityGraphEnricher().enrich(document, SecurityCheckResult.checked(List.of(finding), List.of()));

        GraphNode shared = node(enriched, "org.shared:shared-lib:jar::2.0.0");
        GraphNode api = node(enriched, "com.acme:api:jar::1.0.0");
        GraphNode worker = node(enriched, "com.acme:worker:jar::1.0.0");
        assertThat(shared.securityInsight().maxSeverity()).isEqualTo(SecuritySeverity.HIGH);
        assertThat(api.securityInsight().maxSeverity()).isEqualTo(SecuritySeverity.HIGH);
        assertThat(worker.securityInsight().maxSeverity()).isEqualTo(SecuritySeverity.HIGH);
        assertThat(enriched.securitySummary().vulnerableNodes()).isEqualTo(1);
        assertThat(enriched.securitySummary().affectedModules()).isEqualTo(2);
        assertThat(enriched.securitySummary().high()).isEqualTo(1);
    }

    @Test
    void unmappedFindingsBecomeDiagnostics() {
        GraphDocument document = fixtureDocument();
        List<SecurityFinding> findings = List.of(
            finding("SNYK-JAVA-MISSING-1", SecuritySeverity.LOW, "org.missing:lib-1", "9.9.9"),
            finding("SNYK-JAVA-MISSING-2", SecuritySeverity.LOW, "org.missing:lib-2", "9.9.9"),
            finding("SNYK-JAVA-MISSING-3", SecuritySeverity.LOW, "org.missing:lib-3", "9.9.9"),
            finding("SNYK-JAVA-MISSING-4", SecuritySeverity.LOW, "org.missing:lib-4", "9.9.9"),
            finding("SNYK-JAVA-MISSING-5", SecuritySeverity.LOW, "org.missing:lib-5", "9.9.9"),
            finding("SNYK-JAVA-MISSING-6", SecuritySeverity.LOW, "org.missing:lib-6", "9.9.9")
        );

        GraphDocument enriched = new SecurityGraphEnricher().enrich(document, SecurityCheckResult.checked(findings, List.of()));

        assertThat(enriched.securitySummary().unmappedFindings()).isEqualTo(6);
        assertThat(enriched.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.type()).isEqualTo("snyk-unmapped-findings");
            assertThat(diagnostic.message()).contains("SNYK-JAVA-MISSING-1");
            assertThat(diagnostic.message()).contains("SNYK-JAVA-MISSING-6");
        });
    }

    private static GraphNode node(GraphDocument document, String id) {
        return document.nodes().stream().filter(current -> current.id().equals(id)).findFirst().orElseThrow();
    }

    private static SecurityFinding finding(String id, SecuritySeverity severity, String packageName, String version) {
        return new SecurityFinding(id, severity, "Finding " + id, packageName, version, List.of("9.9.10"), "https://security.snyk.io/vuln/" + id);
    }

    private static GraphDocument fixtureDocument() {
        GraphNode root = graphNode("com.acme:platform-reactor:reactor::1.0.0", "com.acme", "platform-reactor", "1.0.0", "reactor", "root", true, false);
        GraphNode api = graphNode("com.acme:api:jar::1.0.0", "com.acme", "api", "1.0.0", "jar", "module", false, true);
        GraphNode worker = graphNode("com.acme:worker:jar::1.0.0", "com.acme", "worker", "1.0.0", "jar", "module", false, true);
        GraphNode shared = graphNode("org.shared:shared-lib:jar::2.0.0", "org.shared", "shared-lib", "2.0.0", "jar", "compile", false, false);
        return new GraphDocument(
            GraphDocumentBuilder.SCHEMA_VERSION,
            Instant.EPOCH,
            new ProjectInfo("com.acme", "platform", "1.0.0", "pom", "platform", ".", true, List.of("api", "worker")),
            new GraphSummary(4, 4, Map.of("root", 1, "module", 2, "compile", 1), Map.of("com.acme", 3, "org.shared", 1)),
            new ViewerConfig("breadthfirst", 500, "artifact"),
            List.of(root, api, worker, shared),
            List.of(),
            List.of(
                new GraphPath("org.shared:shared-lib:jar::2.0.0", List.of(root.id(), api.id(), shared.id())),
                new GraphPath("org.shared:shared-lib:jar::2.0.0", List.of(root.id(), worker.id(), shared.id()))
            ),
            VersionSummary.disabled(),
            null,
            List.of()
        );
    }

    private static GraphNode graphNode(String id, String groupId, String artifactId, String version, String type, String scope, boolean root, boolean moduleRoot) {
        return new GraphNode(id, groupId, artifactId, version, type, "", scope, false, root ? 0 : moduleRoot ? 1 : 2, root, moduleRoot, artifactId, id, groupId, null, null);
    }
}
