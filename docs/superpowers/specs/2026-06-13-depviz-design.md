# Depviz Maven Dependency Graph Viewer Design

## Objective

Build a standalone Maven plugin that lets a developer run `mvn depviz:open` from a Maven project and open a local, offline, interactive dependency graph viewer.

The tool is for local developer exploration. It must not require an IDE, Graphviz, Node.js at plugin runtime, an external graph tool, a local server, a CDN, or internet access when viewing the generated graph.

## Baseline

- Java baseline: 17.
- Maven baseline: Maven 3.9+.
- Plugin coordinates: `dev.gois.tools:depviz-maven-plugin:0.1.0-SNAPSHOT`.
- Packaging: `maven-plugin`.
- Goal: `open`.
- Default command during local development:

```bash
mvn dev.gois.tools:depviz-maven-plugin:0.1.0-SNAPSHOT:open
```

Configured/published usage:

```bash
mvn depviz:open
```

## Product Scope

The first product version should be built around a robust graph core, not a thin MVP that later needs a model rewrite.

The first implementation should include P0 behavior plus these P1-oriented capabilities in the core/product design:

- multi-module aggregate mode
- path-to-root
- collapse/expand
- grouping/coloring by `groupId`
- diagnostics mode for conflicts/omissions/anomalies when Maven exposes trustworthy metadata

PNG/SVG export is out of scope for the first implementation.

## Architecture

`OpenMojo` should stay thin. It reads Maven/plugin parameters, validates configuration, calls dependency extraction, builds the graph document, writes viewer output, and attempts to open the browser.

The system is split into three layers:

1. **Extraction**
   - Uses Maven APIs to obtain the effective resolved dependency graph.
   - Does not parse human-readable `mvn dependency:tree` text.
   - Detects reactor context for multi-module behavior.

2. **Graph core**
   - Converts Maven data into a Maven-independent graph model.
   - Uses an artifact-deduplicated graph as the main product model.
   - Preserves occurrence/path metadata internally for path-to-root, collapse/expand, and diagnostics.

3. **Output and viewer**
   - Writes `dependency-graph.json`.
   - Copies static viewer assets from the plugin JAR into `target/depviz/assets/`.
   - Generates `dependency-graph.html`.
   - Opens the generated HTML with a local `file://` URL when requested.

## Graph Model

The visual graph is artifact-deduplicated. If several packages depend on the same resolved artifact, the graph contains one node for that artifact and multiple incoming edges.

Edge direction means:

```text
parent dependency -> child dependency
A depends on B => A -> B
```

The graph model should include:

- `GraphDocument`: schema version, generation time, project info, summary, viewer config, nodes, edges, paths, diagnostics.
- `GraphNode`: stable ID, Maven coordinate fields, scope, optional flag, minimum depth, root/module flags, label, display coordinate, group color key.
- `GraphEdge`: stable ID, source, target, scope, optional flag, depth, origin/module metadata when available.
- Internal occurrence/path records: enough information to reconstruct all root-to-artifact paths without duplicating visual artifact nodes.
- `Diagnostics`: conflicts, omitted nodes, convergence notes, and path anomalies when Maven APIs expose them reliably.

Default node ID format:

```text
groupId:artifactId:type:classifier:version
```

Classifier uses an empty segment when absent.

## Multi-Module Behavior

When `mvn depviz:open` runs at the root of a Maven reactor with modules, the plugin generates an aggregate graph automatically.

When the goal runs inside a module, it generates a graph for that module only.

Aggregate graphs use a virtual super-root node that points to module roots. This gives a single layout origin and makes path-to-root behavior clear while preserving each module as its own entry point.

## Configuration Behavior

Defaults:

- `depviz.scope=runtime`
- `depviz.open=true`
- `depviz.layout=breadthfirst`
- `depviz.nodeMode=artifact`
- `depviz.maxInitialLabels=500`
- `depviz.outputDirectory=${project.build.directory}/depviz`

Accepted initial layouts:

- `breadthfirst`
- `force`
- `circle`
- `concentric`

`runtime` is the default scope because it best represents the dependency graph a developer usually needs to reason about for an application in use. Other scopes remain available for targeted investigation.

`depviz.includes` and `depviz.excludes` operate as subtree/path filters:

- Includes preserve the paths necessary to reach matching dependencies.
- Excludes remove the excluded dependency and graph portions that only exist through that dependency.
- Shared nodes remain visible when still reachable through another visible path.

`nodeMode=artifact` is the product default. Occurrence data is kept internally, but occurrence-mode visualization is not the primary first-version UI.

## Browser Opening

The plugin is for local developer use, not CI-first operation.

When `depviz.open=true`, the plugin attempts to open the generated HTML in the default browser. If browser opening fails after HTML/JSON generation succeeds, the Maven goal should not fail. It should log a warning and print the file path plus `file://` URI.

