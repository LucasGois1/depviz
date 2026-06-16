# Publishing Depviz

Depviz uses one version across core, Maven plugin, Gradle plugin, CLI, schema, and viewer.

## Required Accounts

- GitHub repository: `https://github.com/lucasgois1/depviz`
- Maven Central Portal namespace: `io.github.lucasgois1.depviz`
- Gradle Plugin Portal plugin ID: `io.github.lucasgois1.depviz`
- GPG/PGP key for signing Maven Central artifacts

References:

- Maven Central requirements: `https://central.sonatype.org/publish/requirements/`
- Maven Central namespace registration: `https://central.sonatype.org/register/namespace/`
- Maven Central Maven publishing plugin: `https://central.sonatype.org/publish/publish-portal-maven/`
- Gradle Plugin Portal publishing: `https://plugins.gradle.org/docs/publish-plugin`

## Local Verification

```bash
scripts/set-version.sh 0.1.0
mvn -B -Dstyle.color=never verify
mvn -B -pl depviz-core -am install -DskipTests
./gradlew :depviz-gradle-plugin:test --rerun-tasks --no-daemon
mvn -B -DskipTests package
scripts/prepare-release-assets.sh 0.1.0
./gradlew :depviz-gradle-plugin:publishToMavenLocal
```

## Public Distribution Targets

- Maven Central: `depviz-core`, `depviz-maven-plugin`, `depviz-cli`
- Gradle Plugin Portal: `io.github.lucasgois1.depviz`
- GitHub Releases: `depviz-cli.jar`, `install.sh`, `checksums.txt`

Remote publication requires signing credentials, Maven Central namespace setup, Gradle Plugin Portal credentials, and a GitHub release token.

## GitHub Secrets

Create these repository secrets under GitHub repository settings, in **Secrets and variables > Actions**:

| Secret | Used by | Meaning |
| --- | --- | --- |
| `CENTRAL_USERNAME` | `Publish Maven Central` | Maven Central Portal token username |
| `CENTRAL_PASSWORD` | `Publish Maven Central` | Maven Central Portal token password |
| `MAVEN_GPG_PRIVATE_KEY` | `Publish Maven Central` | ASCII-armored private GPG key used to sign artifacts |
| `MAVEN_GPG_PASSPHRASE` | `Publish Maven Central` | Passphrase for `MAVEN_GPG_PRIVATE_KEY` |
| `GRADLE_PUBLISH_KEY` | `Publish Gradle Plugin` | Gradle Plugin Portal API key |
| `GRADLE_PUBLISH_SECRET` | `Publish Gradle Plugin` | Gradle Plugin Portal API secret |

GitHub release creation uses the built-in `GITHUB_TOKEN`; no extra release token is required for the included workflow.

## First Release Order

1. Confirm `LICENSE` is acceptable. The repository currently uses Apache-2.0 with copyright `Copyright 2026 Lucas Gois`.
2. Create or verify the Maven Central namespace `io.github.lucasgois1.depviz`.
3. Create a Maven Central Portal publishing token and save it as `CENTRAL_USERNAME` and `CENTRAL_PASSWORD`.
4. Create or choose a GPG key, export the private key, and save it as `MAVEN_GPG_PRIVATE_KEY`; save the passphrase as `MAVEN_GPG_PASSPHRASE`.
5. Create a Gradle Plugin Portal account and API key, then save `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`.
6. Push `main`.
7. Create and push tag `v0.1.0`, or run the `GitHub Release` workflow manually with version `0.1.0`.
8. Confirm the GitHub release contains `depviz-cli.jar`, `install.sh`, and `checksums.txt`.
9. Run the `Publish Maven Central` workflow with version `0.1.0`. Keep `auto_publish=false` for the first release so you can review the Central deployment before clicking publish.
10. Run the `Publish Gradle Plugin` workflow with version `0.1.0` after Maven Central has the `depviz-core` artifact available.

## Manual Commands

Tag a release:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Install from the public GitHub release:

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | sh
```

Install a specific release:

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | DEPVIZ_VERSION=0.1.0 sh
```

Validate the installed CLI from another Java project:

```bash
~/.depviz/bin/depviz open --project-dir /path/to/java/project --no-browser
```

## Workflow Responsibilities

- `CI`: runs Maven verification and Gradle plugin tests on pushes and pull requests.
- `GitHub Release`: sets the requested release version in the workspace, runs verification, builds `dist/` assets, and creates or updates the GitHub release.
- `Publish Maven Central`: sets the release version, imports the GPG key, writes Maven Central credentials, verifies the project, and runs `mvn -Prelease deploy`.
- `Publish Gradle Plugin`: sets the release version, installs `depviz-core` locally for the Gradle build, and runs `publishPlugins`.

## Notes

- Maven Central releases are immutable. If `0.1.0` is published incorrectly, the next fix must be `0.1.1`.
- The Gradle Plugin Portal can require first-plugin approval. Watch the workflow output and plugin portal UI after the first submission.
- The CLI uses the jar manifest `Implementation-Version` at runtime, so `depviz-cli.jar` from `v0.1.0` delegates to Maven/Gradle plugins `0.1.0`.
- The repository can keep developing on `0.1.0-SNAPSHOT`; the release workflows rewrite versions in the temporary GitHub Actions workspace.

## Historical Notes

Historical Superpowers plans may mention pre-public prototype coordinates such as `dev.gois.tools` or the old `lucasgois/depviz` repository path. Those references are archival and are not the current public distribution contract.
