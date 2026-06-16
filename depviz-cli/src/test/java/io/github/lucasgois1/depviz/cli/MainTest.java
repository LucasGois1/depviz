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
        CliOptions options = new CliOptions(dir, null, "runtime", true, null, null, null, null, null, false, null);
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
}
