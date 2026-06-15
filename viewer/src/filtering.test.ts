import { describe, expect, it } from "vitest";
import { buildVisibility, createFilterState } from "./filtering";
import type { DepvizDocument } from "./types";

const document: DepvizDocument = {
  schemaVersion: "1.0",
  generatedAt: "2026-06-13T00:00:00Z",
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
    nodeCount: 4,
    edgeCount: 3,
    nodesByScope: { root: 1, compile: 2, test: 1 },
    nodesByGroupId: { "dev.example": 1, "org.shared": 1, "org.alpha": 1, "org.test": 1 }
  },
  viewerConfig: { initialLayout: "breadthfirst", maxInitialLabels: 500, nodeMode: "artifact" },
  nodes: [
    node("dev.example:demo:jar::1.0.0", "dev.example", "demo", "1.0.0", "root", false, true),
    node("org.shared:core:jar::2.0.0", "org.shared", "core", "2.0.0", "compile", false),
    node("org.alpha:adapter:jar::1.2.0", "org.alpha", "adapter", "1.2.0", "compile", true),
    node("org.test:fixture:jar::5.0.0", "org.test", "fixture", "5.0.0", "test", false)
  ],
  edges: [
    edge("root-core", "dev.example:demo:jar::1.0.0", "org.shared:core:jar::2.0.0", "compile", false),
    edge("root-adapter", "dev.example:demo:jar::1.0.0", "org.alpha:adapter:jar::1.2.0", "compile", true),
    edge("adapter-fixture", "org.alpha:adapter:jar::1.2.0", "org.test:fixture:jar::5.0.0", "test", false)
  ],
  paths: [],
  diagnostics: []
};

