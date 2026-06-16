package dev.gois.tools.depviz.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gois.tools.depviz.graph.DiagnosticEntry;
import java.util.ArrayList;
import java.util.List;

public final class SnykReportParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityCheckResult parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<SecurityFinding> findings = new ArrayList<>();
            collectFindings(root, findings);
            return SecurityCheckResult.checked(findings, List.of());
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            return new SecurityCheckResult(true, false, List.of(), List.of(new DiagnosticEntry("warning", "snyk-json-invalid", message, null)));
        }
    }

    private static void collectFindings(JsonNode node, List<SecurityFinding> findings) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFindings(child, findings));
            return;
        }
        JsonNode vulnerabilities = node.path("vulnerabilities");
        if (vulnerabilities.isArray()) {
            vulnerabilities.forEach(vulnerability -> addFinding(vulnerability, findings));
        }
        JsonNode projects = node.path("projects");
        if (projects.isArray()) {
            projects.forEach(project -> collectFindings(project, findings));
        }
    }

    private static void addFinding(JsonNode vulnerability, List<SecurityFinding> findings) {
        String packageName = text(vulnerability, "packageName", text(vulnerability, "name", ""));
        String version = text(vulnerability, "version", "");
        findings.add(new SecurityFinding(
            text(vulnerability, "id", text(vulnerability, "issueId", "unknown")),
            SecuritySeverity.parse(text(vulnerability, "severity", "low")),
            text(vulnerability, "title", text(vulnerability, "name", "Untitled Snyk finding")),
            packageName,
            version,
            stringArray(vulnerability.path("fixedIn")),
            text(vulnerability, "url", "")
        ));
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        return value.asText(fallback);
    }

    private static List<String> stringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (!value.isNull()) {
                values.add(value.asText());
            }
        });
        return values;
    }
}
