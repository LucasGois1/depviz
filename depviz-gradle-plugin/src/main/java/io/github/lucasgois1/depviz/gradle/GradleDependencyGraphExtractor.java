package io.github.lucasgois1.depviz.gradle;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import io.github.lucasgois1.depviz.graph.DependencyNodeInput;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;

final class GradleDependencyGraphExtractor {
    DependencyNodeInput extractAggregate(Project rootProject, String scope) {
        List<DependencyNodeInput> projectRoots = rootProject.getAllprojects().stream()
            .filter(project -> project != rootProject)
            .filter(project -> project.getPlugins().hasPlugin("java"))
            .sorted(Comparator.comparing(Project::getPath))
            .map(project -> withModuleScope(extract(project, scope)))
            .toList();
        if (projectRoots.isEmpty()) {
            return extract(rootProject, scope);
        }
        return new DependencyNodeInput(
            new ArtifactCoordinate(
                stringOrDefault(rootProject.getGroup(), "unknown"),
                rootProject.getName() + "-reactor",
                "reactor",
                "",
                stringOrDefault(rootProject.getVersion(), "unspecified")
            ),
            "root",
            false,
            false,
            projectRoots,
            List.of()
        );
    }

    DependencyNodeInput extract(Project project, String scope) {
        Set<DependencyNodeInput> children = new LinkedHashSet<>();
        for (Configuration configuration : configurations(project, scope)) {
            String nodeScope = scopeFor(scope, configuration);
            for (ResolvedDependency dependency : configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
                children.add(toNode(dependency, nodeScope));
            }
        }
        return new DependencyNodeInput(
            new ArtifactCoordinate(
                stringOrDefault(project.getGroup(), "unknown"),
                project.getName(),
                "gradle-project",
                "",
                stringOrDefault(project.getVersion(), "unspecified")
            ),
            "root",
            false,
            false,
            List.copyOf(children),
            List.of()
        );
    }

    private static DependencyNodeInput withModuleScope(DependencyNodeInput node) {
        return new DependencyNodeInput(
            node.coordinate(),
            "module",
            false,
            true,
            node.children(),
            node.diagnostics()
        );
    }

    private static DependencyNodeInput toNode(ResolvedDependency dependency, String scope) {
        return new DependencyNodeInput(
            new ArtifactCoordinate(
                dependency.getModuleGroup(),
                dependency.getModuleName(),
                "jar",
                "",
                dependency.getModuleVersion()
            ),
            scope,
            false,
            false,
            dependency.getChildren().stream().map(child -> toNode(child, scope)).toList(),
            List.of()
        );
    }

    private static List<Configuration> configurations(Project project, String scope) {
        return switch (scope) {
            case "compile" -> List.of(configurationByName(project, "compileClasspath"));
            case "runtime" -> List.of(configurationByName(project, "runtimeClasspath"));
            case "test" -> List.of(configurationByName(project, "testRuntimeClasspath"));
            case "all" -> List.of(
                configurationByName(project, "compileClasspath"),
                configurationByName(project, "runtimeClasspath"),
                configurationByName(project, "testRuntimeClasspath")
            );
            default -> throw new IllegalArgumentException("Unsupported Gradle depviz scope: " + scope);
        };
    }

    private static Configuration configurationByName(Project project, String name) {
        return project.getConfigurations().getByName(name);
    }

    private static String scopeFor(String requestedScope, Configuration configuration) {
        if ("all".equals(requestedScope)) {
            return configuration.getName();
        }
        return requestedScope;
    }

    private static String stringOrDefault(Object value, String fallback) {
        String string = value == null ? "" : value.toString();
        return string.isBlank() ? fallback : string.trim();
    }
}
