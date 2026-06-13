package dev.gois.tools.depviz.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.graph.ArtifactCoordinate;
import org.junit.jupiter.api.Test;

class CoordinatesTest {
    @Test
    void generatesStableIdWithoutClassifier() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("org.springframework", "spring-core", "jar", "", "6.1.0");

        assertThat(Coordinates.stableId(coordinate)).isEqualTo("org.springframework:spring-core:jar::6.1.0");
    }

    @Test
    void generatesStableIdWithClassifier() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("com.acme", "client", "test-jar", "tests", "1.2.3");

        assertThat(Coordinates.stableId(coordinate)).isEqualTo("com.acme:client:test-jar:tests:1.2.3");
    }

    @Test
    void generatesDisplayCoordinateWithoutClassifier() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("org.slf4j", "slf4j-api", "jar", "", "2.0.13");

        assertThat(Coordinates.displayCoordinate(coordinate)).isEqualTo("org.slf4j:slf4j-api:jar:2.0.13");
    }

    @Test
    void generatesLabel() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("org.slf4j", "slf4j-api", "jar", "", "2.0.13");

        assertThat(Coordinates.label(coordinate)).isEqualTo("org.slf4j:slf4j-api\n2.0.13");
    }
}
