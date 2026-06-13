package dev.gois.tools.depviz.config;

import java.util.Locale;

public enum DepvizScope {
    COMPILE("compile"),
    RUNTIME("runtime"),
    TEST("test"),
    PROVIDED("provided"),
    SYSTEM("system"),
    IMPORT("import"),
    ALL("all");

    private final String value;

    DepvizScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DepvizScope parse(String raw) {
        String normalized = raw == null || raw.isBlank() ? "runtime" : raw.trim().toLowerCase(Locale.ROOT);
        for (DepvizScope scope : values()) {
            if (scope.value.equals(normalized)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Invalid depviz.scope '" + raw + "'. Accepted values: compile, runtime, test, provided, system, import, all.");
    }

    public boolean includesDependencyScope(String dependencyScope) {
        if (this == ALL) {
            return true;
        }
        if (dependencyScope == null || dependencyScope.isBlank()) {
            return this == COMPILE || this == RUNTIME;
        }
        String normalized = dependencyScope.toLowerCase(Locale.ROOT);
        return switch (this) {
            case COMPILE -> normalized.equals("compile") || normalized.isBlank();
            case RUNTIME -> normalized.equals("compile") || normalized.equals("runtime") || normalized.isBlank();
            case TEST -> true;
            case PROVIDED -> normalized.equals("provided");
            case SYSTEM -> normalized.equals("system");
            case IMPORT -> normalized.equals("import");
            case ALL -> true;
        };
    }
}
