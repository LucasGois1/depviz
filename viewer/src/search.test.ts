import { describe, expect, it } from "vitest";
import { buildSearchSuggestions, nextSuggestionIndex } from "./search";
import type { DepvizDocument, GraphNode, VisibilityState } from "./types";

const document: DepvizDocument = {
  schemaVersion: "1.0",
  generatedAt: "2026-06-16T00:00:00Z",
  project: {
    groupId: "dev.example",
    artifactId: "demo",
    version: "1.0.0",
    packaging: "jar",
    name: "Demo",
    baseDirectory: "/repo",
    multiModule: false,
    modules: []
  },
  summary: {
    nodeCount: 5,
    edgeCount: 5,
    nodesByScope: { root: 1, compile: 3, runtime: 1 },
    nodesByGroupId: { "dev.example": 1, "com.fasterxml.jackson.core": 3, "org.consumer": 1 }
  },
  versionSummary: {
    enabled: true,
    checked: 4,
    current: 2,
    outdated: 1,
    patch: 0,
    minor: 1,
    major: 0,
    unknown: 0,
    unavailable: 0
  },
  securitySummary: {
    enabled: true,
    source: "snyk",
    checked: true,
    vulnerableNodes: 1,
    affectedModules: 1,
    critical: 1,
    high: 0,
    medium: 0,
    low: 0,
    unmappedFindings: 0
  },
  viewerConfig: { initialLayout: "force", maxInitialLabels: 500, nodeMode: "artifact" },
  nodes: [
    node("dev.example:demo:jar::1.0.0", "dev.example", "demo", "1.0.0", "root", 0, true),
    {
      ...node("com.fasterxml.jackson.core:jackson-databind:jar::2.15.4", "com.fasterxml.jackson.core", "jackson-databind", "2.15.4", "compile", 1),
      versionInsight: {
        currentVersion: "2.15.4",
        latestVersion: "2.16.1",
        updateType: "minor",
        status: "outdated",
        checked: true,
        message: null
      },
      securityInsight: {
        status: "vulnerable",
        maxSeverity: "high",
        vulnerabilityCount: 1,
        critical: 0,
        high: 1,
        medium: 0,
        low: 0,
        source: "snyk",
        findings: []
      }
    },
    {
      ...node("com.fasterxml.jackson.core:jackson-core:jar::2.15.4", "com.fasterxml.jackson.core", "jackson-core", "2.15.4", "compile", 2),
      securityInsight: {
        status: "vulnerable",
        maxSeverity: "critical",
        vulnerabilityCount: 1,
        critical: 1,
        high: 0,
        medium: 0,
        low: 0,
        source: "snyk",
        findings: []
      }
    },
    node("com.fasterxml.jackson.core:jackson-annotations:jar::2.15.4", "com.fasterxml.jackson.core", "jackson-annotations", "2.15.4", "runtime", 2),
    node("org.consumer:uses-jackson:jar::1.0.0", "org.consumer", "uses-jackson", "1.0.0", "compile", 1)
  ],
  edges: [
    edge("root-databind", "dev.example:demo:jar::1.0.0", "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4", "compile"),
    edge("databind-core", "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4", "com.fasterxml.jackson.core:jackson-core:jar::2.15.4", "compile"),
    edge("databind-annotations", "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4", "com.fasterxml.jackson.core:jackson-annotations:jar::2.15.4", "runtime"),
    edge("root-consumer", "dev.example:demo:jar::1.0.0", "org.consumer:uses-jackson:jar::1.0.0", "compile"),
    edge("consumer-core", "org.consumer:uses-jackson:jar::1.0.0", "com.fasterxml.jackson.core:jackson-core:jar::2.15.4", "compile")
  ],
  paths: [],
  diagnostics: []
};

describe("buildSearchSuggestions", () => {
  it("returns no suggestions for a blank query", () => {
    expect(buildSearchSuggestions(document, visibleFor([]), "   ")).toEqual([]);
  });

  it("ranks direct artifact matches before lower value transitive matches", () => {
    const suggestions = buildSearchSuggestions(
      document,
      visibleFor([
        "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4",
        "com.fasterxml.jackson.core:jackson-core:jar::2.15.4",
        "com.fasterxml.jackson.core:jackson-annotations:jar::2.15.4",
        "org.consumer:uses-jackson:jar::1.0.0"
      ]),
      "jackson"
    );

    expect(suggestions.map((suggestion) => suggestion.nodeId)).toEqual([
      "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4",
      "com.fasterxml.jackson.core:jackson-core:jar::2.15.4",
      "com.fasterxml.jackson.core:jackson-annotations:jar::2.15.4",
      "org.consumer:uses-jackson:jar::1.0.0"
    ]);
    expect(suggestions[0].badges.map((badge) => badge.label)).toEqual(["compile", "direct", "high", "minor"]);
    expect(suggestions[1].badges.map((badge) => badge.label)).toEqual(["compile", "shared", "critical"]);
  });

  it("only suggests matching nodes instead of visible path context", () => {
    const suggestions = buildSearchSuggestions(
      document,
      {
        visibleNodeIds: new Set(["dev.example:demo:jar::1.0.0", "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4"]),
        visibleEdgeIds: new Set(["root-databind"]),
        matchingNodeIds: new Set(["com.fasterxml.jackson.core:jackson-databind:jar::2.15.4"])
      },
      "jackson"
    );

    expect(suggestions).toHaveLength(1);
    expect(suggestions[0].nodeId).toBe("com.fasterxml.jackson.core:jackson-databind:jar::2.15.4");
  });

  it("honors the configured suggestion limit", () => {
    const suggestions = buildSearchSuggestions(
      document,
      visibleFor([
        "com.fasterxml.jackson.core:jackson-databind:jar::2.15.4",
        "com.fasterxml.jackson.core:jackson-core:jar::2.15.4",
        "com.fasterxml.jackson.core:jackson-annotations:jar::2.15.4"
      ]),
      "jackson",
      2
    );

    expect(suggestions).toHaveLength(2);
  });
});

describe("nextSuggestionIndex", () => {
  it("starts at the first result when moving down from no active suggestion", () => {
    expect(nextSuggestionIndex(-1, 3, 1)).toBe(0);
  });

  it("wraps keyboard navigation in both directions", () => {
    expect(nextSuggestionIndex(2, 3, 1)).toBe(0);
    expect(nextSuggestionIndex(0, 3, -1)).toBe(2);
  });

  it("returns no active suggestion when there are no suggestions", () => {
    expect(nextSuggestionIndex(0, 0, 1)).toBe(-1);
  });
});

function visibleFor(matchingNodeIds: string[]): VisibilityState {
  return {
    visibleNodeIds: new Set(["dev.example:demo:jar::1.0.0", ...matchingNodeIds]),
    visibleEdgeIds: new Set(document.edges.map((current) => current.id)),
    matchingNodeIds: new Set(matchingNodeIds)
  };
}

function node(id: string, groupId: string, artifactId: string, version: string, scope: string, depth: number, root = false): GraphNode {
  return {
    id,
    groupId,
    artifactId,
    version,
    type: "jar",
    classifier: "",
    scope,
    optional: false,
    depth,
    root,
    moduleRoot: false,
    label: artifactId,
    coordinate: id,
    groupColorKey: groupId
  };
}

function edge(id: string, source: string, target: string, scope: string) {
  return { id, source, target, scope, optional: false, depth: 1 };
}
