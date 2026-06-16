package dev.gois.tools.depviz.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnykRunnerTest {
    @TempDir
    private Path tempDir;

    @Test
    void buildsStructuredCommandWithOrgAndAllProjects() {
        DepvizConfig config = DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, Path.of("target/depviz"),
            "true", null, "/opt/bin/snyk", "depviz-org", "true"
        );

        assertThat(SnykRunner.command(config, Path.of("/tmp/snyk.json"))).containsExactly(
            "/opt/bin/snyk",
            "test",
            "--json-file-output=/tmp/snyk.json",
            "--org=depviz-org",
            "--all-projects"
        );
    }

    @Test
    void disabledModeSkipsSnykInputs() {
        DepvizConfig config = DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, tempDir,
            "false", tempDir.resolve("missing-report.json").toString(), tempDir.resolve("missing-snyk").toString(), null, null
        );

        SecurityCheckResult result = new SnykRunner().run(config, tempDir);

        assertThat(result.enabled()).isFalse();
        assertThat(result.checked()).isFalse();
        assertThat(result.findings()).isEmpty();
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void importsConfiguredJsonReportWithoutRunningCli() throws Exception {
        Path report = tempDir.resolve("snyk.json");
        Files.writeString(report, """
            {
              "vulnerabilities": [
                {
                  "id": "SNYK-JAVA-EXAMPLE-1",
                  "severity": "high",
                  "title": "Example vulnerability",
                  "packageName": "org.example:lib",
                  "version": "1.0.0"
                }
              ]
            }
            """);
        DepvizConfig config = DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, tempDir,
            "true", report.toString(), tempDir.resolve("missing-snyk").toString(), null, null
        );

        SecurityCheckResult result = new SnykRunner().run(config, tempDir);

        assertThat(result.checked()).isTrue();
        assertThat(result.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.id()).isEqualTo("SNYK-JAVA-EXAMPLE-1");
            assertThat(finding.packageName()).isEqualTo("org.example:lib");
            assertThat(finding.version()).isEqualTo("1.0.0");
        });
    }

    @Test
    void importsConfiguredRelativeJsonReportFromWorkingDirectory() throws Exception {
        Path report = tempDir.resolve("reports").resolve("snyk.json");
        Files.createDirectories(report.getParent());
        Files.writeString(report, """
            {
              "vulnerabilities": [
                {
                  "id": "SNYK-JAVA-RELATIVE-1",
                  "severity": "medium",
                  "title": "Relative report vulnerability",
                  "packageName": "org.example:relative-lib",
                  "version": "2.0.0"
                }
              ]
            }
            """);
        DepvizConfig config = DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, tempDir,
            "true", "reports/snyk.json", tempDir.resolve("missing-snyk").toString(), null, null
        );

        SecurityCheckResult result = new SnykRunner().run(config, tempDir);

        assertThat(result.checked()).isTrue();
        assertThat(result.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.id()).isEqualTo("SNYK-JAVA-RELATIVE-1");
            assertThat(finding.packageName()).isEqualTo("org.example:relative-lib");
            assertThat(finding.version()).isEqualTo("2.0.0");
        });
    }

    @Test
    void unavailableCommandInAutoModeReturnsDiagnostic() {
        DepvizConfig config = DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, tempDir,
            "auto", null, tempDir.resolve("missing-snyk").toString(), null, null
        );

        SecurityCheckResult result = new SnykRunner().run(config, tempDir);

        assertThat(result.enabled()).isTrue();
        assertThat(result.checked()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo("warning");
            assertThat(diagnostic.type()).isEqualTo("snyk-unavailable");
        });
    }

    @Test
    void tempFileCreationFailureInAutoModeReturnsUnavailableDiagnostic() {
        SecurityCheckResult result = new SnykRunner((prefix, suffix) -> {
            throw new IOException("disk full");
        }).run(configWithSnyk("auto"), tempDir);

        assertThat(result.enabled()).isTrue();
        assertThat(result.checked()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.type()).isEqualTo("snyk-unavailable");
            assertThat(diagnostic.message()).contains("disk full");
        });
    }

    @Test
    void tempFileCreationFailureInTrueModeReturnsScanFailedDiagnostic() {
        SecurityCheckResult result = new SnykRunner((prefix, suffix) -> {
            throw new IOException("disk full");
        }).run(configWithSnyk("true"), tempDir);

        assertThat(result.enabled()).isTrue();
        assertThat(result.checked()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.type()).isEqualTo("snyk-scan-failed");
            assertThat(diagnostic.message()).contains("disk full");
        });
    }

    private DepvizConfig configWithSnyk(String snyk) {
        return DepvizConfig.fromRaw(
            null, "false", null, null, null, null, null, null, tempDir,
            snyk, null, tempDir.resolve("missing-snyk").toString(), null, null
        );
    }
}
