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

    @Test
    void normalizesBlankTypeClassifierAndVersion() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate(" org.example ", " demo ", " ", " tests ", " 1.0.0-SNAPSHOT ");

        assertThat(coordinate.groupId()).isEqualTo("org.example");
        assertThat(coordinate.artifactId()).isEqualTo("demo");
        assertThat(coordinate.type()).isEqualTo("jar");
        assertThat(coordinate.classifier()).isEqualTo("tests");
        assertThat(coordinate.version()).isEqualTo("1.0.0-SNAPSHOT");
    }

    @Test
    void storesProvidedVersionWithoutSnapshotTimestampConversion() {
        ArtifactCoordinate coordinate = new ArtifactCoordinate(
            "org.example",
            "demo",
            "jar",
            "",
            "1.0.0-20240613.120000-1"
        );

        assertThat(coordinate.version()).isEqualTo("1.0.0-20240613.120000-1");
    }
}
