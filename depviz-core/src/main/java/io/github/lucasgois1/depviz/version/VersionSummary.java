package io.github.lucasgois1.depviz.version;

public record VersionSummary(
    boolean enabled,
    int checked,
    int current,
    int outdated,
    int patch,
    int minor,
    int major,
    int unknown,
    int unavailable
) {
    public static VersionSummary disabled() {
        return new VersionSummary(false, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
