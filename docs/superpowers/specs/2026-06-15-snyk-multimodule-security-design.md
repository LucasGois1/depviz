# Depviz Snyk Security And Multi-Module Aggregate Design

## Objective

Add Snyk vulnerability awareness to Depviz without weakening the core graph model.

Before Snyk is implemented, Depviz must close the existing multi-module gap: the original design says reactor roots should produce an aggregate graph, but the current implementation only extracts the current `MavenProject`. Snyk alerts are only useful if they map to the same dependency universe the viewer shows, so this phase includes aggregate multi-module graph generation as a prerequisite.

The final result should let a developer run `mvn depviz:open` from a local Maven reactor, get a super-root + module graph, and see Snyk vulnerability badges, filters, summaries, and details in the static viewer.

## Product Decisions

- Use approach A: implement real aggregate multi-module graph generation first, then map Snyk onto that graph.
- `open` should behave as a Maven aggregator goal and generate one viewer for the reactor invocation.
- Aggregate graphs use a virtual super-root connected to module roots.
- Shared dependencies remain single graph nodes. If multiple modules depend on the same resolved artifact, they all point to that same dependency node.
- Snyk runs opportunistically by default: `depviz.snyk=auto`.
- If Snyk is unavailable or fails, generation continues and a lightweight diagnostic explains what happened.
- Snyk findings never fail `mvn depviz:open`; Depviz is an exploratory local viewer, not a CI security gate.
- Automatic execution and imported JSON are both supported. Imported JSON is useful for debugging and reproducible fixtures.
- The MVP renders open source dependency vulnerabilities only. The model should remain extensible for future signals such as licenses.
- Mapping is conservative. Only high-confidence package+version matches mark graph nodes.
- Unmapped Snyk findings are reported as diagnostics instead of being guessed onto nodes.
- Security badges and version badges can coexist on a node. Security appears first, version second.
- All severities are in scope: critical, high, medium, low.

## Multi-Module Aggregate Graph

When the goal runs in a Maven reactor root, Depviz should generate one aggregate graph from `reactorProjects`.

The aggregate graph shape is:

```text
reactor super-root
  -> module A root
      -> module A dependencies
  -> module B root
      -> module B dependencies
```

The super-root is virtual. It is not a Maven artifact and should be visually distinct from normal dependencies. It provides a single connected origin for layout, path filtering, and summary behavior.

Each Maven module becomes a module-root node. Module nodes should carry Maven coordinate fields when available and should be marked with `moduleRoot=true`.

Dependency nodes remain deduplicated by resolved coordinate:

```text
groupId:artifactId:type:classifier:version
```

If two modules depend on the same resolved artifact, the graph contains one dependency node and multiple incoming paths. This preserves graph truth and makes shared dependencies visually clear.

The set of modules in the aggregate graph comes from the current Maven reactor execution. This means `-pl`, `-am`, and other Maven reactor selection behavior are respected by Maven before Depviz sees the project list.

When the goal runs inside a single non-reactor module, Depviz still generates a single-module graph.

## Snyk Configuration

Add these plugin parameters:

| Parameter | Values | Default | Behavior |
| --- | --- | --- | --- |
| `depviz.snyk` | `auto`, `true`, `false` | `auto` | Controls Snyk enrichment. |
| `depviz.snykJson` | file path | none | Imports an existing Snyk JSON report. Takes precedence over CLI execution. |
| `depviz.snykCommand` | command/path | `snyk` | Overrides the Snyk executable used for CLI execution. |
| `depviz.snykOrg` | string | none | Passes organization to Snyk via `--org`. |
| `depviz.snykAllProjects` | `true`, `false` | `false` | Adds Snyk all-projects behavior for broader multi-module scans. |

Mode semantics:

- `auto`: run Snyk only when an executable is available. Missing CLI is a warning diagnostic, not a failure.
- `true`: attempt Snyk execution. Missing CLI, auth, or operational failures produce explicit diagnostics, but the Maven goal still succeeds.
- `false`: skip Snyk entirely and omit security badges, filters, and Snyk diagnostics. This explicit disable mode wins even if `depviz.snykJson` is also present.
- `snykJson`: when Snyk is not disabled, parse the given JSON file instead of executing the CLI. Invalid or unreadable JSON becomes a diagnostic.

The user has confirmed that Snyk CLI is already installed and logged in locally. That should be used for manual validation, but automated tests must not depend on live Snyk authentication or network access.

## Snyk Execution

When executing the CLI, Depviz should prefer writing JSON to a temporary file using Snyk's JSON file output behavior rather than relying only on stdout. This keeps parsing separate from terminal logs and stderr.

