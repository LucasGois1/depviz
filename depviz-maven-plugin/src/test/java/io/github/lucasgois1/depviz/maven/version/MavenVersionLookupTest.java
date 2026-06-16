package io.github.lucasgois1.depviz.maven.version;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.version.ArtifactVersionKey;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.junit.jupiter.api.Test;

class MavenVersionLookupTest {
    @Test
    void buildsPomMetadataRequestThatCanDiscoverNewerVersions() {
        RemoteRepository repository = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
        MavenVersionLookup lookup = new MavenVersionLookup(null, null, List.of(repository));

        VersionRangeRequest request = lookup.requestFor(new ArtifactVersionKey("org.example", "library"));

        Artifact artifact = request.getArtifact();
        assertThat(artifact.getGroupId()).isEqualTo("org.example");
        assertThat(artifact.getArtifactId()).isEqualTo("library");
        assertThat(artifact.getExtension()).isEqualTo("pom");
        assertThat(artifact.getClassifier()).isEmpty();
        assertThat(artifact.getVersion()).isEqualTo("[0,)");
        assertThat(request.getRepositories()).containsExactly(repository);
    }
}
