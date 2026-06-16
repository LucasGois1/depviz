package io.github.lucasgois1.depviz.config;

import java.util.Locale;

public enum SnykMode {
    AUTO("auto"),
    TRUE("true"),
    FALSE("false");

    private final String value;

    SnykMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SnykMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (SnykMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid depviz.snyk value: " + raw);
    }
}
