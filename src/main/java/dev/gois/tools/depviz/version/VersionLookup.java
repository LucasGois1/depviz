package dev.gois.tools.depviz.version;

import java.util.List;

public interface VersionLookup {
    List<String> availableVersions(ArtifactVersionKey key, String currentVersion) throws Exception;
}
