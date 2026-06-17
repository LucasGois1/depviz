# Depviz

Depviz generates an offline interactive dependency graph for Java projects.

Run it from a Maven or Gradle project, open the generated HTML, and inspect how dependencies relate to each other as a real graph. Shared dependencies are rendered once, with every dependent package pointing to the same node.

## Features

- Interactive graph viewer with pan, zoom, fit, reset, and node selection.
- Real graph semantics: shared dependencies are deduplicated and shown with multiple incoming edges.
- Search across artifact id, group id, version, scope, classifier, coordinates, and labels.
- Filters for scopes, optional dependencies, update status, and vulnerability severity.
- Multiple graph layouts: dependency map, force, circle, and concentric.
- Toggle labels, version badges, and security badges directly on the canvas.
- Selected-node details with parents, children, dependency paths, versions, and security findings.
- Maven multi-module support through the Maven plugin.
- Gradle multi-project support through the Gradle plugin.
- Optional dependency version checks for Maven projects.
- Optional Snyk vulnerability enrichment through the Snyk CLI or an existing Snyk JSON report.
- Static output: the viewer runs from local files and does not need a server, CDN, or runtime network access.

## Requirements

- Java 17+
- Maven 3.9+ for Maven projects
- Gradle for Gradle projects
- Snyk CLI only if you want live vulnerability enrichment

Node.js is only needed when building Depviz itself from source. Projects that only run Depviz do not need Node.js.

## Install The CLI

The recommended way to use Depviz is the CLI wrapper. It detects Maven or Gradle and delegates to the matching Depviz plugin version.

The CLI does not modify `pom.xml`, `build.gradle`, or `settings.gradle` files.

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | sh
```

Add the installed binary directory to your `PATH` if the installer prints that instruction:

```bash
export PATH="$HOME/.depviz/bin:$PATH"
```

Verify the installation:

```bash
depviz version
```

To install a specific release:

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | DEPVIZ_VERSION=0.1.0 sh
```

## Quick Start

Run from the root of a Maven or Gradle project:

```bash
depviz open
```

Depviz writes the viewer to the project build directory and opens it in your default browser.

For a faster first run in large projects or restricted networks:

```bash
depviz open --no-browser --no-snyk --no-updates
```

Use help for the full CLI reference:

```bash
depviz help
depviz open --help
```

## CLI Commands

| Command | Description |
| --- | --- |
| `depviz open` | Generate and open the dependency graph viewer for the current project. |
| `depviz open --no-browser` | Generate files without opening a browser. |
| `depviz open --project-dir ../service-api` | Analyze a project outside the current directory. |
| `depviz open --tool maven` | Force Maven when both Maven and Gradle files exist. |
| `depviz open --tool gradle` | Force Gradle when both Maven and Gradle files exist. |
| `depviz open --scope compile` | Generate a graph for a specific dependency scope. |
| `depviz open --layout force` | Choose the initial graph layout. |
| `depviz open --output target/custom-depviz` | Write generated files to a custom directory. |
| `depviz open --no-updates` | Disable dependency version checks. Maven only. |
| `depviz open --refresh-dependencies` | Refresh dependency metadata. Adds `-U` for Maven and `--refresh-dependencies` for Gradle. |
| `depviz open --no-snyk` | Disable Snyk enrichment. |
| `depviz open --snyk` | Run Snyk enrichment explicitly. |
| `depviz open --snyk-json target/snyk.json` | Read vulnerabilities from an existing Snyk JSON report. |
| `depviz version` | Print the installed CLI version. |
| `depviz help` | Show CLI help. |

## Direct Maven Usage

You can run the Maven plugin without installing the CLI:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open
```

If your Maven setup resolves the `depviz` plugin prefix, the shorter form also works:

```bash
mvn depviz:open
```

Generate without opening the browser:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.open=false
```

Common Maven examples:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.scope=compile
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.checkUpdates=false
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.snyk=false
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.snykJson=target/snyk.json
```

## Direct Gradle Usage

Apply the Gradle plugin:

```kotlin
plugins {
  id("io.github.lucasgois1.depviz") version "0.1.0"
}
```

Run:

```bash
./gradlew depvizOpen
```

Optional configuration:

```kotlin
depviz {
  scope.set("runtime")
  layout.set("breadthfirst")
  open.set(true)
  outputDirectory.set(project.layout.buildDirectory.dir("depviz"))
  snyk.set("auto")
  snykCommand.set("snyk")
}
```

Gradle support currently generates the graph and can enrich it with Snyk data. Dependency version update checks are currently Maven-only.

## Snyk Vulnerability Insights

Snyk enrichment is optional. Depviz can either run the Snyk CLI while generating the graph or read an existing Snyk JSON report.

Run Snyk through Depviz:

```bash
depviz open --snyk
```

Use an existing report:

```bash
depviz open --snyk-json target/snyk.json
```

Disable Snyk:

```bash
depviz open --no-snyk
```

Security findings do not fail graph generation. When findings are mapped, the viewer shows security badges, severity filters, summary counts, and selected-node details. If Snyk is unavailable, Depviz records diagnostics and still generates the viewer.

## Version Update Insights

For Maven projects, Depviz checks repository metadata during generation and adds version badges, update filters, and selected-node version details.

Disable update checks when working offline, behind a slow mirror, or when you only need the graph:

```bash
depviz open --no-updates
```

The generated viewer does not call Maven Central or any repository at runtime. Update checks happen only while generating the graph.

## Output Files

Default Maven output:

```text
target/depviz/
  dependency-graph.html
  dependency-graph.json
  assets/
    app.js
    style.css
    LICENSES.txt
