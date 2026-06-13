package dev.gois.tools.depviz.util;

import java.util.Objects;

public final class JsonEscaper {
    private JsonEscaper() {
    }

    public static String forInlineScript(String json) {
        return Objects.requireNonNull(json, "json is required.").replace("<", "\\u003C");
    }
}
