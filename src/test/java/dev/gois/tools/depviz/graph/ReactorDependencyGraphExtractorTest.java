package dev.gois.tools.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizConfig;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class ReactorDependencyGraphExtractorTest {
    private final DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, null, Path.of("target/depviz"));

    @Test
    void createsVirtualReactorRootWithModuleChildren() throws Exception {
        MavenProject root = project("com.acme", "platform", "1.0.0", "pom");
        MavenProject api = project("com.acme", "api", "1.0.0", "jar");
        MavenProject worker = project("com.acme", "worker", "1.0.0", "jar");
        TestSingleExtractor single = new TestSingleExtractor(
            extracted("com.acme", "api", "1.0.0", dependency("org.slf4j", "slf4j-api", "2.0.13")),
            extracted("com.acme", "worker", "1.0.0", dependency("org.slf4j", "slf4j-api", "2.0.13"))
        );

        ExtractedDependencyNode aggregate = new ReactorDependencyGraphExtractor(single).extract(root, List.of(root, api, worker), config);

        assertThat(aggregate.coordinate().type()).isEqualTo("reactor");
        assertThat(aggregate.scope()).isEqualTo("root");
        assertThat(aggregate.children()).extracting(child -> child.coordinate().artifactId()).containsExactly("api", "worker");
        assertThat(aggregate.children()).extracting(ExtractedDependencyNode::scope).containsExactly("module", "module");
    }

    @Test
    void usesSingleProjectExtractionWhenNoModuleProjectsAreAvailable() throws Exception {
        MavenProject api = project("com.acme", "api", "1.0.0", "jar");
        ExtractedDependencyNode singleRoot = extracted("com.acme", "api", "1.0.0");

        ExtractedDependencyNode aggregate = new ReactorDependencyGraphExtractor(new TestSingleExtractor(singleRoot)).extract(api, List.of(api), config);

        assertThat(aggregate).isSameAs(singleRoot);
    }

    private static MavenProject project(String groupId, String artifactId, String version, String packaging) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        project.setPackaging(packaging);
        return project;
    }

    private static ExtractedDependencyNode extracted(String groupId, String artifactId, String version, ExtractedDependencyNode... children) {
        return new ExtractedDependencyNode(new ArtifactCoordinate(groupId, artifactId, "jar", "", version), "root", false, List.of(children), List.of());
    }

    private static ExtractedDependencyNode dependency(String groupId, String artifactId, String version) {
        return new ExtractedDependencyNode(new ArtifactCoordinate(groupId, artifactId, "jar", "", version), "compile", false, List.of(), List.of());
    }

    private static final class TestSingleExtractor implements DependencyGraphExtractor {
        private final List<ExtractedDependencyNode> roots;
        private int index;

        private TestSingleExtractor(ExtractedDependencyNode... roots) {
            this.roots = List.of(roots);
        }

        @Override
        public ExtractedDependencyNode extract(MavenProject project, DepvizConfig config) {
            return roots.get(index++);
        }
    }
}
