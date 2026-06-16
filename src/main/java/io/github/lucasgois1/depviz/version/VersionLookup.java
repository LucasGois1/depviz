package io.github.lucasgois1.depviz.version;

import java.util.List;

public interface VersionLookup {
    List<String> availableVersions(ArtifactVersionKey key, String currentVersion) throws Exception;
}