The command should be built as structured process arguments, not shell-concatenated strings. `depviz.snykCommand` identifies the executable or command path; Depviz should avoid free-form shell argument parsing in the MVP.

Conceptual command:

```text
snyk test --json-file-output <temp-file>
```

Additional flags:

- Add `--org <org>` when `depviz.snykOrg` is set.
- Add Snyk all-projects behavior when `depviz.snykAllProjects=true`.

Snyk may return a non-zero exit code when vulnerabilities are found. Depviz must treat that as a successful Snyk scan if a JSON report was produced and can be parsed.

Operational failures include missing command, auth/token errors, unreadable output, malformed JSON, unsupported report shape, timeouts, and unexpected process errors. These become diagnostics and do not prevent viewer generation.

The generated viewer remains static and offline. Snyk calls happen only during Maven generation. The HTML/JS viewer must not call Snyk APIs or external URLs at runtime.

## Security Data Model

Add an optional `securitySummary` to `GraphDocument`.

Example:

```json
"securitySummary": {
  "enabled": true,
  "source": "snyk",
  "checked": true,
  "vulnerableNodes": 8,
  "affectedModules": 3,
  "critical": 1,
  "high": 6,
  "medium": 12,
  "low": 4,
  "unmappedFindings": 2
}
```

Add optional `securityInsight` to graph nodes.

Example for a dependency node:

```json
"securityInsight": {
  "status": "vulnerable",
  "maxSeverity": "high",
  "vulnerabilityCount": 3,
  "critical": 0,
  "high": 2,
  "medium": 1,
  "low": 0,
  "source": "snyk",
  "findings": [
    {
      "id": "SNYK-JAVA-EXAMPLE-123",
      "severity": "high",
      "title": "Example vulnerability",
      "packageName": "org.example:lib",
      "version": "1.2.3",
      "fixedVersions": ["1.2.8"],
      "url": "https://security.snyk.io/vuln/SNYK-JAVA-EXAMPLE-123"
    }
  ]
}
```

Security status values:

- `not-vulnerable`
- `vulnerable`
- `unavailable`
- `unchecked`

Severity values:

- `critical`
- `high`
- `medium`
- `low`

The model should allow future `source` values and finding categories, but the MVP renders Snyk open source vulnerabilities only.

## Snyk Mapping Rules

The parser should normalize Snyk package identifiers into Maven-style keys where possible:

```text
groupId:artifactId
groupId:artifactId:version
```

Depviz should mark a dependency node only when it can match both:

- package identity: `groupId:artifactId`
- resolved version: node version equals the vulnerable package version from the Snyk report

If the report shape does not provide a reliable package version, do not mark graph nodes by package name alone in the MVP.

Unmapped findings are counted in `securitySummary.unmappedFindings` and added to diagnostics. Diagnostics should include enough information for the developer to understand that Snyk found issues Depviz could not confidently place on the graph.

Module security insights are derived from graph paths. A module is affected when it has a path to a vulnerable dependency node. The module node receives an aggregate `securityInsight` with the maximum severity and counts across vulnerable dependencies reachable from that module.

The super-root receives aggregate summary information but should not behave like a normal vulnerable dependency node. It should not dominate the canvas with an alert badge.

## Viewer UX

### Canvas

Nodes may show up to two compact badges:

1. Security badge
2. Version badge

Security badge text:

- `C`: critical
- `H`: high
- `M`: medium
- `L`: low

Version badges keep their current behavior. Because `M` can mean medium severity or major update, the badge style and placement must distinguish security from version. Security should appear first and use severity colors; version should appear second and keep update colors.

Dependency nodes show security badges when directly vulnerable.

Module nodes show security badges when affected by reachable vulnerable dependencies.

The super-root can show aggregate counts in the sidebar, but should not become a large visual alert in the graph.

### Toolbar

Add a security filter control:

- `All`
- `Vulnerable`
- `Critical`
- `High`
- `Medium`
- `Low`

The security filter composes with search, scope, optional, collapse, and update filters.

Filtering by severity preserves graph context. If a transitive dependency matches, the visible graph includes the super-root, affected module, and path parents needed to explain how the dependency is reached.

### Sidebar

For dependency nodes, show:

- max severity
- vulnerability count
- severity counts
- individual findings with ID, severity, title, package/version, fixed versions, and URL
- affected paths already available in the path panel

For module nodes, show two readings:

- `Vulnerabilities`: grouped by vulnerability, listing affected dependencies and paths.
- `Affected modules`: for aggregate views, module impact summaries. When the selected node is a module, this can focus on that module's affected dependencies.

