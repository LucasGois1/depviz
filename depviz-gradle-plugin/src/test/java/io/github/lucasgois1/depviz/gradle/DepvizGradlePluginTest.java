package io.github.lucasgois1.depviz.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DepvizGradlePluginTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path projectDir;

    @Test
    void configuresExtensionDefaults() {
        var project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(DepvizGradlePlugin.class);

        var extension = project.getExtensions().getByType(DepvizExtension.class);
        project.getLayout().getBuildDirectory().set(project.getLayout().getProjectDirectory().dir("custom-build"));

        assertThat(extension.getScope().get()).isEqualTo("runtime");
        assertThat(extension.getOpen().get()).isTrue();
        assertThat(extension.getSnyk().get()).isEqualTo("auto");
        assertThat(extension.getLayout().get()).isEqualTo("breadthfirst");
        assertThat(extension.getOutputDirectory().get().getAsFile()).isEqualTo(project.getLayout().getProjectDirectory().dir("custom-build/depviz").getAsFile());
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
        JsonNode document = OBJECT_MAPPER.readTree(text);
        JsonNode project = document.path("project");
        List<JsonNode> moduleNodes = nodesByArtifactId(document, "api");
        moduleNodes.addAll(nodesByArtifactId(document, "worker"));
        JsonNode sharedDependency = onlyNodeByArtifactId(document, "slf4j-api");

        assertThat(result.task(":depvizOpen").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(text).contains("\"type\" : \"reactor\"");
        assertThat(project.path("multiModule").asBoolean()).isTrue();
        assertThat(textValues(project.path("modules"))).containsExactly(":api", ":worker");
        assertThat(moduleNodes)
            .extracting(node -> node.path("artifactId").asText())
            .containsExactlyInAnyOrder("api", "worker");
        assertThat(moduleNodes).allSatisfy(node -> {
            assertThat(node.path("scope").asText()).isEqualTo("module");
            assertThat(node.path("moduleRoot").asBoolean()).isTrue();
        });
        assertThat(nodesByArtifactId(document, "slf4j-api")).hasSize(1);
        assertThat(incomingSources(document, sharedDependency.path("id").asText()))
            .containsExactlyInAnyOrderElementsOf(moduleNodes.stream().map(node -> node.path("id").asText()).toList());
    }

    @Test
    void compileScopeUsesCompileClasspath() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"scope-sample\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("io.github.lucasgois1.depviz")
            }
            repositories { mavenCentral() }
            dependencies {
                implementation("org.slf4j:slf4j-api:2.0.13")
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-api:5.14.1")
            }
            depviz {
                scope.set("compile")
                open.set(false)
            }
            """);

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("depvizOpen", "--stacktrace")
            .build();

        JsonNode document = OBJECT_MAPPER.readTree(Files.readString(projectDir.resolve("build/depviz/dependency-graph.json")));
        assertThat(nodesByArtifactId(document, "slf4j-api")).hasSize(1);
        assertThat(nodesByArtifactId(document, "junit-jupiter-api")).isEmpty();
    }

    @Test
    void customOutputDirectoryIsRespected() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"output-sample\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("io.github.lucasgois1.depviz")
            }
            repositories { mavenCentral() }
            depviz {
                outputDirectory.set(project.layout.projectDirectory.dir("custom-depviz"))
                open.set(false)
            }
            """);

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("depvizOpen", "--stacktrace")
            .build();

        assertThat(projectDir.resolve("custom-depviz/dependency-graph.json")).exists();
    }

    @Test
    void allScopeIncludesCompileRuntimeAndTestConfigurations() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"all-scope-sample\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("io.github.lucasgois1.depviz")
            }
            repositories { mavenCentral() }
            dependencies {
                implementation("org.slf4j:slf4j-api:2.0.13")
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-api:5.14.1")
            }
            depviz {
                scope.set("all")
                open.set(false)
            }
            """);

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("depvizOpen", "--stacktrace")
            .build();

        JsonNode document = OBJECT_MAPPER.readTree(Files.readString(projectDir.resolve("build/depviz/dependency-graph.json")));
        JsonNode root = rootNode(document);
        JsonNode slf4jApi = onlyNodeByArtifactId(document, "slf4j-api");
        JsonNode junitJupiterApi = onlyNodeByArtifactId(document, "junit-jupiter-api");

        assertThat(nodesByArtifactId(document, "slf4j-api")).hasSize(1);
        assertThat(nodesByArtifactId(document, "junit-jupiter-api")).hasSize(1);
        assertThat(incomingScopes(document, root.path("id").asText(), slf4jApi.path("id").asText()))
            .containsExactlyInAnyOrder("compileClasspath", "runtimeClasspath", "testRuntimeClasspath");
        assertThat(incomingScopes(document, root.path("id").asText(), junitJupiterApi.path("id").asText()))
            .containsExactly("testRuntimeClasspath");
    }

    private static JsonNode rootNode(JsonNode document) {
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode node : document.path("nodes")) {
            if (node.path("root").asBoolean()) {
                matches.add(node);
            }
        }
        assertThat(matches).hasSize(1);
        return matches.get(0);
    }

    private static JsonNode onlyNodeByArtifactId(JsonNode document, String artifactId) {
        List<JsonNode> nodes = nodesByArtifactId(document, artifactId);
        assertThat(nodes).hasSize(1);
        return nodes.get(0);
    }

    private static List<JsonNode> nodesByArtifactId(JsonNode document, String artifactId) {
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode node : document.path("nodes")) {
            if (node.path("artifactId").asText().equals(artifactId)) {
                matches.add(node);
            }
        }
        return matches;
    }

    private static List<String> incomingSources(JsonNode document, String targetId) {
        List<String> sources = new ArrayList<>();
        for (JsonNode edge : document.path("edges")) {
            if (edge.path("target").asText().equals(targetId)) {
                sources.add(edge.path("source").asText());
            }
        }
        return sources;
    }

    private static List<String> incomingScopes(JsonNode document, String sourceId, String targetId) {
        List<String> scopes = new ArrayList<>();
        for (JsonNode edge : document.path("edges")) {
            if (edge.path("source").asText().equals(sourceId) && edge.path("target").asText().equals(targetId)) {
                scopes.add(edge.path("scope").asText());
            }
        }
        return scopes;
    }

    private static List<String> textValues(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : arrayNode) {
            values.add(value.asText());
        }
        return values;
    }
}
