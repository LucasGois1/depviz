package io.github.lucasgois1.depviz.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DepvizGradlePluginTest {
    @TempDir
    Path projectDir;

    @Test
    void configuresExtensionDefaults() {
        var project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(DepvizGradlePlugin.class);

        var extension = project.getExtensions().getByType(DepvizExtension.class);

        assertThat(extension.getScope().get()).isEqualTo("runtime");
        assertThat(extension.getOpen().get()).isTrue();
        assertThat(extension.getSnyk().get()).isEqualTo("auto");
        assertThat(extension.getLayout().get()).isEqualTo("breadthfirst");
        assertThat(extension.getSnykJson().isPresent()).isFalse();
    }

    @Test
    void registersDepvizOpenTask() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"sample\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), "plugins { id(\"io.github.lucasgois1.depviz\") }\n");

        var result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .build();

        assertThat(result.getOutput()).contains("depvizOpen");
        assertThat(result.task(":tasks").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
