package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir
    Path dir;

    @Test
    void rejectsDirectoryWithoutSupportedBuildFiles() {
        assertThatThrownBy(() -> Main.run(new String[] {"open"}, dir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No Maven or Gradle project found")
            .hasMessageContaining("build.gradle.kts")
            .hasMessageContaining("settings.gradle.kts");
    }

    @Test
    void rejectsAmbiguousDirectoryWithoutToolOption() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project />\n");
        Files.writeString(dir.resolve("settings.gradle"), "pluginManagement {}\n");

        assertThatThrownBy(() -> Main.run(new String[] {"open"}, dir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
    }

    @Test
    void delegatesInteractiveAmbiguousSelectionToPrompter() {
        CliOptions options = new CliOptions(dir, null, "runtime", null, false, true, null, null, null, null, null, false, null);
        ProjectDetector.DetectionResult detection =
            new ProjectDetector.DetectionResult(List.of(BuildTool.MAVEN, BuildTool.GRADLE));
        ConsolePrompter prompter = new ConsolePrompter(new ByteArrayInputStream("2\n".getBytes()), new PrintStream(new ByteArrayOutputStream()));

        BuildTool tool = Main.selectTool(options, detection, true, prompter);

        assertThat(tool).isEqualTo(BuildTool.GRADLE);
    }

    @Test
    void rejectsRequestedToolThatWasNotDetectedBeforeRunningProcess() throws Exception {
        Files.writeString(dir.resolve("settings.gradle"), "pluginManagement {}\n");

        assertThatThrownBy(() -> Main.run(new String[] {"open", "--tool", "maven"}, dir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Requested build tool was not detected in " + dir);
    }

    @Test
    void rendersValidationErrorsWithoutStackTrace() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = Main.runMain(new String[] {"open"}, dir, false, new PrintStream(stderr, true, StandardCharsets.UTF_8));

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString(StandardCharsets.UTF_8))
            .contains("No Maven or Gradle project found")
            .doesNotContain("Exception")
            .doesNotContain("\tat ");
    }

    @Test
    void printsRootHelpWithoutInspectingProject() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = Main.runMain(
            new String[] {"--help"},
            dir,
            false,
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8)
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(stdout.toString(StandardCharsets.UTF_8))
            .contains("Usage:")
            .contains("depviz open [options]")
            .contains("Commands:")
            .contains("version");
        assertThat(stderr.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void printsOpenHelpWithoutInspectingProject() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = Main.runMain(
            new String[] {"open", "-h"},
            dir,
            false,
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8)
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(stdout.toString(StandardCharsets.UTF_8))
            .contains("Usage: depviz open [options]")
            .contains("--no-updates")
            .contains("--refresh-dependencies")
            .contains("Corporate Maven mirrors");
        assertThat(stderr.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void printsVersionWithoutInspectingProject() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        String previous = System.setProperty("depviz.version", "9.8.7");
        try {
            int exitCode = Main.runMain(
                new String[] {"--version"},
                dir,
                false,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8)
            );

            assertThat(exitCode).isEqualTo(0);
            assertThat(stdout.toString(StandardCharsets.UTF_8)).isEqualTo("depviz 9.8.7\n");
            assertThat(stderr.toString(StandardCharsets.UTF_8)).isEmpty();
        } finally {
            if (previous == null) {
                System.clearProperty("depviz.version");
            } else {
                System.setProperty("depviz.version", previous);
            }
        }
    }

    @Test
    void printsVersionWithShortOption() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        String previous = System.setProperty("depviz.version", "9.8.7");
        try {
            int exitCode = Main.runMain(
                new String[] {"-v"},
                dir,
                false,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8)
            );

            assertThat(exitCode).isEqualTo(0);
            assertThat(stdout.toString(StandardCharsets.UTF_8)).isEqualTo("depviz 9.8.7\n");
            assertThat(stderr.toString(StandardCharsets.UTF_8)).isEmpty();
        } finally {
            if (previous == null) {
                System.clearProperty("depviz.version");
            } else {
                System.setProperty("depviz.version", previous);
            }
        }
    }

    @Test
    void fallsBackToSnapshotVersionWhenManifestVersionIsUnavailable() {
        String previous = System.clearProperty("depviz.version");
        try {
            assertThat(Main.resolveVersion()).isEqualTo("0.1.0-SNAPSHOT");
        } finally {
            if (previous != null) {
                System.setProperty("depviz.version", previous);
            }
        }
    }

    @Test
    void usesConfiguredVersionWhenRunningFromTestsOrBuildTool() {
        String previous = System.setProperty("depviz.version", "9.8.7");
        try {
            assertThat(Main.resolveVersion()).isEqualTo("9.8.7");
        } finally {
            if (previous == null) {
                System.clearProperty("depviz.version");
            } else {
                System.setProperty("depviz.version", previous);
            }
        }
    }
}
