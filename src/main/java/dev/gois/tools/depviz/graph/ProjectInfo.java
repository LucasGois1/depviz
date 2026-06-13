package dev.gois.tools.depviz.graph;

import java.util.List;

public record ProjectInfo(
    String groupId,
    String artifactId,
    String version,
    String packaging,
    String name,
    String basedir,
    boolean multiModule,
    List<String> modules
) {
    public ProjectInfo {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
