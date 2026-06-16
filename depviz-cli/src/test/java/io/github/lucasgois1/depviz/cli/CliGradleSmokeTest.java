package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliGradleSmokeTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path projectDir;

    @AfterEach
    void stopGradleBeforeTempDirCleanup() throws Exception {
        Path gradleUserHome = projectDir.resolve("gradle-user-home");
        if (!Files.isDirectory(gradleUserHome)) {
            return;
        }
        runNestedBuild(
            "Nested Gradle stop failed",
            List.of(
                repoRoot().resolve("gradlew").toAbsolutePath().normalize().toString(),
                "--gradle-user-home",
                gradleUserHome.toAbsolutePath().normalize().toString(),
                "--stop"
            ),
            repoRoot(),
            supportedGradleJavaHome()
        );
        deleteRecursivelyWithRetry(gradleUserHome);
    }

    @Test
    void cliRunsGradlePluginWithoutEditingBuildFiles() throws Exception {
        Path settingsGradle = projectDir.resolve("settings.gradle.kts");
        Path buildGradle = projectDir.resolve("build.gradle.kts");
        String originalSettings = "rootProject.name = \"cli-gradle-sample\"\n";
        String originalBuild =
            """
            plugins { java }
            repositories { mavenCentral() }
            dependencies { runtimeOnly("org.slf4j:slf4j-api:2.0.13") }
            """;
        Files.writeString(settingsGradle, originalSettings, StandardCharsets.UTF_8);
        Files.writeString(buildGradle, originalBuild, StandardCharsets.UTF_8);

        Path isolatedRepo = projectDir.resolve("isolated-m2");
        Path gradleUserHome = projectDir.resolve("gradle-user-home");
        Path gradleJavaHome = supportedGradleJavaHome();
        seedIsolatedRepository(isolatedRepo, gradleUserHome, gradleJavaHome);
        writeGradleWrapperShim(isolatedRepo, gradleUserHome, gradleJavaHome);

        int exitCode = Main.run(
            new String[] {"open", "--tool", "gradle", "--no-browser", "--no-snyk", "--scope", "runtime"},
            projectDir,
            false
        );

        Path graphFile = projectDir.resolve("build/depviz/dependency-graph.json");
        assertThat(exitCode).isZero();
        assertThat(graphFile).exists();
        assertGraphContainsSampleProjectAndDependency(graphFile);
        assertThat(Files.readString(buildGradle, StandardCharsets.UTF_8)).isEqualTo(originalBuild);
        assertThat(Files.readString(settingsGradle, StandardCharsets.UTF_8)).isEqualTo(originalSettings);
    }

    private void writeGradleWrapperShim(Path isolatedRepo, Path gradleUserHome, Path gradleJavaHome) throws Exception {
        Path repoGradlew = repoRoot().resolve("gradlew").toAbsolutePath().normalize();
        Path shim = projectDir.resolve("gradlew");
        String javaHomeExports = "";
        if (gradleJavaHome != null) {
            javaHomeExports = "export JAVA_HOME="
                + shellQuote(gradleJavaHome.toString())
                + "\nexport PATH=\"$JAVA_HOME/bin:$PATH\"\n";
        }
        String script = "#!/bin/sh\n"
            + javaHomeExports
            + "export GRADLE_USER_HOME="
            + shellQuote(gradleUserHome.toAbsolutePath().normalize().toString())
            + "\nexec "
            + shellQuote(repoGradlew.toString())
            + " --no-daemon"
            + " --gradle-user-home "
            + shellQuote(gradleUserHome.toAbsolutePath().normalize().toString())
            + " "
            + shellQuote("-Dmaven.repo.local=" + isolatedRepo.toAbsolutePath().normalize())
            + " \"$@\"\n";
        Files.writeString(shim, script, StandardCharsets.UTF_8);
        assertThat(shim.toFile().setExecutable(true)).isTrue();
    }

    private static void seedIsolatedRepository(Path isolatedRepo, Path gradleUserHome, Path gradleJavaHome) throws Exception {
        Path repoRoot = repoRoot();
        runNestedBuild(
            "Nested Maven install failed",
            List.of(
                ExecutableSelector.maven(repoRoot),
                "-Dmaven.repo.local=" + isolatedRepo.toAbsolutePath().normalize(),
                "-pl",
                "depviz-core",
                "-am",
                "install",
                "-DskipTests"
            ),
            repoRoot,
            null
        );
        List<String> gradlePublishCommand = new ArrayList<>();
        gradlePublishCommand.add(repoRoot.resolve("gradlew").toAbsolutePath().normalize().toString());
        gradlePublishCommand.addAll(
            List.of(
                "--gradle-user-home",
                gradleUserHome.toAbsolutePath().normalize().toString(),
                "-Dmaven.repo.local=" + isolatedRepo.toAbsolutePath().normalize(),
                "--no-daemon",
                "--stacktrace",
                ":depviz-gradle-plugin:publishToMavenLocal"
            )
        );
        runNestedBuild(
            "Nested Gradle publish failed",
            gradlePublishCommand,
            repoRoot,
            gradleJavaHome
        );
    }

    private static void runNestedBuild(String failureMessage, List<String> command, Path directory, Path javaHome) throws Exception {
        Path logFile = Files.createTempFile("depviz-gradle-smoke", ".log");
        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(logFile.toFile());
        if (javaHome != null) {
            builder.environment().put("JAVA_HOME", javaHome.toString());
            builder.environment()
                .put(
                    "PATH",
                    javaHome.resolve("bin") + File.pathSeparator + builder.environment().getOrDefault("PATH", "")
                );
        }
        Process process = builder.start();

        int exitCode = process.waitFor();

        assertThat(exitCode)
            .describedAs("%s:%n%s", failureMessage, Files.readString(logFile, StandardCharsets.UTF_8))
            .isZero();
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                && Files.isRegularFile(candidate.resolve("gradlew"))
                && Files.isDirectory(candidate.resolve("depviz-cli"))
                && Files.isDirectory(candidate.resolve("depviz-gradle-plugin"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to locate depviz repository root from " + current);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void deleteRecursivelyWithRetry(Path directory) throws Exception {
        IOException lastException = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                deleteRecursively(directory);
                return;
            } catch (IOException exception) {
                lastException = exception;
                Thread.sleep(200);
            }
        }
        throw lastException;
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static Path supportedGradleJavaHome() {
        int currentMajor = parseJavaMajor(System.getProperty("java.specification.version"));
        if (isSupportedGradleRuntime(currentMajor)) {
            return null;
        }
        for (Path candidate : supportedJavaHomeCandidates()) {
            if (Files.isDirectory(candidate) && isSupportedGradleRuntime(javaMajor(candidate))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static List<Path> supportedJavaHomeCandidates() {
        List<Path> candidates = new ArrayList<>();
        addJavaHomeCandidate(candidates, System.getenv("GRADLE_SMOKE_JAVA_HOME"));
        addJavaHomeCandidate(candidates, System.getenv("JAVA_HOME"));
        addJavaHomeCandidate(candidates, System.getProperty("user.home") + "/.sdkman/candidates/java/current");
        addJavaHomesFromDirectory(candidates, Path.of(System.getProperty("user.home"), ".sdkman/candidates/java"));
        addJavaHomesFromDirectory(candidates, Path.of(System.getProperty("user.home"), ".jenv/versions"));
        addJavaHomesFromDirectory(candidates, Path.of(System.getProperty("user.home"), ".asdf/installs/java"));
        candidates.add(Path.of("/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"));
        candidates.add(Path.of("/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"));
        candidates.add(Path.of("/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"));
        candidates.add(Path.of("/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"));
        candidates.add(Path.of("/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"));
        candidates.add(Path.of("/usr/local/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"));
        return candidates;
    }

    private static boolean isSupportedGradleRuntime(int major) {
        return major >= 17 && major <= 24;
    }

    private static void addJavaHomeCandidate(List<Path> candidates, String javaHome) {
        if (javaHome != null && !javaHome.isBlank()) {
            candidates.add(Path.of(javaHome));
        }
    }

    private static void addJavaHomesFromDirectory(List<Path> candidates, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isDirectory).sorted().forEach(candidates::add);
        } catch (Exception exception) {
            return;
        }
    }

    private static int javaMajor(Path javaHome) {
        Path release = javaHome.resolve("release");
        if (!Files.isRegularFile(release)) {
            return -1;
        }
        try {
            for (String line : Files.readAllLines(release, StandardCharsets.UTF_8)) {
                if (line.startsWith("JAVA_VERSION=")) {
                    return parseJavaMajor(line.substring("JAVA_VERSION=".length()).replace("\"", ""));
                }
            }
        } catch (Exception exception) {
            return -1;
        }
        return -1;
    }

    private static int parseJavaMajor(String version) {
        if (version == null || version.isBlank()) {
            return -1;
        }
        String normalized = version.trim();
        if (normalized.startsWith("1.")) {
            normalized = normalized.substring(2);
        }
        int end = 0;
        while (end < normalized.length() && Character.isDigit(normalized.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        return Integer.parseInt(normalized.substring(0, end));
    }

    private static void assertGraphContainsSampleProjectAndDependency(Path graphFile) throws Exception {
        JsonNode document = OBJECT_MAPPER.readTree(graphFile.toFile());
        JsonNode rootNode = node(document, "unknown", "cli-gradle-sample", "unspecified");
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
