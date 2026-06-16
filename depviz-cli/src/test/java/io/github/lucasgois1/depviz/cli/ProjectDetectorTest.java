package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectDetectorTest {
    @TempDir
    Path dir;

    @Test
    void detectsMavenOnlyInCurrentDirectory() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.MAVEN);
    }

    @Test
    void detectsGradleFromSettingsOrBuildFile() throws Exception {
        Files.writeString(dir.resolve("settings.gradle.kts"), "rootProject.name = \"demo\"");
        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.GRADLE);
    }

    @Test
    void reportsBothToolsWhenBothArePresent() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        Files.writeString(dir.resolve("build.gradle.kts"), "plugins { java }");
        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.MAVEN, BuildTool.GRADLE);
    }

    @Test
    void doesNotWalkParentDirectories() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        Path child = Files.createDirectory(dir.resolve("child"));
        assertThat(ProjectDetector.detect(child).tools()).isEmpty();
    }
}
