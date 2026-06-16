package io.github.lucasgois1.depviz.graph;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.util.PatternMatcher;
import java.util.List;
import java.util.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

public final class MavenDependencyGraphExtractor implements DependencyGraphExtractor {
    private static final String FAILURE_PREFIX = "Failed to resolve Maven dependency graph: ";

    private final DependencyCollectorBuilder dependencyCollectorBuilder;

    public MavenDependencyGraphExtractor(DependencyCollectorBuilder dependencyCollectorBuilder) {
        this.dependencyCollectorBuilder = Objects.requireNonNull(
            dependencyCollectorBuilder,
            "dependencyCollectorBuilder is required."
        );
    }

    @Override
    public ExtractedDependencyNode extract(MavenProject project, DepvizConfig config) throws MojoExecutionException {
        Objects.requireNonNull(project, "project is required.");
        Objects.requireNonNull(config, "config is required.");

        try {
            DependencyNode resolvedRoot = dependencyCollectorBuilder.collectDependencyGraph(buildingRequest(project), null);
            ExtractedDependencyNode root = new ExtractedDependencyNode(
                projectCoordinate(project),
                "root",
                optional(project.getArtifact()),
                resolvedRoot.getChildren().stream().map(this::toExtractedNode).toList(),
                List.of()
            );
            return GraphFilters.apply(
                root,
                config.scope(),
                PatternMatcher.parseList(config.includes()),
                PatternMatcher.parseList(config.excludes())
            );
        } catch (DependencyCollectorBuilderException | IllegalArgumentException exception) {
            throw new MojoExecutionException(FAILURE_PREFIX + exception.getMessage(), exception);
        }
    }

    private static ProjectBuildingRequest buildingRequest(MavenProject project) {
        ProjectBuildingRequest source = project.getProjectBuildingRequest();
        ProjectBuildingRequest request = source == null
            ? new DefaultProjectBuildingRequest()
            : new DefaultProjectBuildingRequest(source);
        request.setProject(project);
        request.setResolveDependencies(true);
        request.setResolveVersionRanges(true);
        return request;
    }

    private ExtractedDependencyNode toExtractedNode(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        return new ExtractedDependencyNode(
            artifactCoordinate(artifact),
            scope(artifact),
            optional(node, artifact),
            node.getChildren().stream().map(this::toExtractedNode).toList(),
            List.of()
        );
    }

    private static ArtifactCoordinate projectCoordinate(MavenProject project) {
        Artifact artifact = project.getArtifact();
        if (artifact != null) {
            return artifactCoordinate(artifact);
        }
        return new ArtifactCoordinate(
            project.getGroupId(),
            project.getArtifactId(),
            project.getPackaging(),
            "",
            project.getVersion()
        );
    }

    private static ArtifactCoordinate artifactCoordinate(Artifact artifact) {
        return new ArtifactCoordinate(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getType(),
            artifact.getClassifier(),
            baseVersion(artifact)
        );
    }

    private static String baseVersion(Artifact artifact) {
        String baseVersion = artifact.getBaseVersion();
        if (baseVersion != null && !baseVersion.isBlank()) {
            return baseVersion;
        }
        return artifact.getVersion();
    }

    private static String scope(Artifact artifact) {
        return artifact == null ? null : artifact.getScope();
    }

    private static boolean optional(Artifact artifact) {
        return artifact != null && artifact.isOptional();
    }

    private static boolean optional(DependencyNode node, Artifact artifact) {
        Boolean nodeOptional = node.getOptional();
        if (nodeOptional != null) {
            return nodeOptional;
        }
        return optional(artifact);
    }
}
