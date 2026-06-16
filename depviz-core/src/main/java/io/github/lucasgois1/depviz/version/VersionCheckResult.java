package io.github.lucasgois1.depviz.version;

import io.github.lucasgois1.depviz.graph.DiagnosticEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record VersionCheckResult(
    Map<String, VersionInsight> insightsByNodeId,
    VersionSummary summary,
    List<DiagnosticEntry> diagnostics
) {
    public VersionCheckResult {
        insightsByNodeId = Map.copyOf(insightsByNodeId);
        summary = Objects.requireNonNull(summary, "summary is required.");
        diagnostics = List.copyOf(diagnostics);
    }

    public static VersionCheckResult empty(boolean enabled) {
        return new VersionCheckResult(
            Map.of(),
            enabled ? new VersionSummary(true, 0, 0, 0, 0, 0, 0, 0, 0) : VersionSummary.disabled(),
            List.of()
        );
    }

    public static VersionCheckResult disabled() {
        return empty(false);
    }

    public static VersionCheckResult failed(boolean enabled, String message) {
        return new VersionCheckResult(
            Map.of(),
            enabled ? new VersionSummary(true, 0, 0, 0, 0, 0, 0, 0, 0) : VersionSummary.disabled(),
            List.of(new DiagnosticEntry("warning", "version-update-check-failed", message, null))
        );
    }
}
