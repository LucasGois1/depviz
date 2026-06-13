package dev.gois.tools.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraphDocumentBuilderTest {
    private final DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, Path.of("target/depviz"));

    @Test
    void createsRootNode() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0");

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.nodes()).hasSize(1);
        assertThat(document.nodes().get(0).root()).isTrue();
        assertThat(document.nodes().get(0).id()).isEqualTo("com.acme:app:jar::1.0.0");
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
    void preventsDuplicateEdges() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", shared, shared);

        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);

        assertThat(document.edges()).hasSize(1);
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
            .hasSize(2);
    }

    private static ProjectInfo projectInfo() {
        return new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", ".", false, List.of());
    }

    private static ExtractedDependencyNode node(String groupId, String artifactId, String version, ExtractedDependencyNode... children) {
        return new ExtractedDependencyNode(
            new ArtifactCoordinate(groupId, artifactId, "jar", "", version),
            "compile",
            false,
            List.of(children),
            List.of()
        );
    }
}
