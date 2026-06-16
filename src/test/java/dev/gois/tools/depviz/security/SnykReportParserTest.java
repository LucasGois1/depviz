package dev.gois.tools.depviz.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SnykReportParserTest {
    private final SnykReportParser parser = new SnykReportParser();

    @Test
    void parsesSingleProjectVulnerabilities() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/snyk/single-project.json"));

        SecurityCheckResult result = parser.parse(json);

        assertThat(result.checked()).isTrue();
        assertThat(result.findings()).hasSize(2);
        assertThat(result.findings().get(0)).satisfies(finding -> {
            assertThat(finding.id()).isEqualTo("SNYK-JAVA-ORGEXAMPLE-1");
            assertThat(finding.severity()).isEqualTo(SecuritySeverity.HIGH);
            assertThat(finding.packageName()).isEqualTo("org.example:lib");
            assertThat(finding.version()).isEqualTo("1.0.0");
            assertThat(finding.fixedVersions()).containsExactly("1.0.2");
        });
    }

    @Test
    void parsesMultiProjectArrayVulnerabilities() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/snyk/multi-project.json"));

        SecurityCheckResult result = parser.parse(json);

        assertThat(result.checked()).isTrue();
        assertThat(result.findings()).extracting(SecurityFinding::id)
            .containsExactly("SNYK-JAVA-ORGSHARED-1", "SNYK-JAVA-UNMAPPED-1");
        assertThat(result.findings()).extracting(SecurityFinding::severity)
            .containsExactly(SecuritySeverity.CRITICAL, SecuritySeverity.LOW);
    }

    @Test
    void parsesNestedProjectsVulnerabilitiesInOrder() {
        String json = """
            {
              "projects": [
                {
                  "vulnerabilities": [
                    { "id": "SNYK-JAVA-NESTED-1", "severity": "medium" }
                  ]
                },
                {
                  "vulnerabilities": [
                    { "id": "SNYK-JAVA-NESTED-2", "severity": "high" }
                  ]
                }
              ]
            }
            """;

        SecurityCheckResult result = parser.parse(json);

        assertThat(result.checked()).isTrue();
        assertThat(result.findings()).extracting(SecurityFinding::id)
            .containsExactly("SNYK-JAVA-NESTED-1", "SNYK-JAVA-NESTED-2");
        assertThat(result.findings()).extracting(SecurityFinding::severity)
            .containsExactly(SecuritySeverity.MEDIUM, SecuritySeverity.HIGH);
    }

    @Test
    void invalidJsonReturnsDiagnosticInsteadOfThrowing() {
        SecurityCheckResult result = parser.parse("{not-json");

        assertThat(result.checked()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.type()).isEqualTo("snyk-json-invalid");
            assertThat(diagnostic.severity()).isEqualTo("warning");
        });
    }

    @Test
    void collectFindingsFailuresAreNotReportedAsInvalidJson() {
        SnykReportParser parser = new SnykReportParser(new CollectorFailureObjectMapper());

        assertThatThrownBy(() -> parser.parse("{}"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("collector failure");
    }

    private static final class CollectorFailureObjectMapper extends ObjectMapper {
        @Override
        public JsonNode readTree(String content) {
            return new CollectorFailureNode();
        }
    }

    private static final class CollectorFailureNode extends ObjectNode {
        private CollectorFailureNode() {
            super(JsonNodeFactory.instance);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CollectorFailureNode deepCopy() {
            return this;
        }

        @Override
        public JsonNode path(String propertyName) {
            throw new IllegalStateException("collector failure");
        }
    }
}
