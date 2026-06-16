package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir
    Path dir;

    @Test
    void rejectsDirectoryWithoutSupportedBuildFiles() {
        assertThatThrownBy(() -> Main.run(new String[] {"open"}, dir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No Maven or Gradle project found");
    }

    @Test
    void rejectsAmbiguousDirectoryWithoutToolOption() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project />\n");
        Files.writeString(dir.resolve("settings.gradle"), "pluginManagement {}\n");

        assertThatThrownBy(() -> Main.run(new String[] {"open"}, dir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
    }
}
