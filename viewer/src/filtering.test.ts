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
