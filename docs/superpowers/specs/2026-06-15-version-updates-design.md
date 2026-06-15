# Depviz Version Update Badges Design

## Objective

Add version update awareness to Depviz so the generated dependency graph shows which resolved Maven dependencies have newer stable versions available.

The first version of this feature keeps the graph as the primary surface. Updates appear as badges on dependency nodes, with details in the sidebar and filters in the toolbar.

## Product Decisions

- The UI direction is graph-first with badges.
- Update checking is enabled by default.
- Update data is resolved through the local Maven project context, not Maven Central HTTP calls.
- All dependencies in the final generated graph are checked, including transitive dependencies.
- Only stable releases are considered for recommendations.
- Update severity is classified as `patch`, `minor`, `major`, or `unknown`.
- Version check failures do not fail the Maven goal. The viewer still opens and shows diagnostics.
- The toolbar includes an update filter in the MVP.

## User Experience

The graph remains the main view. A dependency node can show a compact badge:

- `P` for patch update available.
- `m` for minor update available.
- `M` for major update available.
- `?` for update available, but type cannot be classified confidently.

Badge colors should communicate risk without overwhelming the graph:

- Patch: low-risk positive/blue or green tone.
- Minor: warning/amber tone.
- Major: stronger warning/red-orange tone.
- Unknown: neutral warning tone.

The toolbar gains an update filter. The first implementation should support:

- `All`: current behavior.
- `Outdated`: only nodes with an available update, plus graph context needed to understand paths.
- `Patch`
- `Minor`
- `Major`
- `Unknown`

The filter must preserve graph truth. When filtering to outdated nodes, visible context should include the root/path parents needed to understand how a transitive dependency is reached. Shared dependencies must remain shared nodes with multiple incoming edges.

The selected node sidebar adds a version section when update data exists:

- Current version.
- Latest stable version found.
- Update type.
- Status: current, outdated, unavailable, or unchecked.
- Whether the dependency is direct or transitive, inferred from graph depth/path context.
- Existing paths remain the source of truth for how the dependency is reached.

The graph summary area should include update counters:

- Total checked.
- Outdated.
- Patch.
- Minor.
- Major.
- Unknown.
- Unavailable, when checks fail for some artifacts.

## Configuration

Add:

```text
depviz.checkUpdates=true|false
```

Default: `true`.

Rationale: the product should be useful immediately for local development. A developer who is offline, behind a broken repository mirror, or working in a slow repository setup can disable checks explicitly:

```bash
mvn depviz:open -Ddepviz.checkUpdates=false
```

When disabled, the viewer renders exactly as it does today, without version badges, version summary, or update filters with active counts.

## Architecture

`OpenMojo` remains thin. It should parse the new config flag, extract the dependency graph, apply graph filters, run update checking, build the document, write files, and open the browser.

Recommended flow:

```text
OpenMojo
  -> MavenDependencyGraphExtractor
  -> GraphFilters
  -> VersionUpdateChecker
  -> GraphDocumentBuilder
  -> ViewerWriter
```

The update checker should operate on the dependencies that survive `scope`, `includes`, and `excludes`. This avoids spending time checking artifacts that will not appear in the viewer.

The viewer remains static and offline at runtime. It must not call Maven Central, repository URLs, Snyk, or any external service. It only renders the version data embedded in `dependency-graph.json` and `dependency-graph.html`.

## Maven Resolution Strategy

The `VersionUpdateChecker` should use Maven's local project context to discover available versions. It must respect the analyzed project's effective repository configuration, including:

- `settings.xml`
- mirrors
- authentication
- corporate repositories
- project repositories
- repository policies
- local repository cache

The checker should not parse human-readable output from `mvn versions:display-dependency-updates`. It should use Maven APIs so results can be mapped reliably to graph nodes.

The checker should collect unique artifact coordinates from the final graph. For each coordinate, it checks available versions for the same `groupId`, `artifactId`, `type`, and classifier where relevant. It compares the current resolved version against the newest stable version Maven can resolve.

If an artifact's versions cannot be resolved, the checker records an unavailable result for that artifact and adds a diagnostic. It should continue checking the remaining artifacts.

## Data Model

Add a version insight model to graph nodes:

```json
"versionInsight": {
  "currentVersion": "6.1.14",
  "latestVersion": "6.1.16",
  "updateType": "patch",
  "status": "outdated",
  "checked": true,
  "message": null
}
```

Status values:

- `current`: checked and no newer stable version found.
- `outdated`: checked and a newer stable version found.
- `unavailable`: attempted but failed for this artifact.
- `unchecked`: checks disabled or skipped.

