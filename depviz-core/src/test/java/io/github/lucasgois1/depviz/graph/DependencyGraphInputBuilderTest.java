package io.github.lucasgois1.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyGraphInputBuilderTest {
    @Test
    void buildsDocumentFromBuildToolNeutralInput() {
        DependencyNodeInput root = new DependencyNodeInput(
            new ArtifactCoordinate("com.acme", "app", "jar", "", "1.0.0"),
            "root",
            false,
            false,
            List.of(new DependencyNodeInput(
                new ArtifactCoordinate("org.slf4j", "slf4j-api", "jar", "", "2.0.13"),
                "runtime",
                false,
                false,
                List.of(),
                List.of()
            )),
            List.of()
        );
        DependencyGraphInput input = new DependencyGraphInput(
            root,
            new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", "/tmp/app", false, List.of())
        );

        GraphDocument document = new GraphDocumentBuilder().build(
            input,
            DepvizConfig.fromRaw(null, "false", null, null, null, null, null, null, Path.of("target/depviz"), "false", null, null, null, null),
            VersionCheckResult.disabled()
        );

        assertThat(document.nodes()).extracting(GraphNode::artifactId).contains("app", "slf4j-api");
        assertThat(document.edges()).singleElement().satisfies(edge -> {
            assertThat(edge.source()).isEqualTo("com.acme:app:jar::1.0.0");
            assertThat(edge.target()).isEqualTo("org.slf4j:slf4j-api:jar::2.0.13");
        });
    }

    @Test
    void usesExplicitModuleRootWhenScopeIsNotModule() {
        DependencyGraphInput input = input(new DependencyNodeInput(
            new ArtifactCoordinate("com.acme", "api", "jar", "", "1.0.0"),
            "runtime",
            false,
            true,
            List.of(),
            List.of()
        ));

        GraphDocument document = new GraphDocumentBuilder().build(input, defaultConfig(), VersionCheckResult.disabled());

        assertThat(document.nodes()).singleElement().satisfies(node -> {
            assertThat(node.scope()).isEqualTo("root");
            assertThat(node.moduleRoot()).isTrue();
        });
    }

    @Test
    void doesNotInferModuleRootFromModuleScope() {
        DependencyGraphInput input = input(new DependencyNodeInput(
            new ArtifactCoordinate("com.acme", "api", "jar", "", "1.0.0"),
            "module",
            false,
            false,
            List.of(),
            List.of()
        ));

        GraphDocument document = new GraphDocumentBuilder().build(input, defaultConfig(), VersionCheckResult.disabled());

        assertThat(document.nodes()).singleElement().satisfies(node -> {
            assertThat(node.scope()).isEqualTo("root");
            assertThat(node.moduleRoot()).isFalse();
        });
    }

    private static DependencyGraphInput input(DependencyNodeInput root) {
        return new DependencyGraphInput(
            root,
            new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", "/tmp/app", false, List.of())
        );
    }

    private static DepvizConfig defaultConfig() {
        return DepvizConfig.fromRaw(
            null,
            "false",
            null,
            null,
            null,
            null,
            null,
            null,
            Path.of("target/depviz"),
            "false",
            null,
            null,
            null,
            null
        );
    }
}
