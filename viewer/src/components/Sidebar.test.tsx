import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { securitySummaryStats, Sidebar, versionSummaryStats } from "./Sidebar";
import type { Adjacency, DepvizDocument, FilterState, SecuritySummary, VersionSummary } from "../types";

describe("versionSummaryStats", () => {
  it("includes the unknown update counter in compact summary stats", () => {
    const summary: VersionSummary = {
      enabled: true,
      checked: 12,
      current: 4,
      outdated: 6,
      patch: 1,
      minor: 2,
      major: 3,
      unknown: 5,
      unavailable: 2
    };

    expect(versionSummaryStats(summary)).toEqual([
      { label: "Outdated", value: 6 },
      { label: "Major", value: 3 },
      { label: "Minor", value: 2 },
      { label: "Patch", value: 1 },
      { label: "Unknown", value: 5 },
      { label: "Unavailable", value: 2 }
    ]);
  });
});

describe("securitySummaryStats", () => {
  it("includes vulnerable node, severity, module, and unmapped counters", () => {
    const summary: SecuritySummary = {
      enabled: true,
      source: "snyk",
      checked: true,
      vulnerableNodes: 4,
      affectedModules: 2,
      critical: 1,
      high: 2,
      medium: 3,
      low: 4,
      unmappedFindings: 5
    };

    expect(securitySummaryStats(summary)).toEqual([
      { label: "Vulnerable", value: 4 },
      { label: "Modules", value: 2 },
      { label: "Critical", value: 1 },
      { label: "High", value: 2 },
      { label: "Medium", value: 3 },
      { label: "Low", value: 4 },
      { label: "Unmapped", value: 5 }
    ]);
  });
});

describe("Sidebar", () => {
  it("renders security summary stats when security reporting is enabled", () => {
    const html = renderToStaticMarkup(
      <Sidebar
        document={documentWithSecuritySummary()}
        adjacency={emptyAdjacency()}
        nodeById={new Map()}
        selectedNode={null}
        filters={emptyFilters()}
        visibility={{ visibleNodeIds: new Set(), visibleEdgeIds: new Set(), matchingNodeIds: new Set() }}
        onSelectNode={() => undefined}
        onToggleCollapse={() => undefined}
      />
    );

    expect(html).toContain('aria-label="Security summary"');
    expect(html).toContain("Vulnerable");
    expect(html).toContain("Critical");
    expect(html).toContain("Unmapped");
    expect(html.indexOf('aria-label="Version summary"')).toBeLessThan(html.indexOf('aria-label="Security summary"'));
  });

  it("does not render security summary stats when security reporting is disabled", () => {
    const document = documentWithSecuritySummary();
    const html = renderToStaticMarkup(
      <Sidebar
        document={{ ...document, securitySummary: { ...document.securitySummary!, enabled: false } }}
        adjacency={emptyAdjacency()}
        nodeById={new Map()}
        selectedNode={null}
        filters={emptyFilters()}
        visibility={{ visibleNodeIds: new Set(), visibleEdgeIds: new Set(), matchingNodeIds: new Set() }}
        onSelectNode={() => undefined}
        onToggleCollapse={() => undefined}
      />
    );

    expect(html).not.toContain('aria-label="Security summary"');
  });
});

function documentWithSecuritySummary(): DepvizDocument {
  return {
    schemaVersion: "1.0",
    generatedAt: "2026-06-15T00:00:00Z",
    project: {
      groupId: "org.example",
      artifactId: "demo",
      version: "1.0.0",
      packaging: "jar",
      name: "Demo",
      baseDirectory: "/repo",
      multiModule: false,
      modules: []
    },
    summary: {
      nodeCount: 0,
      edgeCount: 0,
      nodesByScope: {},
      nodesByGroupId: {}
    },
    versionSummary: {
      enabled: true,
      checked: 0,
      current: 0,
      outdated: 0,
      patch: 0,
      minor: 0,
      major: 0,
      unknown: 0,
      unavailable: 0
    },
    securitySummary: {
      enabled: true,
      source: "snyk",
      checked: true,
      vulnerableNodes: 4,
      affectedModules: 2,
      critical: 1,
      high: 2,
      medium: 3,
      low: 4,
      unmappedFindings: 5
    },
    viewerConfig: { initialLayout: "force", maxInitialLabels: 500, nodeMode: "artifact" },
    nodes: [],
    edges: [],
    paths: [],
    diagnostics: []
  };
}

function emptyAdjacency(): Adjacency {
  return {
    parents: new Map(),
    children: new Map(),
    incomingEdges: new Map(),
    outgoingEdges: new Map()
  };
}

function emptyFilters(): FilterState {
  return {
    search: "",
    scopes: new Set(),
    optionalMode: "all",
    updateMode: "all",
    securityMode: "all",
    collapsedNodeIds: new Set()
  };
}
