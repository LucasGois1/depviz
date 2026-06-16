package io.github.lucasgois1.depviz.security;

import java.util.List;

public record SecurityFinding(
    String id,
    SecuritySeverity severity,
    String title,
    String packageName,
    String version,
    List<String> fixedVersions,
    String url
) {
    public SecurityFinding {
        fixedVersions = fixedVersions == null ? List.of() : List.copyOf(fixedVersions);
    }
}
