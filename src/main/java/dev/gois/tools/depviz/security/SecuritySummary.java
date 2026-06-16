package dev.gois.tools.depviz.security;

public record SecuritySummary(
    boolean enabled,
    String source,
    boolean checked,
    int vulnerableNodes,
    int affectedModules,
    int critical,
    int high,
    int medium,
    int low,
    int unmappedFindings
) {
}
