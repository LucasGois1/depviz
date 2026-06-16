package io.github.lucasgois1.depviz.gradle;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import io.github.lucasgois1.depviz.graph.DependencyNodeInput;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;

final class GradleDependencyGraphExtractor {
    DependencyNodeInput extract(Project project, String scope) {
        Configuration configuration = configuration(project, scope);
        Set<DependencyNodeInput> children = new LinkedHashSet<>();
        for (ResolvedDependency dependency : configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
            children.add(toNode(dependency, scope));
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

    private static Configuration configuration(Project project, String scope) {
        String name = switch (scope) {
            case "compile" -> "compileClasspath";
            case "test" -> "testRuntimeClasspath";
            case "runtime", "all" -> "runtimeClasspath";
            default -> throw new IllegalArgumentException("Unsupported Gradle depviz scope: " + scope);
        };
        return project.getConfigurations().getByName(name);
    }

    private static String stringOrDefault(Object value, String fallback) {
        String string = value == null ? "" : value.toString();
        return string.isBlank() ? fallback : string.trim();
    }
}
