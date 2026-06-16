package dev.gois.tools.depviz.security;

import java.util.Comparator;
import java.util.List;

public record SecurityInsight(
    String status,
    SecuritySeverity maxSeverity,
    int vulnerabilityCount,
    int critical,
    int high,
    int medium,
    int low,
    String source,
    List<SecurityFinding> findings
) {
    public SecurityInsight {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static SecurityInsight vulnerable(List<SecurityFinding> findings) {
        List<SecurityFinding> safeFindings = findings == null ? List.of() : List.copyOf(findings);
        SecuritySeverity max = safeFindings.stream()
            .map(SecurityFinding::severity)
            .max(Comparator.comparingInt(SecuritySeverity::rank))
            .orElse(SecuritySeverity.LOW);
        return new SecurityInsight(
            "vulnerable",
            max,
            safeFindings.size(),
            count(safeFindings, SecuritySeverity.CRITICAL),
            count(safeFindings, SecuritySeverity.HIGH),
            count(safeFindings, SecuritySeverity.MEDIUM),
            count(safeFindings, SecuritySeverity.LOW),
            "snyk",
            safeFindings
        );
    }

    private static int count(List<SecurityFinding> findings, SecuritySeverity severity) {
        return (int) findings.stream().filter(finding -> finding.severity() == severity).count();
    }
}
