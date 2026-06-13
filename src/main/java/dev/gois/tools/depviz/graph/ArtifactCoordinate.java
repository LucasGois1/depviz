package dev.gois.tools.depviz.graph;

public record ArtifactCoordinate(
    String groupId,
    String artifactId,
    String type,
    String classifier,
    String version
) {
    public ArtifactCoordinate {
        groupId = required(groupId, "groupId");
        artifactId = required(artifactId, "artifactId");
        type = type == null || type.isBlank() ? "jar" : type.trim();
        classifier = classifier == null ? "" : classifier.trim();
        version = required(version, "version");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }
}
