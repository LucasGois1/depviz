package io.github.lucasgois1.depviz.graph;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public final class ReactorDependencyGraphExtractor {
    private final DependencyGraphExtractor singleProjectExtractor;

    public ReactorDependencyGraphExtractor(DependencyGraphExtractor singleProjectExtractor) {
        this.singleProjectExtractor = Objects.requireNonNull(singleProjectExtractor, "singleProjectExtractor is required.");
    }

    public ExtractedDependencyNode extract(MavenProject executionProject, List<MavenProject> reactorProjects, DepvizConfig config) throws MojoExecutionException {
        Objects.requireNonNull(executionProject, "executionProject is required.");
        Objects.requireNonNull(config, "config is required.");

        List<MavenProject> modules = moduleProjects(executionProject, reactorProjects);
        if (modules.isEmpty() || (modules.size() == 1 && modules.get(0) == executionProject)) {
            return singleProjectExtractor.extract(executionProject, config);
        }

        List<ExtractedDependencyNode> moduleRoots = new ArrayList<>();
        for (MavenProject module : modules) {
            moduleRoots.add(extractModule(module, config));
        }

        return new ExtractedDependencyNode(
            reactorCoordinate(executionProject),
            "root",
            false,
            moduleRoots,
            List.of()
        );
    }

    private ExtractedDependencyNode extractModule(MavenProject project, DepvizConfig config) throws MojoExecutionException {
        try {
            ExtractedDependencyNode root = singleProjectExtractor.extract(project, config);
            return new ExtractedDependencyNode(root.coordinate(), "module", false, root.children(), root.diagnostics());
        } catch (MojoExecutionException exception) {
            throw new MojoExecutionException("Failed to extract module " + project.getArtifactId() + ": " + exception.getMessage(), exception);
        }
    }

    private static List<MavenProject> moduleProjects(MavenProject executionProject, List<MavenProject> reactorProjects) {
        if (reactorProjects == null || reactorProjects.isEmpty()) {
            return List.of(executionProject);
        }
        List<MavenProject> projects = reactorProjects.stream()
            .filter(project -> project != null)
            .filter(project -> project != executionProject || !"pom".equals(project.getPackaging()))
            .toList();
        return projects.isEmpty() ? List.of(executionProject) : projects;
    }

    private static ArtifactCoordinate reactorCoordinate(MavenProject project) {
        return new ArtifactCoordinate(
            valueOrUnknown(project.getGroupId()),
            valueOrUnknown(project.getArtifactId()) + "-reactor",
            "reactor",
            "",
            valueOrUnknown(project.getVersion())
        );
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
