package io.github.lucasgois1.depviz.version;

public record VersionInsight(
    String currentVersion,
    String latestVersion,
    String updateType,
    String status,
    boolean checked,
    String message
) {}
