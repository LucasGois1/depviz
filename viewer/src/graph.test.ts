import { describe, expect, it } from "vitest";
import { buildAdjacency, hasSharedDependencies, recommendedInitialLayout, shouldShowAllLabelsInitially, toCytoscapeElements } from "./graph";
import type { DepvizDocument } from "./types";

const sharedTargetId = "org.shared:logging:jar::2.0.0";

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
    nodesByScope: { root: 1, compile: 3 },
    nodesByGroupId: { "dev.example": 1, "org.alpha": 1, "org.beta": 1, "org.shared": 1 }
  },
  viewerConfig: { initialLayout: "breadthfirst", maxInitialLabels: 500, nodeMode: "artifact" },
  nodes: [
    node("dev.example:demo:jar::1.0.0", "dev.example", "demo", true),
    node("org.alpha:client:jar::1.0.0", "org.alpha", "client"),
    node("org.beta:service:jar::1.0.0", "org.beta", "service"),
    node(sharedTargetId, "org.shared", "logging")
  ],
  edges: [
    edge("root-alpha", "dev.example:demo:jar::1.0.0", "org.alpha:client:jar::1.0.0"),
    edge("alpha-shared", "org.alpha:client:jar::1.0.0", sharedTargetId),
    edge("beta-shared", "org.beta:service:jar::1.0.0", sharedTargetId)
  ],
  paths: [
    { target: sharedTargetId, nodeIds: ["dev.example:demo:jar::1.0.0", "org.alpha:client:jar::1.0.0", sharedTargetId] },
    { target: sharedTargetId, nodeIds: ["dev.example:demo:jar::1.0.0", "org.beta:service:jar::1.0.0", sharedTargetId] }
  ],
  diagnostics: []
};

describe("toCytoscapeElements", () => {
  it("creates one Cytoscape node for a shared dependency with multiple incoming edges", () => {
    const elements = toCytoscapeElements(document);
    const sharedNodes = elements.filter((element) => element.group === "nodes" && element.data.id === sharedTargetId);
    const incomingEdges = elements.filter((element) => element.group === "edges" && element.data.target === sharedTargetId);

    expect(sharedNodes).toHaveLength(1);
    expect(incomingEdges.map((edgeElement) => edgeElement.data.source).sort()).toEqual([
      "org.alpha:client:jar::1.0.0",
      "org.beta:service:jar::1.0.0"
    ]);
  });

  it("preserves node metadata needed by the sidebar and styling", () => {
    const elements = toCytoscapeElements(document);
    const root = elements.find((element) => element.group === "nodes" && element.data.id === "dev.example:demo:jar::1.0.0");

    expect(root?.data).toMatchObject({
      label: "dev.example:demo",
      coordinate: "dev.example:demo:jar::1.0.0",
      groupColorKey: "dev.example",
      root: true
    });
  });

  it("marks shared dependencies with fan-in metadata for canvas styling", () => {
    const elements = toCytoscapeElements(document);
    const shared = elements.find((element) => element.group === "nodes" && element.data.id === sharedTargetId);
    const root = elements.find((element) => element.group === "nodes" && element.data.id === "dev.example:demo:jar::1.0.0");
    const edgeToShared = elements.find((element) => element.group === "edges" && element.data.id === "alpha-shared");

    expect(shared?.data).toMatchObject({
      fanIn: 2,
      shared: true
    });
    expect(shared?.classes?.split(" ")).toContain("shared");
    expect(edgeToShared?.data).toMatchObject({
      targetFanIn: 2,
      sharedTarget: true
    });
    expect(edgeToShared?.classes?.split(" ")).toContain("to-shared");
    expect(root?.data.fanIn).toBe(0);
    expect(root?.classes?.split(" ")).not.toContain("shared");
  });
});

describe("buildAdjacency", () => {
  it("indexes parents and children by shared graph node ID", () => {
    const adjacency = buildAdjacency(document);

    expect(adjacency.parents.get(sharedTargetId)).toEqual(
      new Set(["org.alpha:client:jar::1.0.0", "org.beta:service:jar::1.0.0"])
    );
    expect(adjacency.children.get("org.alpha:client:jar::1.0.0")).toEqual(new Set([sharedTargetId]));
  });
});

describe("hasSharedDependencies", () => {
  it("detects real graph convergence when multiple packages point at one dependency", () => {
    expect(hasSharedDependencies(document)).toBe(true);
  });

  it("does not treat a simple tree as shared", () => {
    expect(hasSharedDependencies({ ...document, edges: document.edges.slice(0, 2) })).toBe(false);
  });
});

describe("recommendedInitialLayout", () => {
  it("uses force layout for graphs with shared dependencies", () => {
    expect(recommendedInitialLayout(document, "breadthfirst")).toBe("force");
  });

  it("keeps the configured layout when the graph has no shared dependencies", () => {
    expect(recommendedInitialLayout({ ...document, edges: document.edges.slice(0, 2) }, "breadthfirst")).toBe("breadthfirst");
  });

  it("keeps an explicit non-hierarchical layout even when dependencies are shared", () => {
    expect(recommendedInitialLayout(document, "circle")).toBe("circle");
  });
});

describe("shouldShowAllLabelsInitially", () => {
  it("starts with key labels only when the graph has shared dependencies", () => {
    expect(shouldShowAllLabelsInitially(document)).toBe(false);
  });

  it("shows all labels for small tree-shaped graphs", () => {
    expect(shouldShowAllLabelsInitially({ ...document, edges: document.edges.slice(0, 2) })).toBe(true);
  });

  it("starts with key labels only when the graph exceeds the configured label limit", () => {
    expect(shouldShowAllLabelsInitially({ ...document, viewerConfig: { ...document.viewerConfig, maxInitialLabels: 2 } })).toBe(false);
  });
});

function node(id: string, groupId: string, artifactId: string, root = false) {
  return {
    id,
    groupId,
    artifactId,
    version: "1.0.0",
    type: "jar",
    classifier: "",
    scope: root ? "root" : "compile",
    optional: false,
    depth: root ? 0 : 1,
    root,
    moduleRoot: false,
    label: `${groupId}:${artifactId}`,
    coordinate: id,
    groupColorKey: groupId
  };
}

function edge(id: string, source: string, target: string) {
  return { id, source, target, scope: "compile", optional: false, depth: 1 };
}
