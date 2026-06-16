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

    @Test
    void generatesSingleProjectRuntimeGraph() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories { mavenCentral() }
            }

            rootProject.name = "gradle-sample"
            """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("io.github.lucasgois1.depviz")
            }

            dependencies {
                runtimeOnly("org.slf4j:slf4j-api:2.0.13")
            }

            depviz {
                open.set(false)
                scope.set(" Runtime ")
            }
            """);

        var result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("depvizOpen", "--stacktrace")
            .build();

        Path json = projectDir.resolve("build/depviz/dependency-graph.json");
        assertThat(result.task(":depvizOpen").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(json).exists();
        String graphJson = Files.readString(json);
        assertThat(graphJson).contains("\"artifactId\" : \"gradle-sample\"");
        assertThat(graphJson).contains("\"artifactId\" : \"slf4j-api\"");
        assertThat(result.getOutput()).contains("Open this URI manually:");
    }

    @Test
    void aggregatesMultiProjectGraphWithSharedDependency() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
            rootProject.name = "gradle-platform"
            include("api", "worker")
            """);
        Files.createDirectories(projectDir.resolve("api"));
        Files.createDirectories(projectDir.resolve("worker"));
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins { id("io.github.lucasgois1.depviz") }
            allprojects {
                group = "com.acme"
                version = "1.0.0"
                repositories { mavenCentral() }
            }
            subprojects {
                apply(plugin = "java")
                dependencies {
                    "runtimeOnly"("org.slf4j:slf4j-api:2.0.13")
                }
            }
            depviz { open.set(false) }
            """);
        Files.writeString(projectDir.resolve("api/build.gradle.kts"), "");
        Files.writeString(projectDir.resolve("worker/build.gradle.kts"), "");

        var result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("depvizOpen", "--stacktrace")
            .build();

        Path json = projectDir.resolve("build/depviz/dependency-graph.json");
        String text = Files.readString(json);

        assertThat(result.task(":depvizOpen").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(text).contains("\"type\" : \"reactor\"");
        assertThat(text).contains("\"artifactId\" : \"api\"");
        assertThat(text).contains("\"artifactId\" : \"worker\"");
        assertThat(countOccurrences(text, "\"artifactId\" : \"slf4j-api\"")).isEqualTo(1);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
