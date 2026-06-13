package dev.gois.tools.depviz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gois.tools.depviz.graph.ArtifactCoordinate;
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
    void rejectsInvalidPattern() {
        assertThatThrownBy(() -> PatternMatcher.parse("only-one-segment"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid dependency pattern");
    }
}
