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

    @Test
    void excludeWinsOverIncludeWhenDependencyMatchesBoth() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("org.slf4j", "slf4j-api", "2.0.13", "compile")
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(
            root,
            DepvizScope.ALL,
            PatternMatcher.parseList("org.slf4j:*"),
            PatternMatcher.parseList("org.slf4j:*")
        );

        assertThat(filtered.children()).isEmpty();
    }

    @Test
    void sharedDependencyRemainsThroughNonExcludedParent() {
        ExtractedDependencyNode shared = node("org.slf4j", "slf4j-api", "2.0.13", "compile");
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("com.acme", "excluded-feature", "1.0.0", "compile", shared),
            node("com.acme", "kept-feature", "1.0.0", "compile", shared)
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(
            root,
            DepvizScope.ALL,
            PatternMatcher.parseList("org.slf4j:*"),
            PatternMatcher.parseList("com.acme:excluded-feature")
        );

        assertThat(filtered.children()).extracting(child -> child.coordinate().artifactId()).containsExactly("kept-feature");
        assertThat(filtered.children().get(0).children()).extracting(child -> child.coordinate().artifactId())
            .containsExactly("slf4j-api");
    }

    @Test
    void preservesMetadataWhenCopyingNodesWithChangedChildren() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("com.acme", "feature", "jar", "tests", "1.0.0");
        List<DiagnosticEntry> diagnostics = List.of(new DiagnosticEntry("warning", "scope", "Scope was inferred.", null));
        ExtractedDependencyNode feature = new ExtractedDependencyNode(
            coordinate,
            "runtime",
            true,
            List.of(node("junit", "junit", "4.13.2", "test")),
            diagnostics
        );
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile", feature);

        ExtractedDependencyNode filtered = GraphFilters.apply(root, DepvizScope.RUNTIME, List.of(), List.of());

        ExtractedDependencyNode filteredFeature = filtered.children().get(0);
        assertThat(filteredFeature.coordinate()).isEqualTo(coordinate);
        assertThat(filteredFeature.scope()).isEqualTo("runtime");
        assertThat(filteredFeature.optional()).isTrue();
        assertThat(filteredFeature.diagnostics()).containsExactlyElementsOf(diagnostics);
        assertThat(filteredFeature.children()).isEmpty();
    }

    @Test
    void acceptsNullIncludeAndExcludeLists() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("org.slf4j", "slf4j-api", "2.0.13", "compile")
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(root, DepvizScope.ALL, null, null);

        assertThat(filtered.children()).hasSize(1);
    }

    @Test
    void noIncludeMatchReturnsRootWithEmptyChildren() {
        ExtractedDependencyNode root = node("com.acme", "app", "1.0.0", "compile",
            node("org.slf4j", "slf4j-api", "2.0.13", "compile")
        );

        ExtractedDependencyNode filtered = GraphFilters.apply(
            root,
            DepvizScope.ALL,
            PatternMatcher.parseList("com.fasterxml.jackson.core:*"),
            List.of()
        );

        assertThat(filtered.coordinate()).isEqualTo(root.coordinate());
        assertThat(filtered.children()).isEmpty();
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
