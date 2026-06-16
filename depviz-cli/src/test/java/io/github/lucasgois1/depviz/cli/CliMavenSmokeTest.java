package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliMavenSmokeTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path projectDir;

    @Test
    void runsMavenProjectWithoutMutatingPom() throws Exception {
        Path pomXml = projectDir.resolve("pom.xml");
        String originalPom =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>

              <groupId>com.example</groupId>
              <artifactId>sample-maven-project</artifactId>
              <version>1.0.0</version>

              <dependencies>
                <dependency>
                  <groupId>org.slf4j</groupId>
                  <artifactId>slf4j-api</artifactId>
                  <version>2.0.13</version>
                </dependency>
              </dependencies>
            </project>
            """;
        Files.writeString(pomXml, originalPom, StandardCharsets.UTF_8);

        Path isolatedRepo = projectDir.resolve("isolated-m2");
        seedIsolatedRepository(isolatedRepo);
        Files.createDirectories(projectDir.resolve(".mvn"));
        Files.writeString(
            projectDir.resolve(".mvn/maven.config"),
            "-Dmaven.repo.local=" + isolatedRepo.toAbsolutePath().normalize() + System.lineSeparator(),
            StandardCharsets.UTF_8
        );

        int exitCode = Main.run(
            new String[] {"open", "--tool", "maven", "--no-browser", "--no-snyk", "--scope", "runtime"},
            projectDir,
            false
        );

        Path graphFile = projectDir.resolve("target/depviz/dependency-graph.json");
        assertThat(exitCode).isZero();
        assertThat(graphFile).exists();
        assertGraphContainsSampleProjectAndDependency(graphFile);
        assertThat(Files.readString(pomXml, StandardCharsets.UTF_8)).isEqualTo(originalPom);
    }

    private static void seedIsolatedRepository(Path isolatedRepo) throws Exception {
        Path repoRoot = repoRoot();
        Path logFile = Files.createTempFile("depviz-seed-maven", ".log");
        List<String> command = List.of(
            ExecutableSelector.maven(repoRoot),
            "-Dmaven.repo.local=" + isolatedRepo.toAbsolutePath().normalize(),
            "-pl",
            "depviz-maven-plugin",
            "-am",
            "install",
            "-DskipTests",
            "-Dinvoker.skip=true"
        );
        Process process = new ProcessBuilder(command)
            .directory(repoRoot.toFile())
            .redirectErrorStream(true)
            .redirectOutput(logFile.toFile())
            .start();

        int exitCode = process.waitFor();

        assertThat(exitCode)
            .describedAs("Nested Maven install failed:%n%s", Files.readString(logFile, StandardCharsets.UTF_8))
            .isZero();
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                && Files.isDirectory(candidate.resolve("depviz-cli"))
                && Files.isDirectory(candidate.resolve("depviz-maven-plugin"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to locate depviz repository root from " + current);
    }

    private static void assertGraphContainsSampleProjectAndDependency(Path graphFile) throws Exception {
        JsonNode document = OBJECT_MAPPER.readTree(graphFile.toFile());
        JsonNode rootNode = node(document, "com.example", "sample-maven-project", "1.0.0");
        JsonNode slf4jNode = node(document, "org.slf4j", "slf4j-api", "2.0.13");

        assertThat(document.path("summary").path("nodeCount").asInt()).isPositive();
        assertThat(document.path("summary").path("edgeCount").asInt()).isPositive();
        assertThat(rootNode).isNotNull();
        assertThat(rootNode.path("root").asBoolean()).isTrue();
        assertThat(slf4jNode).isNotNull();
        assertThat(hasEdgeTargeting(document, slf4jNode.path("id").asText())).isTrue();
        assertThat(hasEdge(document, rootNode.path("id").asText(), slf4jNode.path("id").asText())).isTrue();
    }

    private static JsonNode node(JsonNode document, String groupId, String artifactId, String version) {
        for (JsonNode node : document.path("nodes")) {
            if (groupId.equals(node.path("groupId").asText())
                && artifactId.equals(node.path("artifactId").asText())
                && version.equals(node.path("version").asText())) {
                return node;
            }
        }
        return null;
    }

    private static boolean hasEdgeTargeting(JsonNode document, String targetNodeId) {
        for (JsonNode edge : document.path("edges")) {
            if (targetNodeId.equals(edge.path("target").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEdge(JsonNode document, String sourceNodeId, String targetNodeId) {
        for (JsonNode edge : document.path("edges")) {
            if (sourceNodeId.equals(edge.path("source").asText())
                && targetNodeId.equals(edge.path("target").asText())) {
                return true;
            }
        }
        return false;
    }
}
