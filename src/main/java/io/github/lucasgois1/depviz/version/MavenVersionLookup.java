package io.github.lucasgois1.depviz.version;

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

public final class MavenVersionLookup implements VersionLookup {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public MavenVersionLookup(
        RepositorySystem repositorySystem,
        RepositorySystemSession session,
        List<RemoteRepository> repositories
    ) {
        this.repositorySystem = repositorySystem;
        this.session = session;
        this.repositories = repositories == null ? List.of() : List.copyOf(repositories);
    }

    @Override
    public List<String> availableVersions(ArtifactVersionKey key, String currentVersion)
        throws VersionRangeResolutionException {
        VersionRangeRequest request = requestFor(key);
        VersionRangeResult result = repositorySystem.resolveVersionRange(session, request);
        return result.getVersions().stream().map(Version::toString).toList();
    }

    VersionRangeRequest requestFor(ArtifactVersionKey key) {
        return new VersionRangeRequest(
            new DefaultArtifact(key.groupId(), key.artifactId(), "", "pom", "[0,)"),
            repositories,
            null
        );
    }
}
