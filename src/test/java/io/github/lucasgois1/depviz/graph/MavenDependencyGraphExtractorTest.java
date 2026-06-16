package io.github.lucasgois1.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.junit.jupiter.api.Test;

class MavenDependencyGraphExtractorTest {
    @Test
    void extractsProjectRootWithRootScope() throws Exception {
        MavenProject project = new MavenProject();
        project.setArtifact(projectArtifact());

        MavenDependencyGraphExtractor extractor = new MavenDependencyGraphExtractor(request ->
            new TestDependencyNode(project.getArtifact())
        );

        ExtractedDependencyNode root = extractor.extract(project, defaultConfig());

        assertThat(root.scope()).isEqualTo("root");
    }

    private static DepvizConfig defaultConfig() {
        return DepvizConfig.fromRaw(null, "false", null, null, null, null, null, null);
    }

    private static Artifact projectArtifact() {
        return new DefaultArtifact(
            "com.acme",
            "demo",
            "1.0.0",
            "compile",
            "jar",
            "",
            new JarArtifactHandler()
        );
    }

    private record TestDependencyNode(Artifact artifact) implements DependencyNode {
        @Override
        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public List<DependencyNode> getChildren() {
            return List.of();
        }

        @Override
        public boolean accept(DependencyNodeVisitor visitor) {
            return true;
        }

        @Override
        public DependencyNode getParent() {
            return null;
        }

        @Override
        public String getPremanagedVersion() {
            return null;
        }

        @Override
        public String getPremanagedScope() {
            return null;
        }

        @Override
        public String getVersionConstraint() {
            return null;
        }

        @Override
        public String toNodeString() {
            return artifact.toString();
        }

        @Override
        public Boolean getOptional() {
            return artifact.isOptional();
        }

        @Override
        public List<Exclusion> getExclusions() {
            return List.of();
        }
    }

    private static final class JarArtifactHandler implements ArtifactHandler {
        @Override
        public String getExtension() {
            return "jar";
        }

        @Override
        public String getDirectory() {
            return "jar";
        }

        @Override
        public String getClassifier() {
            return "";
        }

        @Override
        public String getPackaging() {
            return "jar";
        }

        @Override
        public boolean isIncludesDependencies() {
            return false;
        }

        @Override
        public String getLanguage() {
            return "java";
        }

        @Override
        public boolean isAddedToClasspath() {
            return true;
        }
    }
}