describe("buildVisibility", () => {
  it("keeps matching nodes, ancestors, descendants, and connecting edges visible for search", () => {
    const visibility = buildVisibility(document, {
      ...createFilterState(),
      search: "adapter"
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.alpha:adapter:jar::1.2.0",
        "org.test:fixture:jar::5.0.0"
      ])
    );
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-adapter", "adapter-fixture"]));
  });

  it("filters by scope while retaining the root and matching reachable edges", () => {
    const visibility = buildVisibility(document, {
      ...createFilterState(),
      scopes: new Set(["compile"])
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.shared:core:jar::2.0.0",
        "org.alpha:adapter:jar::1.2.0"
      ])
    );
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-core", "root-adapter"]));
  });

  it("can hide optional dependencies and graph portions only reachable through them", () => {
    const visibility = buildVisibility(document, {
      ...createFilterState(),
      optionalMode: "required"
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set(["dev.example:demo:jar::1.0.0", "org.shared:core:jar::2.0.0"])
    );
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-core"]));
  });

  it("keeps a shared dependency visible when a selected-scope edge reaches a node with a different retained scope", () => {
    const runtimeSharedDocument: DepvizDocument = {
      ...document,
      summary: {
        nodeCount: 4,
        edgeCount: 4,
        nodesByScope: { root: 1, compile: 3, runtime: 0 },
        nodesByGroupId: document.summary.nodesByGroupId
      },
      nodes: [
        node("dev.example:demo:jar::1.0.0", "dev.example", "demo", "1.0.0", "root", false, true),
        node("org.alpha:adapter:jar::1.2.0", "org.alpha", "adapter", "1.2.0", "compile", false),
        node("org.beta:runtime-adapter:jar::3.0.0", "org.beta", "runtime-adapter", "3.0.0", "runtime", false),
        node("org.shared:core:jar::2.0.0", "org.shared", "core", "2.0.0", "compile", false)
      ],
      edges: [
        edge("root-alpha", "dev.example:demo:jar::1.0.0", "org.alpha:adapter:jar::1.2.0", "compile", false),
        edge("root-beta", "dev.example:demo:jar::1.0.0", "org.beta:runtime-adapter:jar::3.0.0", "runtime", false),
        edge("alpha-shared", "org.alpha:adapter:jar::1.2.0", "org.shared:core:jar::2.0.0", "compile", false),
        edge("beta-shared", "org.beta:runtime-adapter:jar::3.0.0", "org.shared:core:jar::2.0.0", "runtime", false)
      ]
    };

    const visibility = buildVisibility(runtimeSharedDocument, {
      ...createFilterState(),
      scopes: new Set(["runtime"])
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.beta:runtime-adapter:jar::3.0.0",
        "org.shared:core:jar::2.0.0"
      ])
    );
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-beta", "beta-shared"]));
  });

  it("filters to outdated dependencies while keeping root and path context visible", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      updateMode: "outdated"
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.current:platform:jar::1.0.0",
        "org.major:api:jar::1.0.0",
        "org.patch:helper:jar::1.0.0",
        "org.test:fixture:jar::1.0.0"
      ])
    );
    expect(visibility.matchingNodeIds).toEqual(
      new Set(["org.major:api:jar::1.0.0", "org.patch:helper:jar::1.0.0", "org.test:fixture:jar::1.0.0"])
    );
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-platform", "platform-api", "api-helper", "root-fixture"]));
  });

  it("filters to major updates while keeping path context visible", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      updateMode: "major"
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.current:platform:jar::1.0.0",
        "org.major:api:jar::1.0.0",
        "org.patch:helper:jar::1.0.0"
      ])
    );
    expect(visibility.matchingNodeIds).toEqual(new Set(["org.major:api:jar::1.0.0"]));
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-platform", "platform-api", "api-helper"]));
  });

  it("composes patch update filtering with scope filters", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      updateMode: "patch",
      scopes: new Set(["compile"])
    });

    expect(visibility.visibleNodeIds).toEqual(
      new Set([
        "dev.example:demo:jar::1.0.0",
        "org.current:platform:jar::1.0.0",
        "org.major:api:jar::1.0.0",
        "org.patch:helper:jar::1.0.0"
      ])
    );
    expect(visibility.matchingNodeIds).toEqual(new Set(["org.patch:helper:jar::1.0.0"]));
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-platform", "platform-api", "api-helper"]));
  });

  it("keeps existing visibility behavior when update filtering is all", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      updateMode: "all"
    });

    expect(visibility.visibleNodeIds).toEqual(new Set(versionDocument.nodes.map((versionNode) => versionNode.id)));
    expect(visibility.visibleEdgeIds).toEqual(new Set(versionDocument.edges.map((versionEdge) => versionEdge.id)));
    expect(visibility.matchingNodeIds).toEqual(new Set());
  });

  it("treats update mode as all when version summary is missing", () => {
    const { versionSummary, ...documentWithoutVersionSummary } = versionDocument;
    const visibility = buildVisibility(documentWithoutVersionSummary, {
      ...createFilterState(),
      updateMode: "outdated"
    });

    expect(versionSummary?.enabled).toBe(true);
    expect(visibility.visibleNodeIds).toEqual(new Set(versionDocument.nodes.map((versionNode) => versionNode.id)));
    expect(visibility.visibleEdgeIds).toEqual(new Set(versionDocument.edges.map((versionEdge) => versionEdge.id)));
    expect(visibility.matchingNodeIds).toEqual(new Set());
  });

  it("treats update mode as all when version summary is disabled", () => {
    const disabledVersionDocument: DepvizDocument = {
      ...versionDocument,
      versionSummary: {
        ...versionDocument.versionSummary!,
        enabled: false
      }
    };
    const visibility = buildVisibility(disabledVersionDocument, {
      ...createFilterState(),
      updateMode: "outdated"
    });

    expect(visibility.visibleNodeIds).toEqual(new Set(versionDocument.nodes.map((versionNode) => versionNode.id)));
    expect(visibility.visibleEdgeIds).toEqual(new Set(versionDocument.edges.map((versionEdge) => versionEdge.id)));
    expect(visibility.matchingNodeIds).toEqual(new Set());
  });

  it("does not reveal an outdated descendant hidden behind a collapsed ancestor", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      updateMode: "outdated",
      collapsedNodeIds: new Set(["org.current:platform:jar::1.0.0"])
    });

    expect(visibility.visibleNodeIds).not.toContain("org.major:api:jar::1.0.0");
    expect(visibility.visibleNodeIds).not.toContain("org.patch:helper:jar::1.0.0");
    expect(visibility.visibleNodeIds).toEqual(new Set(["dev.example:demo:jar::1.0.0", "org.test:fixture:jar::1.0.0"]));
    expect(visibility.matchingNodeIds).toEqual(new Set(["org.test:fixture:jar::1.0.0"]));
    expect(visibility.visibleEdgeIds).toEqual(new Set(["root-fixture"]));
  });

  it("intersects search matches with update mode matches", () => {
    const visibility = buildVisibility(versionDocument, {
      ...createFilterState(),
      search: "platform",
      updateMode: "outdated"
    });

    expect(visibility.visibleNodeIds).toEqual(new Set());
    expect(visibility.matchingNodeIds).toEqual(new Set());
    expect(visibility.visibleEdgeIds).toEqual(new Set());
  });
});

