package io.github.lucasgois1.depviz.security;

import io.github.lucasgois1.depviz.graph.DiagnosticEntry;
import java.util.List;

public record SecurityCheckResult(
    boolean enabled,
    boolean checked,
    List<SecurityFinding> findings,
    List<DiagnosticEntry> diagnostics
) {
    public SecurityCheckResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static SecurityCheckResult disabled() {
        return new SecurityCheckResult(false, false, List.of(), List.of());
    }

    public static SecurityCheckResult unavailable(String type, String message) {
        return new SecurityCheckResult(true, false, List.of(), List.of(new DiagnosticEntry("warning", type, message, null)));
    }

    public static SecurityCheckResult checked(List<SecurityFinding> findings, List<DiagnosticEntry> diagnostics) {
        return new SecurityCheckResult(true, true, findings, diagnostics);
    }
}