```

Default Gradle output:

```text
build/depviz/
  dependency-graph.html
  dependency-graph.json
  assets/
    app.js
    style.css
    LICENSES.txt
```

Open `dependency-graph.html` in a browser. The JSON file is useful for debugging or tooling.

## Configuration Reference

### Maven Properties

| Property | Default | Description |
| --- | --- | --- |
| `depviz.scope` | `runtime` | Dependency scope: `compile`, `runtime`, `test`, `provided`, `system`, `import`, or `all`. |
| `depviz.open` | `true` | Open the generated HTML after writing files. |
| `depviz.outputDirectory` | `target/depviz` | Output directory. |
| `depviz.layout` | `breadthfirst` | Initial layout: `breadthfirst`, `force`, `circle`, or `concentric`. |
| `depviz.maxInitialLabels` | `500` | Start with labels enabled only when the graph has at most this many nodes. |
| `depviz.checkUpdates` | `true` | Enable Maven dependency version checks. |
| `depviz.snyk` | `auto` | Snyk mode: `auto`, `true`, or `false`. |
| `depviz.snykJson` | none | Read Snyk findings from a local JSON report. |
| `depviz.snykOrg` | none | Pass `--org` to the Snyk CLI. |
| `depviz.snykAllProjects` | `false` | Pass `--all-projects` to the Snyk CLI. |
| `depviz.snykCommand` | `snyk` | Snyk executable or wrapper command. |
| `depviz.includes` | none | Keep branches containing matching dependencies. Supports `*` wildcards. |
| `depviz.excludes` | none | Remove matching dependency subtrees. Supports `*` wildcards. |
| `depviz.nodeMode` | `artifact` | Only `artifact` is currently supported. |

Include and exclude patterns use `groupId:artifactId` or `groupId:artifactId:type:version`, for example:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.includes=org.springframework:*
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.excludes=junit:*,org.junit:*,org.mockito:*
```

### Gradle Extension

| Property | Default | Description |
| --- | --- | --- |
| `scope` | `runtime` | Dependency scope. |
| `open` | `true` | Open the generated HTML after writing files. |
| `outputDirectory` | `build/depviz` | Output directory. |
| `layout` | `breadthfirst` | Initial layout. |
| `snyk` | `auto` | Snyk mode: `auto`, `true`, or `false`. |
| `snykJson` | none | Read Snyk findings from a local JSON report. |
| `snykOrg` | none | Pass `--org` to the Snyk CLI. |
| `snykAllProjects` | `false` | Pass `--all-projects` to the Snyk CLI. |
| `snykCommand` | `snyk` | Snyk executable or wrapper command. |

## Troubleshooting

### `depviz` is not found after install

Add the install directory to your shell profile:

```bash
export PATH="$HOME/.depviz/bin:$PATH"
```

### Maven cannot resolve the plugin

Use the fully qualified coordinate:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open
```

If your company uses a Maven mirror, make sure the Depviz artifact is available through that mirror. You can also try:

```bash
depviz open --refresh-dependencies
```

### The browser did not open

The files may still have been generated. Open the HTML manually:

```text
target/depviz/dependency-graph.html
```

Or generate without browser opening:

```bash
depviz open --no-browser
```

### The graph is too large or generation is slow

Start with a narrower or faster run:

```bash
depviz open --scope compile --no-updates --no-snyk
```

For Maven, include or exclude dependency families:

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.includes=org.springframework:*
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0:open -Ddepviz.excludes=junit:*,org.junit:*,org.mockito:*
```

### Snyk is unavailable

Disable Snyk:

```bash
depviz open --no-snyk
```

Or generate a report separately and pass it to Depviz:

```bash
depviz open --snyk-json target/snyk.json
```

## Security And Privacy

Depviz is designed for local development. The generated viewer is static and offline, but the generated files may contain project coordinates, dependency coordinates, versions, scopes, module names, diagnostics, update metadata, vulnerability titles, severities, advisory links, and the analyzed project path.

Treat `dependency-graph.html` and `dependency-graph.json` as project metadata. Do not publish generated output unless that metadata is safe to share.

## Build From Source

Build and test everything:

```bash
mvn -q -DskipTests=false install
```

Build the viewer directly:

```bash
cd viewer
npm test -- --run
npm run build
```

The project is licensed under the Apache License, Version 2.0.
