package dev.gois.tools.depviz.util;

import dev.gois.tools.depviz.graph.ArtifactCoordinate;

public final class Coordinates {
    private Coordinates() {
    }

    public static String stableId(ArtifactCoordinate coordinate) {
        return coordinate.groupId()
            + ":"
            + coordinate.artifactId()
            + ":"
            + coordinate.type()
            + ":"
            + coordinate.classifier()
            + ":"
            + coordinate.version();
    }

    public static String displayCoordinate(ArtifactCoordinate coordinate) {
        if (coordinate.classifier().isBlank()) {
            return coordinate.groupId()
                + ":"
                + coordinate.artifactId()
                + ":"
                + coordinate.type()
                + ":"
                + coordinate.version();
        }
        return coordinate.groupId()
            + ":"
            + coordinate.artifactId()
            + ":"
            + coordinate.type()
            + ":"
            + coordinate.classifier()
            + ":"
            + coordinate.version();
    }

    public static String label(ArtifactCoordinate coordinate) {
        return coordinate.groupId() + ":" + coordinate.artifactId() + "\n" + coordinate.version();
    }
}
