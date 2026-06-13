package dev.gois.tools.depviz.config;

import java.util.Locale;

public enum NodeMode {
    ARTIFACT("artifact");

    private final String value;

    NodeMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static NodeMode parse(String raw) {
        String normalized = raw == null || raw.isBlank() ? "artifact" : raw.trim().toLowerCase(Locale.ROOT);
        if ("occurrence".equals(normalized)) {
            throw new IllegalArgumentException("depviz.nodeMode=occurrence is not implemented yet.");
        }
        if ("artifact".equals(normalized)) {
            return ARTIFACT;
        }
        throw new IllegalArgumentException("Invalid depviz.nodeMode '" + raw + "'. Accepted values: artifact.");
    }
}
