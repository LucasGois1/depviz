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

  it("serializes version update attributes without changing node identity or size", () => {
    const versionNodeId = "org.alpha:client:jar::1.0.0";
    const versionDocument: DepvizDocument = {
      ...document,
      nodes: document.nodes.map((current) =>
        current.id === versionNodeId
          ? {
              ...current,
              versionInsight: {
                currentVersion: "1.0.0",
                latestVersion: "2.0.0",
                updateType: "major",
                status: "outdated",
                checked: true,
                message: null
              }
            }
          : current
      )
    };

    const plainGraph = toSigmaGraph(document, "force");
    const versionGraph = toSigmaGraph(versionDocument, "force");
    const versionNode = versionGraph.getNodeAttributes(versionNodeId);

    expect(versionGraph.nodes().sort()).toEqual(plainGraph.nodes().sort());
    expect(versionNode).toMatchObject({
      versionStatus: "outdated",
      updateType: "major",
      updateBadge: "M",
      forceLabel: true
    });
    expect(versionNode.size).toBe(plainGraph.getNodeAttribute(versionNodeId, "size"));
    expect(versionNode.baseSize).toBe(plainGraph.getNodeAttribute(versionNodeId, "baseSize"));
  });

  it("forces unavailable dependency labels while leaving current dependencies on normal label rules", () => {
    const unavailableNodeId = "org.beta:service:jar::1.0.0";
    const currentNodeId = sharedTargetId;
    const versionDocument: DepvizDocument = {
      ...document,
      nodes: document.nodes.map((current) => {
        if (current.id === unavailableNodeId) {
          return {
            ...current,
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
        if (current.id === currentNodeId) {
          return {
            ...current,
            versionInsight: {
              currentVersion: "2.0.0",
              latestVersion: "2.0.0",
              updateType: "none",
              status: "current",
              checked: true,
              message: null
            }
          };
        }
        return current;
      })
    };

    const graph = toSigmaGraph(versionDocument, "force");
    const adjacency = buildAdjacency(versionDocument);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(versionDocument.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(versionDocument.edges.map((current) => current.id)),
      matchingNodeIds: new Set()
    };

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: null, showLabels: false });

    expect(graph.getNodeAttributes(unavailableNodeId)).toMatchObject({
      label: "service",
      forceLabel: true,
      versionStatus: "unavailable",
      updateType: "unavailable",
      updateBadge: "!"
    });
    expect(graph.getNodeAttribute(currentNodeId, "updateBadge")).toBeUndefined();
    expect(graph.getNodeAttribute(currentNodeId, "forceLabel")).toBe(true);
  });

  it("preserves pre-badge node sizing semantics", () => {
    const hubDocument: DepvizDocument = {
      ...document,
      nodes: [
        node("dev.example:demo:jar::1.0.0", "dev.example", "demo", true),
        ...Array.from({ length: 10 }, (_, index) => node(`org.parent:parent-${index}:jar::1.0.0`, "org.parent", `parent-${index}`)),
        node(sharedTargetId, "org.shared", "logging")
      ],
      edges: Array.from({ length: 10 }, (_, index) => edge(`parent-${index}-shared`, `org.parent:parent-${index}:jar::1.0.0`, sharedTargetId))
    };

    const graph = toSigmaGraph(hubDocument, "force");

    expect(graph.getNodeAttribute("dev.example:demo:jar::1.0.0", "baseSize")).toBe(10.5);
    expect(graph.getNodeAttribute("org.parent:parent-0:jar::1.0.0", "baseSize")).toBe(7);
    expect(graph.getNodeAttribute(sharedTargetId, "baseSize")).toBe(19);
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

  it("staggers shared dependency lanes so labels do not stack on the same row", () => {
    const deeperSharedId = "org.shared:deep-shared:jar::1.0.0";
    const staggeredDocument: DepvizDocument = {
      ...document,
      nodes: [...document.nodes, node(deeperSharedId, "org.shared", "deep-shared", false, 3)],
      edges: [
        ...document.edges,
        edge("alpha-deeper-shared", "org.alpha:client:jar::1.0.0", deeperSharedId, 3),
        edge("beta-deeper-shared", "org.beta:service:jar::1.0.0", deeperSharedId, 3)
      ]
    };
    const graph = toSigmaGraph(staggeredDocument, "force");
    const firstShared = graph.getNodeAttributes(sharedTargetId);
    const deeperShared = graph.getNodeAttributes(deeperSharedId);

    expect(Math.abs(firstShared.y - deeperShared.y)).toBeGreaterThan(0.8);
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

  it("forces every label when all labels are enabled", () => {
    const deepNodeId = "org.gamma:deep-helper:jar::1.0.0";
    const deepDocument: DepvizDocument = {
      ...document,
      nodes: [...document.nodes, node(deepNodeId, "org.gamma", "deep-helper", false, 2)],
      edges: [...document.edges, edge("alpha-deep", "org.alpha:client:jar::1.0.0", deepNodeId, 2)]
    };
    const graph = toSigmaGraph(deepDocument, "force");
    const adjacency = buildAdjacency(deepDocument);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(deepDocument.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(deepDocument.edges.map((current) => current.id)),
      matchingNodeIds: new Set()
    };

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: null, showLabels: true });

    expect(graph.nodes().every((id) => graph.getNodeAttribute(id, "label") === graph.getNodeAttribute(id, "baseLabel"))).toBe(true);
    expect(graph.nodes().every((id) => graph.getNodeAttribute(id, "forceLabel"))).toBe(true);
  });
});

function node(id: string, groupId: string, artifactId: string, root = false, depth = root ? 0 : 1) {
  return {
    id,
    groupId,
    artifactId,
    version: "1.0.0",
    type: "jar",
    classifier: "",
    scope: root ? "root" : "compile",
    optional: false,
    depth,
    root,
    moduleRoot: false,
    label: `${groupId}:${artifactId}`,
    coordinate: id,
    groupColorKey: groupId
  };
}

function edge(id: string, source: string, target: string, depth = 1) {
  return { id, source, target, scope: "compile", optional: false, depth };
}

function distance(left: { x: number; y: number }, right: { x: number; y: number }): number {
  return Math.hypot(left.x - right.x, left.y - right.y);
}
