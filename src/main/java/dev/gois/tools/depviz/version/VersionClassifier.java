package dev.gois.tools.depviz.version;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

public final class VersionClassifier {
    private static final String CURRENT = "current";
    private static final String OUTDATED = "outdated";
    private static final String NONE = "none";
    private static final String UNKNOWN = "unknown";
    private static final Pattern PRERELEASE_MARKER = Pattern.compile(
        "(?i)(snapshot|alpha|beta|rc|cr|m|milestone|preview|ea)"
    );
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("\\d+");

    private VersionClassifier() {}

    public static boolean isStable(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        return !PRERELEASE_MARKER.matcher(version).find();
    }

    public static Optional<String> latestStable(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }

        return versions.stream()
            .filter(VersionClassifier::isStable)
            .max(Comparator.comparing(ComparableVersion::new));
    }

    public static VersionInsight classify(String currentVersion, String latestVersion) {
        String current = normalize(currentVersion);
        String latest = normalize(latestVersion);
        requireVersion(current, "Current");
        requireVersion(latest, "Latest");

        ComparableVersion currentComparable = new ComparableVersion(current);
        ComparableVersion latestComparable = new ComparableVersion(latest);
        if (latestComparable.compareTo(currentComparable) <= 0) {
            return new VersionInsight(current, latest, NONE, CURRENT, true, null);
        }

        return new VersionInsight(current, latest, classifyUpdateType(current, latest), OUTDATED, true, null);
    }

    private static String classifyUpdateType(String currentVersion, String latestVersion) {
        Optional<int[]> currentParts = semanticParts(currentVersion);
        Optional<int[]> latestParts = semanticParts(latestVersion);
        if (currentParts.isEmpty() || latestParts.isEmpty()) {
            return UNKNOWN;
        }

        int[] current = currentParts.get();
        int[] latest = latestParts.get();
        if (latest[0] != current[0]) {
            return "major";
        }
        if (latest[1] != current[1]) {
            return "minor";
        }
        if (latest[2] != current[2]) {
            return "patch";
        }
        return UNKNOWN;
    }

    private static Optional<int[]> semanticParts(String version) {
        String[] segments = version.split("\\.");
        if (segments.length == 0 || segments.length > 4) {
            return Optional.empty();
        }

        int[] parts = new int[] { 0, 0, 0 };
        int limit = Math.min(segments.length, parts.length);
        for (int index = 0; index < limit; index++) {
            if (!NUMERIC_SEGMENT.matcher(segments[index]).matches()) {
                return Optional.empty();
            }
            try {
                parts[index] = Integer.parseInt(segments[index]);
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        for (int index = parts.length; index < segments.length; index++) {
            if (!NUMERIC_SEGMENT.matcher(segments[index]).matches()) {
                return Optional.empty();
            }
        }
        return Optional.of(parts);
    }

    private static String normalize(String version) {
        return version == null ? "" : version.trim();
    }

    private static void requireVersion(String version, String label) {
        if (version.isBlank()) {
            throw new IllegalArgumentException(label + " version must not be blank");
        }
    }
}
