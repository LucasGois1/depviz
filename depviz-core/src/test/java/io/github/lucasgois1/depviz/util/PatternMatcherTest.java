package io.github.lucasgois1.depviz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import org.junit.jupiter.api.Test;

class PatternMatcherTest {
    private final ArtifactCoordinate springCore = new ArtifactCoordinate("org.springframework", "spring-core", "jar", "", "6.1.0");

    @Test
    void matchesGroupWildcard() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:*");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void matchesArtifactWildcard() {
        PatternMatcher matcher = PatternMatcher.parse("*:spring-core");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void matchesExactGroupAndArtifact() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:spring-core");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void doesNotMatchDifferentArtifact() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:spring-web");

        assertThat(matcher.matches(springCore)).isFalse();
    }

    @Test
    void matchesFullCoordinatePattern() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:spring-core:jar:6.1.0");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void parsesCommaSeparatedPatterns() {
        assertThat(PatternMatcher.parseList("org.springframework:spring-core, com.acme:*"))
            .extracting(PatternMatcher::groupId, PatternMatcher::artifactId)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("org.springframework", "spring-core"),
                org.assertj.core.groups.Tuple.tuple("com.acme", "*")
            );
    }

    @Test
    void rejectsEmptyCommaEntries() {
        assertThatThrownBy(() -> PatternMatcher.parseList("a:b,,c:d"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");

        assertThatThrownBy(() -> PatternMatcher.parseList(",a:b"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");

        assertThatThrownBy(() -> PatternMatcher.parseList("a:b,"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");
    }

    @Test
    void returnsEmptyListForBlankPatternList() {
        assertThat(PatternMatcher.parseList(null)).isEmpty();
        assertThat(PatternMatcher.parseList("  ")).isEmpty();
    }

    @Test
    void rejectsBlankSegments() {
        assertThatThrownBy(() -> PatternMatcher.parse("org.springframework: "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");
    }

    @Test
    void matchesEmbeddedGroupWildcard() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework*:spring-core");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void matchesEmbeddedArtifactSuffixWildcard() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:*-core");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void matchesEmbeddedArtifactContainsWildcard() {
        PatternMatcher matcher = PatternMatcher.parse("org.springframework:*spring*");

        assertThat(matcher.matches(springCore)).isTrue();
    }

    @Test
    void treatsDotsAsLiteralCharactersInGlobPatterns() {
        PatternMatcher matcher = PatternMatcher.parse("orgxspringframework*:spring-core");

        assertThat(matcher.matches(springCore)).isFalse();
    }

    @Test
    void rejectsInvalidPattern() {
        assertThatThrownBy(() -> PatternMatcher.parse("only-one-segment"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");
    }
}
