package io.github.lucasgois1.depviz.graph;

import io.github.lucasgois1.depviz.security.SecurityInsight;
import io.github.lucasgois1.depviz.version.VersionInsight;

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
    VersionInsight versionInsight,
    SecurityInsight securityInsight
) {
}
