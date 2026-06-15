package dev.gois.tools.depviz.graph;

import dev.gois.tools.depviz.version.VersionInsight;

public record GraphNode(
    String id,
    String groupId,
    String artifactId,
    String version,
    String type,
    String classifier,
    String scope,
    boolean optional,
    int depth,
    boolean root,
    boolean moduleRoot,
    String label,
    String coordinate,
    String groupColorKey,
    VersionInsight versionInsight
) {
}
