# Publishing Depviz

Depviz uses one version across core, Maven plugin, Gradle plugin, CLI, schema, and viewer.

## Local Verification

```bash
mvn verify
mvn install -DskipTests
./gradlew :depviz-gradle-plugin:test
./gradlew :depviz-gradle-plugin:publishToMavenLocal
```

## Public Distribution Targets

- Maven Central: `depviz-core`, `depviz-maven-plugin`, `depviz-cli`
- Gradle Plugin Portal: `io.github.lucasgois1.depviz`
- GitHub Releases: `depviz-cli.jar`, `depviz`, `install.sh`, `checksums.txt`

Remote publication requires signing credentials, Maven Central namespace setup, Gradle Plugin Portal credentials, and a GitHub release token.

## Historical Notes

Historical Superpowers plans may mention pre-public prototype coordinates such as `dev.gois.tools` or the old `lucasgois/depviz` repository path. Those references are archival and are not the current public distribution contract.
