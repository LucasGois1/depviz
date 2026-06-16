import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { DetailsPanel, versionStatusText, versionUpdateText } from "./DetailsPanel";
import type { Adjacency, FilterState, GraphNode, SecurityInsight, VersionInsight } from "../types";

describe("version display helpers", () => {
  it("shows unavailable status separately from the actual update type", () => {
    const insight: VersionInsight = {
      currentVersion: "1.0.0",
      latestVersion: null,
      updateType: "unknown",
      status: "unavailable",
      checked: true,
      message: "Repository lookup failed"
    };

    expect(versionStatusText(insight)).toBe("unavailable");
    expect(versionUpdateText(insight)).toBe("unknown");
  });
});

describe("DetailsPanel", () => {
  it("renders selected node version insight fields and unavailable message", () => {
    const selectedNode = nodeWithUnavailableInsight();
    const html = renderToStaticMarkup(
      <DetailsPanel
        adjacency={emptyAdjacency()}
        nodeById={new Map([[selectedNode.id, selectedNode]])}
        selectedNode={selectedNode}
        filters={emptyFilters()}
        onSelectNode={() => undefined}
        onToggleCollapse={() => undefined}
      />
    );

    expect(html).toContain("Current");
    expect(html).toContain("1.0.0");
    expect(html).toContain("Latest stable");
    expect(html).toContain("Status");
    expect(html).toContain("unavailable");
    expect(html).toContain("Update");
    expect(html).toContain("unknown");
    expect(html).toContain("Repository lookup failed");
  });

  it("renders selected node security insight after version details", () => {
    const selectedNode = {
      ...nodeWithUnavailableInsight(),
      securityInsight: vulnerableInsight()
    };
    const html = renderToStaticMarkup(
      <DetailsPanel
        adjacency={emptyAdjacency()}
        nodeById={new Map([[selectedNode.id, selectedNode]])}
        selectedNode={selectedNode}
        filters={emptyFilters()}
        onSelectNode={() => undefined}
        onToggleCollapse={() => undefined}
      />
    );

    expect(html.indexOf("<h3>Version</h3>")).toBeLessThan(html.indexOf("<h3>Security</h3>"));
    expect(html).toContain("Max severity");
    expect(html).toContain("high");
    expect(html).toContain("Findings");
    expect(html).toContain("Prototype pollution in example-lib");
    expect(html).toContain("SNYK-JAVA-EXAMPLELIB-123");
    expect(html).toContain("Package");
    expect(html).toContain("org.example:example-lib");
    expect(html).toContain("Fixed versions");
    expect(html).toContain("1.0.1, 1.1.0");
  });
});

function vulnerableInsight(): SecurityInsight {
  return {
    status: "vulnerable",
    maxSeverity: "high",
    vulnerabilityCount: 2,
    critical: 0,
    high: 1,
    medium: 1,
    low: 0,
    source: "snyk",
    findings: [
      {
        id: "SNYK-JAVA-EXAMPLELIB-123",
        severity: "high",
        title: "Prototype pollution in example-lib",
        packageName: "org.example:example-lib",
        version: "1.0.0",
        fixedVersions: ["1.0.1", "1.1.0"],
        url: "https://security.example/SNYK-JAVA-EXAMPLELIB-123"
      }
    ]
  };
}

function nodeWithUnavailableInsight(): GraphNode {
  return {
    id: "org.example:client:jar::1.0.0",
    groupId: "org.example",
    artifactId: "client",
    version: "1.0.0",
    type: "jar",
    classifier: "",
    scope: "compile",
    optional: false,
    depth: 1,
    root: false,
    moduleRoot: false,
    label: "org.example:client",
    coordinate: "org.example:client:jar::1.0.0",
    groupColorKey: "org.example",
    versionInsight: {
      currentVersion: "1.0.0",
      latestVersion: null,
      updateType: "unknown",
      status: "unavailable",
      checked: true,
      message: "Repository lookup failed"
    }
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
