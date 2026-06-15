package dev.gois.tools.depviz.config;

import java.nio.file.Path;

public record DepvizConfig(
    DepvizScope scope,
    boolean open,
    String includes,
    String excludes,
    DepvizLayout initialLayout,
    NodeMode nodeMode,
    int maxInitialLabels,
    boolean checkUpdates,
    Path outputDirectory,
    SnykMode snykMode,
    Path snykJson,
    String snykCommand,
    String snykOrg,
    boolean snykAllProjects
) {
    public static DepvizConfig fromRaw(
        String scope,
        String open,
        String includes,
        String layout,
        String nodeMode,
        String maxInitialLabels,
        String excludes,
        Path outputDirectory
    ) {
        return fromRaw(scope, open, includes, layout, nodeMode, maxInitialLabels, excludes, null, outputDirectory);
    }

    public static DepvizConfig fromRaw(
        String scope,
        String open,
        String includes,
        String layout,
        String nodeMode,
        String maxInitialLabels,
        String excludes,
        String checkUpdates,
        Path outputDirectory
    ) {
        return fromRaw(
            scope,
            open,
            includes,
            layout,
            nodeMode,
            maxInitialLabels,
            excludes,
            checkUpdates,
            outputDirectory,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static DepvizConfig fromRaw(
        String scope,
        String open,
        String includes,
        String layout,
        String nodeMode,
        String maxInitialLabels,
        String excludes,
        String checkUpdates,
        Path outputDirectory,
        String snyk,
        String snykJson,
        String snykCommand,
        String snykOrg,
        String snykAllProjects
    ) {
        DepvizScope parsedScope = DepvizScope.parse(scope);
        DepvizLayout parsedLayout = DepvizLayout.parse(layout);
        NodeMode parsedNodeMode = NodeMode.parse(nodeMode);
        boolean parsedOpen = parseOpen(open);
        int parsedMaxInitialLabels = parseMaxInitialLabels(maxInitialLabels);
        boolean parsedCheckUpdates = parseCheckUpdates(checkUpdates);
        Path resolvedOutputDirectory = outputDirectory == null ? Path.of("target", "depviz") : outputDirectory;
        return new DepvizConfig(
            parsedScope,
            parsedOpen,
            blankToNull(includes),
            blankToNull(excludes),
            parsedLayout,
            parsedNodeMode,
            parsedMaxInitialLabels,
            parsedCheckUpdates,
            resolvedOutputDirectory,
            SnykMode.parse(snyk),
            blankPath(snykJson),
            defaultSnykCommand(snykCommand),
            blankToNull(snykOrg),
            parseSnykAllProjects(snykAllProjects)
        );
    }

    private static boolean parseSnykAllProjects(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        throw new IllegalArgumentException("depviz.snykAllProjects must be true or false.");
    }

    private static Path blankPath(String raw) {
        return raw == null || raw.isBlank() ? null : Path.of(raw.trim());
    }

    private static String defaultSnykCommand(String raw) {
        String value = blankToNull(raw);
        return value == null ? "snyk" : value;
    }

    private static boolean parseCheckUpdates(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        throw new IllegalArgumentException("Invalid depviz.checkUpdates value: " + raw);
    }

    private static int parseMaxInitialLabels(String raw) {
        if (raw == null || raw.isBlank()) {
            return 500;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("depviz.maxInitialLabels must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("depviz.maxInitialLabels must be an integer.", exception);
        }
    }

    private static boolean parseOpen(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("depviz.open must be true or false.");
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }
}