function node(
  id: string,
  groupId: string,
  artifactId: string,
  version: string,
  scope: string,
  optional: boolean,
  root = false
) {
  return {
    id,
    groupId,
    artifactId,
    version,
    type: "jar",
    classifier: "",
    scope,
    optional,
    depth: root ? 0 : 1,
    root,
    moduleRoot: false,
    label: `${groupId}:${artifactId}`,
    coordinate: id,
    groupColorKey: groupId
  };
}

function edge(id: string, source: string, target: string, scope: string, optional: boolean) {
  return { id, source, target, scope, optional, depth: 1 };
}

const versionDocument: DepvizDocument = {
  ...document,
  summary: {
    nodeCount: 6,
    edgeCount: 5,
    nodesByScope: { root: 1, compile: 4, test: 1 },
    nodesByGroupId: {
      "dev.example": 1,
      "org.current": 1,
      "org.major": 1,
      "org.patch": 1,
      "org.test": 1,
      "org.unavailable": 1
    }
  },
  versionSummary: {
    enabled: true,
    checked: 5,
    current: 1,
    outdated: 3,
    patch: 2,
    minor: 0,
    major: 1,
    unknown: 0,
    unavailable: 1
  },
  nodes: [
    versionNode("dev.example:demo:jar::1.0.0", "dev.example", "demo", "root", "none", "current", true),
    versionNode("org.current:platform:jar::1.0.0", "org.current", "platform", "compile", "none", "current"),
    versionNode("org.major:api:jar::1.0.0", "org.major", "api", "compile", "major", "outdated"),
    versionNode("org.patch:helper:jar::1.0.0", "org.patch", "helper", "compile", "patch", "outdated"),
    versionNode("org.test:fixture:jar::1.0.0", "org.test", "fixture", "test", "patch", "outdated"),
    versionNode("org.unavailable:legacy:jar::1.0.0", "org.unavailable", "legacy", "compile", "unknown", "unavailable")
  ],
  edges: [
    edge("root-platform", "dev.example:demo:jar::1.0.0", "org.current:platform:jar::1.0.0", "compile", false),
    edge("platform-api", "org.current:platform:jar::1.0.0", "org.major:api:jar::1.0.0", "compile", false),
    edge("api-helper", "org.major:api:jar::1.0.0", "org.patch:helper:jar::1.0.0", "compile", false),
    edge("root-fixture", "dev.example:demo:jar::1.0.0", "org.test:fixture:jar::1.0.0", "test", false),
    edge("root-legacy", "dev.example:demo:jar::1.0.0", "org.unavailable:legacy:jar::1.0.0", "compile", false)
  ]
};

function versionNode(
  id: string,
  groupId: string,
  artifactId: string,
  scope: string,
  updateType: "patch" | "minor" | "major" | "unknown" | "none",
  status: "current" | "outdated" | "unavailable" | "unchecked",
  root = false
) {
  return {
    ...node(id, groupId, artifactId, "1.0.0", scope, false, root),
    versionInsight: {
      currentVersion: "1.0.0",
      latestVersion: status === "unavailable" ? null : "2.0.0",
      updateType,
      status,
      checked: status !== "unchecked",
      message: null
    }
  };
}
