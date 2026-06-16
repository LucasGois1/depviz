package io.github.lucasgois1.depviz.util;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public record PatternMatcher(String groupId, String artifactId, String type, String version) {
    public static List<PatternMatcher> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",", -1))
            .map(String::trim)
            .map(PatternMatcher::parse)
            .toList();
    }

    public static PatternMatcher parse(String raw) {
        String[] parts = raw == null ? new String[0] : raw.trim().split(":", -1);
        if (parts.length != 2 && parts.length != 4) {
            throw new IllegalArgumentException(
                "Invalid dependency pattern '" + raw + "'. Use groupId:artifactId or groupId:artifactId:type:version."
            );
        }
        return new PatternMatcher(
            normalize(parts[0]),
            normalize(parts[1]),
            parts.length == 4 ? normalize(parts[2]) : "*",
            parts.length == 4 ? normalize(parts[3]) : "*"
        );
    }

    public boolean matches(ArtifactCoordinate coordinate) {
        return segmentMatches(groupId, coordinate.groupId())
            && segmentMatches(artifactId, coordinate.artifactId())
            && segmentMatches(type, coordinate.type())
            && segmentMatches(version, coordinate.version());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid dependency pattern segment.");
        }
        return value.trim();
    }

    private static boolean segmentMatches(String pattern, String value) {
        if (!pattern.contains("*")) {
            return pattern.equals(value);
        }
        String regex = Arrays.stream(pattern.split("\\*", -1))
            .map(Pattern::quote)
            .reduce((left, right) -> left + ".*" + right)
            .orElse("");
        return value.matches(regex);
    }
}