When `depviz.open=false`, the plugin writes all files and prints the generated HTML path/URI without trying to open a browser.

## Viewer Implementation

The viewer source should be built with:

- React
- Vite
- shadcn/ui
- Cytoscape.js

The Maven plugin runtime must not require Node.js. The plugin project build compiles the React viewer into static assets that are packaged in the plugin JAR. At `depviz:open` runtime, the plugin only copies those already-built assets.

The generated output structure should be:

```text
target/depviz/
  dependency-graph.html
  dependency-graph.json
  assets/
    app.js
    style.css
    cytoscape.min.js or bundled equivalent
    LICENSES.txt
```

The viewer must run from `file://` with no network calls and no CDN references.

## Viewer UX

The UI uses a large graph canvas on the left and a sidebar on the right.

The canvas supports:

- pan
- zoom
- node dragging
- node selection
- selected-node highlighting
- immediate-neighborhood highlighting
- dimming unrelated nodes and edges
- fit view
- reset view
- layout switching

The sidebar supports:

- graph summary: node count, edge count, scopes, top `groupId`s
- search by coordinate, node ID, `groupId`, `artifactId`, `version`, scope, type, and classifier
- scope filter
- optional dependency filter
- label visibility control
- layout selector
- selected node details
- immediate parents: "Depended on by"
- immediate children: "Depends on"
- copy coordinate action
- full path-to-root display with pagination or progressive expansion
- diagnostics section separate from the main details view
- collapse/expand controls

Default coloring is by primary `groupId`. Scope remains available through filters and labels rather than being the default visual color dimension.

The UI should feel like a polished developer tool, not a bare generated HTML report. shadcn/ui should be used to provide strong interaction states, accessible controls, visual consistency, and a high-quality sidebar experience.

## Collapse And Path Semantics

Collapse/expand must preserve graph truth.

Collapsing a branch hides only the graph portion that becomes unreachable because of that collapse. A shared artifact remains visible if it is still reachable through another visible parent.

Path-to-root should not discard paths. If a selected artifact has many paths, the sidebar should show them with pagination or progressive expansion.

## Diagnostics Mode

The main view shows the effective resolved dependency graph Maven uses.

Diagnostics is a separate section/mode. It may show conflicts, omitted dependencies, convergence issues, and anomalies when the extraction layer can obtain reliable Maven metadata.

If some diagnostic metadata is unavailable in Maven APIs for a project, the viewer should say that clearly rather than inventing uncertain data.

## Error Handling

Failures should be short and actionable.

The goal should fail for:

- invalid `depviz.scope`
- invalid `depviz.layout`
- invalid `depviz.nodeMode`
- invalid include/exclude pattern
- dependency resolution failure
- output directory creation failure
- template/resource loading failure
- JSON serialization failure
- viewer asset writing failure

The goal should not fail solely because the browser could not be opened after output was successfully generated.

## Security And Privacy

The plugin must not upload dependency data anywhere.

The viewer must not call external runtime resources.

Dependency data must be rendered as JSON/text, not inserted as raw HTML. Embedded JSON must be escaped safely so dependency coordinates cannot close a script tag.

The plugin must not modify the analyzed project's `pom.xml` or source files.

Bundled third-party JavaScript must include license text.

## Testing Strategy

Use TDD where practical.

Java unit tests should cover:

- config parsing and validation
- coordinate ID and display generation
- include/exclude pattern matching
- scope filtering
- graph root creation
- artifact deduplication
- multiple incoming edges
- duplicate edge prevention
- shortest depth calculation
- path-to-root construction
- collapse reachability
- summary generation
- JSON writing
- safe JSON embedding
- viewer file writing
- browser opener fallback behavior

Viewer tests should cover search/filter/path/collapse helper behavior where feasible without making the build brittle.

The Maven build should include Maven Invoker Plugin tests that run the plugin against at least one sample Maven project and verify:

- `dependency-graph.html` is generated
- `dependency-graph.json` is generated
- JSON contains the project root and at least one dependency
- HTML contains the viewer shell
- generated HTML/assets do not reference CDN URLs
- `-Ddepviz.open=false` avoids browser opening
- invalid scope fails clearly

Manual verification before declaring completion should include:

```bash
mvn -q -DskipTests=false test
mvn -q -DskipTests=false install
mvn dev.gois.tools:depviz-maven-plugin:0.1.0-SNAPSHOT:open -Ddepviz.open=false
```

Then open the generated HTML and verify pan, zoom, drag, search, filters, node details, neighborhood highlighting, layout switch, path-to-root, collapse behavior, and offline operation.

## Known First-Version Non-Goals

- Java source/package/class dependency analysis.
- Modifying dependency versions.
- Modifying analyzed project POMs.
- Uploading graph data.
- Requiring IDEs or external graph tools.
- PNG/SVG export.
- Local server mode.
- Live reload.
- Branch-to-branch dependency diff.
