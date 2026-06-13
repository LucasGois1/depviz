package dev.gois.tools.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizScope;
import dev.gois.tools.depviz.util.PatternMatcher;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraphFiltersTest {
    @Test
    void filtersByRuntimeScope() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("junit", "junit", "4.13.2", "test")
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(root, DepvizScope.RUNTIME, List.of(), List.of());

        assertThat(filtered.children()).isEmpty();
    }

    @Test
    void includePreservesPathToMatchingDependency() {
        ExtractedDependencyNode jackson = node("com.fasterxml.jackson.core", "jackson-databind", "2.17.0", "compile");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("com.acme", "feature", "1.0.0", "compile", jackson)
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(
            root,
            DepvizScope.ALL,
            PatternMatcher.parseList("com.fasterxml.jackson.core:*"),
            List.of()
        );

        assertThat(filtered.children()).hasSize(1);
        assertThat(filtered.children().get(0).children()).hasSize(1);
    }

    @Test
    void excludeRemovesExcludedBranch() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("org.mockito", "mockito-core", "5.0.0", "test"),
            node("org.slf4j", "slf4j-api", "2.0.13", "compile")
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(
            root,
            DepvizScope.ALL,
            List.of(),
            PatternMatcher.parseList("org.mockito:*")
        );

        assertThat(filtered.children()).extracting(child -> child.coordinate().groupId()).containsExactly("org.slf4j");
    }

    private static ExtractedDependencyNode node(
        String groupId,
        String artifactId,
        String version,
        String scope,
        ExtractedDependencyNode... children
    ) {
        return new ExtractedDependencyNode(
            new ArtifactCoordinate(groupId, artifactId, "jar", "", version),
            scope,
            false,
            List.of(children),
            List.of()
        );
    }
}