Update type values:

- `patch`
- `minor`
- `major`
- `unknown`
- `none`

Add a version summary to the graph document:

```json
"versionSummary": {
  "enabled": true,
  "checked": 16,
  "current": 9,
  "outdated": 7,
  "patch": 4,
  "minor": 2,
  "major": 1,
  "unknown": 0,
  "unavailable": 0
}
```

The viewer must tolerate missing `versionInsight` and `versionSummary` for backwards compatibility with older JSON fixtures or external consumers.

## Stable Version Rules

The MVP only recommends stable releases.

Ignore versions containing prerelease markers such as:

- `SNAPSHOT`
- `alpha`
- `beta`
- `rc`
- `cr`
- `m`
- `milestone`
- `preview`
- `ea`

Matching should be case-insensitive. The implementation should avoid classifying ordinary stable versions as prerelease solely because a group or artifact name contains these strings. The filtering applies to version strings only.

## Version Classification Rules

Classification is best-effort and Maven-friendly, not strict SemVer.

Rules:

- Do not suggest downgrades.
- Extract leading numeric components from both current and latest versions.
- Compare numeric components from left to right.
- If major increases, classify as `major`.
- If major is the same and minor increases, classify as `minor`.
- If major and minor are the same and patch increases, classify as `patch`.
- If a newer stable version exists but confident classification is not possible, classify as `unknown`.

Examples:

| Current | Latest | Type |
| --- | --- | --- |
| `6.1.14` | `6.1.16` | `patch` |
| `2.17.2` | `2.18.3` | `minor` |
| `1.12.11` | `2.0.0` | `major` |
| `20240205` | `20250601` | `unknown` |

## Error Handling

Version checking must not block graph generation.

If the entire checker fails, the document is still written with:

- `versionSummary.enabled=true`
- `checked=0`
- `unavailable` reflecting skipped artifacts when available
- a global diagnostic explaining that version checks were unavailable

If a single artifact fails, only that artifact is marked `unavailable`, and the checker continues.

Invalid values for `depviz.checkUpdates` should fail fast during config parsing, matching the existing behavior for invalid booleans and enums.

## Viewer Behavior

Graph rendering:

- Nodes with `status=outdated` show update badges.
- Current nodes show no badge by default.
- Unavailable nodes do not get a badge unless the diagnostics view needs to call attention to them.
- Badge rendering should not resize or shift the graph layout.

Toolbar:

- Add an update filter control near existing scope/optional filters.
- The filter composes with search, scope, optional, and collapse filters.
- `Outdated` should preserve path context to visible outdated nodes.

Sidebar:

- Add a version section above or near dependency properties.
- For transitive dependencies, do not invent a direct upgrade instruction in the MVP. Use existing paths to show how the dependency is reached.
- If a future version can infer direct upgrade candidates reliably, that belongs in a later "upgrade path" feature.

Diagnostics:

- Global version-check failures appear in the existing Diagnostics tab.
- Per-node unavailable checks appear when the node is selected.

## Testing Strategy

Java tests:

- Config parsing for `depviz.checkUpdates` default, true, false, and invalid values.
- Stable version filtering.
- Version comparison and classification.
- Version summary aggregation.
- Graph document serialization with version insights.
- Non-blocking checker failures generating diagnostics.

Integration tests:

- A fixture project where known dependencies have mocked or controlled available versions.
- Verify generated JSON contains `versionSummary` and node `versionInsight` values.
- Verify `-Ddepviz.checkUpdates=false` omits active update data and still renders.
- Avoid tests that depend on live public repository state.

Viewer tests:

- Badges render for patch, minor, major, and unknown updates.
- Update filter composes with existing visibility logic.
- Sidebar displays version insight for selected outdated nodes.
- Viewer handles missing version fields without crashing.

## Out Of Scope

- Snyk integration.
- CVE/vulnerability display.
- Automatic `pom.xml` edits.
- Upgrade path recommendation for direct dependency changes.
- Maven Central HTTP fallback.
- Pre-release opt-in.
- CI report mode.
- Exporting update reports as markdown, SARIF, or HTML separate from the viewer.

## Implementation Decisions

- Use Maven Resolver through Maven's injected repository system APIs to request available versions for `groupId:artifactId`.
- Treat versions as artifact-level metadata. The checker maps results back to graph nodes with the same `groupId` and `artifactId`; `type` and classifier remain part of the graph node identity but do not require separate metadata queries in the MVP.
- Run version checks sequentially in the MVP. Bounded parallelism can be added later only after repository load and error behavior are understood.
