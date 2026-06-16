package io.github.lucasgois1.depviz.security;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum SecuritySeverity {
    LOW(1, "low"),
    MEDIUM(2, "medium"),
    HIGH(3, "high"),
    CRITICAL(4, "critical");

    private final int rank;
    private final String value;

    SecuritySeverity(int rank, String value) {
        this.rank = rank;
        this.value = value;
    }

    public int rank() {
        return rank;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static SecuritySeverity parse(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (SecuritySeverity severity : values()) {
            if (severity.value.equals(normalized)) {
                return severity;
            }
        }
        return LOW;
    }
}
