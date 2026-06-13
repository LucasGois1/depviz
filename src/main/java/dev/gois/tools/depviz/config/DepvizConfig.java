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
    Path outputDirectory
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
        DepvizScope parsedScope = DepvizScope.parse(scope);
        DepvizLayout parsedLayout = DepvizLayout.parse(layout);
        NodeMode parsedNodeMode = NodeMode.parse(nodeMode);
        boolean parsedOpen = parseOpen(open);
        int parsedMaxInitialLabels = parseMaxInitialLabels(maxInitialLabels);
        Path resolvedOutputDirectory = outputDirectory == null ? Path.of("target", "depviz") : outputDirectory;
        return new DepvizConfig(
            parsedScope,
            parsedOpen,
            blankToNull(includes),
            blankToNull(excludes),
            parsedLayout,
            parsedNodeMode,
            parsedMaxInitialLabels,
            resolvedOutputDirectory
        );
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
