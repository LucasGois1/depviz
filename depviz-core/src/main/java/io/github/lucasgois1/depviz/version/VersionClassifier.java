package io.github.lucasgois1.depviz.version;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class VersionClassifier {
    private static final String CURRENT = "current";
    private static final String OUTDATED = "outdated";
    private static final String NONE = "none";
    private static final String UNKNOWN = "unknown";
    private static final Pattern PRERELEASE_MARKER = Pattern.compile(
        "(?i)(?:^|[._-]|(?<=\\d))(?:snapshot|alpha|beta|milestone|preview|rc|cr|m|ea)(?=$|[._-]|\\d)"
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
            .max(VersionClassifier::compareVersions);
    }

    public static VersionInsight classify(String currentVersion, String latestVersion) {
        String current = normalize(currentVersion);
        String latest = normalize(latestVersion);
        requireVersion(current, "Current");
        requireVersion(latest, "Latest");

        if (compareVersions(latest, current) <= 0) {
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

    private static int compareVersions(String left, String right) {
        List<Token> leftTokens = tokens(left);
        List<Token> rightTokens = tokens(right);
        int size = Math.max(leftTokens.size(), rightTokens.size());
        for (int index = 0; index < size; index++) {
            Token leftToken = index < leftTokens.size() ? leftTokens.get(index) : Token.ZERO;
            Token rightToken = index < rightTokens.size() ? rightTokens.get(index) : Token.ZERO;
            int comparison = leftToken.compareTo(rightToken);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static List<Token> tokens(String version) {
        List<Token> tokens = Pattern.compile("\\d+|[A-Za-z]+")
            .matcher(version)
            .results()
            .map(match -> Token.of(match.group()))
            .toList();
        int end = tokens.size();
        while (end > 0 && tokens.get(end - 1).isNeutral()) {
            end--;
        }
        return tokens.subList(0, end);
    }

    private static String normalize(String version) {
        return version == null ? "" : version.trim();
    }

    private static void requireVersion(String version, String label) {
        if (version.isBlank()) {
            throw new IllegalArgumentException(label + " version must not be blank");
        }
    }

    private record Token(BigInteger number, String text) implements Comparable<Token> {
        private static final Token ZERO = new Token(BigInteger.ZERO, null);

        private static Token of(String value) {
            if (NUMERIC_SEGMENT.matcher(value).matches()) {
                return new Token(new BigInteger(value), null);
            }
            return new Token(null, value.toLowerCase());
        }

        private boolean isNeutral() {
            return BigInteger.ZERO.equals(number)
                || "final".equals(text)
                || "release".equals(text)
                || "ga".equals(text);
        }

        @Override
        public int compareTo(Token other) {
            if (number != null && other.number != null) {
                return number.compareTo(other.number);
            }
            if (number != null) {
                return 1;
            }
            if (other.number != null) {
                return -1;
            }
            return text.compareTo(other.text);
        }
    }
}