For super-root, show aggregate security summary:

- vulnerable dependency count
- affected module count
- severity counts
- unmapped finding count
- Snyk execution status

For documents without `securitySummary` or `securityInsight`, the viewer must behave like today. No security controls should appear for old JSON.

## Diagnostics

Snyk diagnostics should use the existing diagnostics panel and should not pollute the graph.

Diagnostic cases:

- `snyk-unavailable`: CLI not found in `auto` or `true`.
- `snyk-auth-failed`: authentication/token problem.
- `snyk-scan-failed`: operational CLI failure.
- `snyk-json-invalid`: imported or generated JSON could not be parsed.
- `snyk-unmapped-findings`: findings were present but not confidently mapped to graph nodes.

Diagnostic severity should usually be `warning`. Use `error` only when the requested Snyk input is explicitly invalid, such as an unreadable `depviz.snykJson` file.

Vulnerabilities themselves are not diagnostics. They are first-class security findings in node insights and summaries.

## Error Handling

The Maven goal fails for invalid Depviz configuration values, such as an invalid `depviz.snyk` mode or invalid boolean syntax.

The Maven goal does not fail for:

- Snyk CLI missing
- Snyk not authenticated
- Snyk operational errors
- Snyk findings
- unmapped Snyk findings
- malformed Snyk JSON from an external file

Those situations produce diagnostics and still write `dependency-graph.html` and `dependency-graph.json`.

If multi-module extraction fails for a module, the existing dependency graph failure behavior should remain explicit and actionable. Multi-module extraction errors are core graph-generation errors, not optional Snyk diagnostics.

## Privacy And Security Notes

Snyk CLI may contact Snyk services during generation and may use local authentication state or `SNYK_TOKEN`. Depviz should document that Snyk enrichment is a generation-time network feature, unlike the static viewer.

The generated JSON may contain:

- Maven project/module coordinates
- dependency coordinates and versions
- Snyk vulnerability IDs and titles
- fixed versions
- Snyk URLs
- Snyk diagnostics
- local project base directory metadata already present in Depviz

The viewer remains offline after generation, but the output files should still be treated as project/security metadata and should not be published casually.

## Testing Strategy

### Java Unit Tests

Config tests:

- default `depviz.snyk=auto`
- `auto`, `true`, `false`
- invalid mode fails fast
- `depviz.snykJson`
- `depviz.snykCommand`
- `depviz.snykOrg`
- `depviz.snykAllProjects`

Graph tests:

- aggregate reactor creates a virtual super-root
- modules are direct children of the super-root
- module nodes are marked as `moduleRoot`
- shared dependencies are deduplicated and receive multiple incoming edges
- paths from module roots to shared dependencies are preserved
- single-module behavior still works

Snyk parser tests:

- parse fixture with critical/high/medium/low findings
- parse both a single Snyk project report and an aggregate/multi-project report shape
- parse fixed versions and URL when present
- tolerate missing optional fields
- ignore unsupported categories in the MVP
- report malformed JSON as diagnostic

Mapping tests:

- exact package+version maps to dependency node
- package without confident version does not map
- unmatched finding increments `unmappedFindings`
- vulnerable dependency aggregates into affected module insight
- super-root summary aggregates without becoming a vulnerable dependency node

### Viewer Tests

- security summary stats render when present
- security controls hide when `securitySummary` is absent
- security filter preserves graph context
- critical/high/medium/low filters compose with existing filters
- labels can draw both security and version badges
- details panel renders dependency findings
- module details render aggregate security impact
- old JSON without security fields remains compatible

### Integration Tests

- Maven Invoker multi-module fixture validates super-root, module roots, shared dependency deduplication, and path preservation.
- Maven Invoker Snyk fixture uses `depviz.snykJson` with a small committed JSON report.
- Invoker tests must not require live Snyk CLI, authentication, Snyk token, or network access.
- Manual validation can use the user's installed and authenticated Snyk CLI with `depviz.snyk=auto`.

## Manual Validation Plan

After implementation:

1. Run a multi-module fixture and inspect generated JSON for super-root, module nodes, and shared dependency edges.
2. Run with `-Ddepviz.snykJson=<fixture>` and confirm security summary, node insights, filters, badges, and details.
3. Run a real local project with Snyk CLI logged in and `-Ddepviz.snyk=auto`.
4. Confirm that vulnerability exit codes do not fail `mvn depviz:open`.
5. Open the generated viewer from `file://` and confirm no runtime Snyk/network calls are required.

## References

- Snyk CLI `test` command documentation: https://docs.snyk.io/developer-tools/snyk-cli/snyk-cli/commands/test
