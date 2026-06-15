import { describe, expect, it } from "vitest";
import { buildAdjacency } from "./graph";
import { applySigmaGraphState, toSigmaGraph } from "./sigmaGraph";
import type { DepvizDocument, VisibilityState } from "./types";

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

describe("toSigmaGraph", () => {
  it("builds one Graphology node for a shared dependency with multiple incoming edges", () => {
    const graph = toSigmaGraph(document, "force");

    expect(graph.order).toBe(4);
    expect(graph.size).toBe(3);
    expect(graph.hasNode(sharedTargetId)).toBe(true);
    expect(graph.inNeighbors(sharedTargetId).sort()).toEqual(["org.alpha:client:jar::1.0.0", "org.beta:service:jar::1.0.0"]);
  });

  it("adds Sigma display attributes and fan-in metadata", () => {
    const graph = toSigmaGraph(document, "force");
    const shared = graph.getNodeAttributes(sharedTargetId);
    const root = graph.getNodeAttributes("dev.example:demo:jar::1.0.0");
    const edgeToShared = graph.getEdgeAttributes("alpha-shared");

    expect(shared).toMatchObject({
      artifactId: "logging",
      baseLabel: "logging",
      fanIn: 2,
      shared: true,
      label: "logging",
      type: "circle"
    });
    expect(shared.coordinate).toBe("org.shared:logging:jar::2.0.0");
    expect(shared.size).toBeGreaterThan(root.size);
    expect(Number.isFinite(shared.x)).toBe(true);
    expect(Number.isFinite(shared.y)).toBe(true);
    expect(edgeToShared).toMatchObject({
      targetFanIn: 2,
      sharedTarget: true,
      type: "arrow"
    });
  });

  it("keeps shared dependencies in a readable central band without overlapping primary nodes", () => {
    const graph = toSigmaGraph(document, "force");
    const root = graph.getNodeAttributes("dev.example:demo:jar::1.0.0");
    const alpha = graph.getNodeAttributes("org.alpha:client:jar::1.0.0");
    const beta = graph.getNodeAttributes("org.beta:service:jar::1.0.0");
    const shared = graph.getNodeAttributes(sharedTargetId);

    expect(root.x).toBeLessThan(alpha.x);
    expect(root.x).toBeLessThan(beta.x);
    expect(shared.x).toBeGreaterThan(alpha.x);
    expect(shared.x).toBeGreaterThan(beta.x);
    expect(distance(alpha, beta)).toBeGreaterThan(1.6);
    expect(distance(alpha, shared)).toBeGreaterThan(1.6);
    expect(distance(beta, shared)).toBeGreaterThan(1.6);
  });
});

describe("applySigmaGraphState", () => {
  it("dims non-neighbors and keeps the selected neighborhood emphasized", () => {
    const graph = toSigmaGraph(document, "force");
    const adjacency = buildAdjacency(document);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(document.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(document.edges.map((current) => current.id)),
      matchingNodeIds: new Set([sharedTargetId])
    };

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: sharedTargetId, showLabels: false });

    expect(graph.getNodeAttribute(sharedTargetId, "highlighted")).toBe(true);
    expect(graph.getNodeAttribute(sharedTargetId, "forceLabel")).toBe(true);
    expect(graph.getNodeAttribute("dev.example:demo:jar::1.0.0", "color")).toContain("rgba");
    expect(graph.getNodeAttribute("org.alpha:client:jar::1.0.0", "color")).not.toContain("rgba");
    expect(graph.getEdgeAttribute("alpha-shared", "color")).toBe("#334155");
    expect(graph.getEdgeAttribute("root-alpha", "color")).toContain("rgba");
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

function distance(left: { x: number; y: number }, right: { x: number; y: number }): number {
  return Math.hypot(left.x - right.x, left.y - right.y);
}
