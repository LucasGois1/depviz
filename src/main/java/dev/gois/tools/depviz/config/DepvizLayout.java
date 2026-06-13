package dev.gois.tools.depviz.config;

import java.util.Locale;

public enum DepvizLayout {
    BREADTHFIRST("breadthfirst"),
    FORCE("force"),
    CIRCLE("circle"),
    CONCENTRIC("concentric");

    private final String value;

    DepvizLayout(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DepvizLayout parse(String raw) {
        String normalized = raw == null || raw.isBlank() ? "breadthfirst" : raw.trim().toLowerCase(Locale.ROOT);
        for (DepvizLayout layout : values()) {
            if (layout.value.equals(normalized)) {
                return layout;
            }
        }
        throw new IllegalArgumentException("Invalid depviz.layout '" + raw + "'. Accepted values: breadthfirst, force, circle, concentric.");
    }
}
