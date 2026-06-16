import { describe, expect, it } from "vitest";
import { buildAdjacency } from "./graph";
import { applySigmaGraphState, toSigmaGraph, visibleSigmaNodeExtent, type SigmaDependencyGraph } from "./sigmaGraph";
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
    const direct = graph.getNodeAttributes("org.alpha:client:jar::1.0.0");
    const edgeToShared = graph.getEdgeAttributes("alpha-shared");

    expect(shared).toMatchObject({
      artifactId: "logging",
      baseLabel: "logging",
      fanIn: 2,
      shared: true,
      label: "logging",
      type: "circle",
      baseColor: "#2dd4bf"
    });
    expect(shared.coordinate).toBe("org.shared:logging:jar::2.0.0");
    expect(shared.size).toBeGreaterThan(root.size);
    expect(root.baseColor).toBe("#f8fafc");
    expect(direct.baseColor).toBe("#58a6ff");
    expect(Number.isFinite(shared.x)).toBe(true);
    expect(Number.isFinite(shared.y)).toBe(true);
    expect(edgeToShared).toMatchObject({
      targetFanIn: 2,
      sharedTarget: true,
      type: "arrow",
      baseColor: "rgba(45, 212, 191, 0.58)"
    });
    expect(edgeToShared.baseSize).toBeGreaterThanOrEqual(1);
  });

  it("uses high-contrast hub styling for heavily shared dependencies and their edges", () => {
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
    const hubNode = graph.getNodeAttributes(sharedTargetId);
    const hubEdge = graph.getEdgeAttributes("parent-0-shared");

    expect(hubNode.baseColor).toBe("#a78bfa");
    expect(hubEdge.baseColor).toBe("rgba(167, 139, 250, 0.64)");
    expect(hubEdge.baseSize).toBeGreaterThan(1.8);
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
      updateType: "unknown",
      updateBadge: "!"
    });
    expect(graph.getNodeAttribute(currentNodeId, "versionStatus")).toBe("current");
    expect(graph.getNodeAttribute(currentNodeId, "updateType")).toBe("none");
    expect(graph.getNodeAttribute(currentNodeId, "updateBadge")).toBeUndefined();
    expect(graph.getNodeAttribute(currentNodeId, "forceLabel")).toBe(true);
  });

  it("serializes vulnerable security badge attributes and forces labels through graph state", () => {
    const vulnerableNodeId = "org.security:vulnerable-helper:jar::1.0.0";
    const securityDocument: DepvizDocument = {
      ...document,
      nodes: [
        ...document.nodes,
        {
          ...node(vulnerableNodeId, "org.security", "vulnerable-helper", false, 3),
          securityInsight: {
            status: "vulnerable",
            maxSeverity: "high",
            vulnerabilityCount: 2,
            critical: 0,
            high: 1,
            medium: 1,
            low: 0,
            source: "snyk",
            findings: []
          }
        }
      ],
      edges: [...document.edges, edge("beta-vulnerable", "org.beta:service:jar::1.0.0", vulnerableNodeId, 3)]
    };
    const graph = toSigmaGraph(securityDocument, "force");
    const adjacency = buildAdjacency(securityDocument);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(securityDocument.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(securityDocument.edges.map((current) => current.id)),
      matchingNodeIds: new Set()
    };

    expect(graph.getNodeAttributes(vulnerableNodeId)).toMatchObject({
      securitySeverity: "high",
      securityBadge: "H",
      forceLabel: true
    });

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: null, showLabels: false });

    expect(graph.getNodeAttributes(vulnerableNodeId)).toMatchObject({
      label: "vulnerable-helper",
      forceLabel: true,
      securitySeverity: "high",
      securityBadge: "H"
    });
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
    expect(graph.getNodeAttribute(sharedTargetId, "baseColor")).toBe("#a78bfa");
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

  it("spreads dense dependency maps across continuous positions instead of fixed lanes", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "force");
    const extent = visibleSigmaNodeExtent(graph);
    const roundedXPositions = new Set(graph.nodes().map((nodeId) => graph.getNodeAttribute(nodeId, "x").toFixed(2)));

    expect(extent).not.toBeNull();
    expect(roundedXPositions.size).toBeGreaterThan(24);
    expect(extent!.maxX - extent!.minX).toBeGreaterThan(24);
  });

  it("keeps dense dependency map nodes separated when projected into the viewer canvas", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "force");

    expect(projectedMinimumNodeGap(graph)).toBeGreaterThan(0);
  });

  it("limits forced labels in dense graphs to high-signal nodes", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "force");
    const adjacency = buildAdjacency(denseDocument);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(denseDocument.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(denseDocument.edges.map((current) => current.id)),
      matchingNodeIds: new Set()
    };

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: null, showLabels: false });

    const labeledNodes = graph.nodes().filter((nodeId) => graph.getNodeAttribute(nodeId, "label"));
    expect(labeledNodes.length).toBeLessThan(30);
    expect(labeledNodes).toContain("dev.example:dense-demo:jar::1.0.0");
  });

  it("lays out dense flow graphs as a left-to-right dependency map instead of a vertical stack", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "breadthfirst");
    const extent = visibleSigmaNodeExtent(graph);

    expect(extent).not.toBeNull();
    expect(extent!.maxX - extent!.minX).toBeGreaterThan(36);
    expect(extent!.maxY - extent!.minY).toBeLessThan(120);
    expect(extent!.maxX - extent!.minX).toBeGreaterThan((extent!.maxY - extent!.minY) * 1.2);
    expect(graph.getNodeAttribute("dev.example:dense-demo:jar::1.0.0", "x")).toBeLessThan(
      graph.getNodeAttribute("org.direct:direct-0:jar::1.0.0", "x")
    );
    expect(graph.getNodeAttribute("org.direct:direct-0:jar::1.0.0", "x")).toBeLessThan(
      graph.getNodeAttribute("org.shared:shared-0:jar::1.0.0", "x")
    );
  });

  it("keeps dense flow nodes separated when projected into the viewer canvas", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "breadthfirst");

    expect(projectedMinimumNodeGap(graph)).toBeGreaterThan(0);
  });

  it("clusters group layout nodes by namespace instead of distributing namespaces around one circle", () => {
    const groupedDocument = groupedCommunityDocument();
    const graph = toSigmaGraph(groupedDocument, "circle");

    expect(averageSameGroupDistance(graph)).toBeLessThan(averageDifferentGroupCentroidDistance(graph) * 0.58);
  });

  it("uses a community mosaic for many namespaces instead of placing every group on one ring", () => {
    const graph = toSigmaGraph(manyNamespaceDocument(), "circle");

    expect(groupCentroidRadiusSpread(graph)).toBeGreaterThan(10);
  });

  it("keeps Maven namespace families together in the group layout", () => {
    const graph = toSigmaGraph(namespaceFamilyDocument(), "circle");
    const jacksonCore = graph.getNodeAttributes("com.fasterxml.jackson.core:jackson-core:jar::1.0.0");
    const jacksonModule = graph.getNodeAttributes("com.fasterxml.jackson.module:jackson-module:jar::1.0.0");
    const quarkus = graph.getNodeAttributes("io.quarkus:quarkus-core:jar::1.0.0");

    expect(distance(jacksonCore, jacksonModule)).toBeLessThan(distance(jacksonCore, quarkus) * 0.55);
  });

  it("places hub layout fan-in junctions near the center and leaves at the perimeter", () => {
    const denseDocument = denseSharedDependencyDocument(96);
    const graph = toSigmaGraph(denseDocument, "concentric");
    const center = graphCenter(graph);
    const hubDistance = distance(graph.getNodeAttributes("org.shared:shared-0:jar::1.0.0"), center);
    const leafDistance = distance(graph.getNodeAttributes("org.direct:direct-95:jar::1.0.0"), center);

    expect(hubDistance).toBeLessThan(leafDistance * 0.62);
  });

  it("preserves the base force-layout lane y placement", () => {
    const deeperSharedId = "org.shared:deep-shared:jar::1.0.0";
    const laneDocument: DepvizDocument = {
      ...document,
      nodes: [...document.nodes, node(deeperSharedId, "org.shared", "deep-shared", false, 3)],
      edges: [
        ...document.edges,
        edge("alpha-deeper-shared", "org.alpha:client:jar::1.0.0", deeperSharedId, 3),
        edge("beta-deeper-shared", "org.beta:service:jar::1.0.0", deeperSharedId, 3)
      ]
    };
    const graph = toSigmaGraph(laneDocument, "force");
    const firstShared = graph.getNodeAttributes(sharedTargetId);
    const deeperShared = graph.getNodeAttributes(deeperSharedId);

    expect(firstShared.y).toBe(0);
    expect(deeperShared.y).toBe(0);
  });

  it("computes the graph extent from visible nodes only", () => {
    const graph = toSigmaGraph(document, "force");
    const hiddenNodeId = "org.alpha:client:jar::1.0.0";
    graph.mergeNodeAttributes(hiddenNodeId, { hidden: true, x: 1_000, y: 1_000 });

    const visibleNodes = graph.nodes().filter((nodeId) => nodeId !== hiddenNodeId);
    const visibleX = visibleNodes.map((nodeId) => graph.getNodeAttribute(nodeId, "x"));
    const visibleY = visibleNodes.map((nodeId) => graph.getNodeAttribute(nodeId, "y"));

    expect(visibleSigmaNodeExtent(graph)).toEqual({
      minX: Math.min(...visibleX),
      maxX: Math.max(...visibleX),
      minY: Math.min(...visibleY),
      maxY: Math.max(...visibleY)
    });
  });

  it("returns no extent when every node is hidden", () => {
    const graph = toSigmaGraph(document, "force");
    graph.forEachNode((nodeId) => {
      graph.mergeNodeAttributes(nodeId, { hidden: true });
    });

    expect(visibleSigmaNodeExtent(graph)).toBeNull();
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
    expect(graph.getEdgeAttribute("alpha-shared", "color")).toBe("#dbeafe");
    expect(graph.getEdgeAttribute("root-alpha", "color")).toContain("rgba");
  });

  it("emphasizes search matches and visible search paths", () => {
    const graph = toSigmaGraph(document, "force");
    const adjacency = buildAdjacency(document);
    const visibility: VisibilityState = {
      visibleNodeIds: new Set(document.nodes.map((current) => current.id)),
      visibleEdgeIds: new Set(document.edges.map((current) => current.id)),
      matchingNodeIds: new Set([sharedTargetId])
    };

    applySigmaGraphState(graph, { adjacency, visibility, selectedNodeId: null, showLabels: false, searchActive: true });

    expect(graph.getNodeAttribute(sharedTargetId, "color")).toBe("#fbbf24");
    expect(graph.getNodeAttribute(sharedTargetId, "size")).toBeGreaterThan(graph.getNodeAttribute(sharedTargetId, "baseSize"));
    expect(graph.getNodeAttribute("org.alpha:client:jar::1.0.0", "color")).toBe("rgba(88, 166, 255, 0.64)");
    expect(graph.getEdgeAttribute("alpha-shared", "color")).toBe("#fbbf24");
    expect(graph.getEdgeAttribute("alpha-shared", "size")).toBeGreaterThan(graph.getEdgeAttribute("alpha-shared", "baseSize"));
    expect(graph.getEdgeAttribute("root-alpha", "color")).toBe("rgba(88, 166, 255, 0.5)");
  });

  it("shows deep current labels without forcing them when all labels are enabled", () => {
    const deepNodeId = "org.gamma:deep-helper:jar::1.0.0";
    const deepDocument: DepvizDocument = {
      ...document,
      nodes: [
        ...document.nodes,
        {
          ...node(deepNodeId, "org.gamma", "deep-helper", false, 2),
          versionInsight: {
            currentVersion: "1.0.0",
            latestVersion: "1.0.0",
            updateType: "none",
            status: "current",
            checked: true,
            message: null
          }
        }
      ],
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
    expect(graph.getNodeAttribute(deepNodeId, "label")).toBe("deep-helper");
    expect(graph.getNodeAttribute(deepNodeId, "forceLabel")).toBe(false);
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

function denseSharedDependencyDocument(count: number): DepvizDocument {
  const rootId = "dev.example:dense-demo:jar::1.0.0";
  const directNodes = Array.from({ length: count }, (_, index) => node(`org.direct:direct-${index}:jar::1.0.0`, "org.direct", `direct-${index}`, false, 1));
  const sharedNodes = Array.from({ length: Math.floor(count / 2) }, (_, index) =>
    node(`org.shared:shared-${index}:jar::1.0.0`, "org.shared", `shared-${index}`, false, 2)
  );
  const nodes = [node(rootId, "dev.example", "dense-demo", true), ...directNodes, ...sharedNodes];
  const edges = [
    ...directNodes.map((current, index) => edge(`root-direct-${index}`, rootId, current.id, 1)),
    ...sharedNodes.flatMap((current, index) => [
      edge(`direct-${index * 2}-shared-${index}`, directNodes[index * 2].id, current.id, 2),
      edge(`direct-${index * 2 + 1}-shared-${index}`, directNodes[index * 2 + 1].id, current.id, 2)
    ])
  ];

  return {
    ...document,
    summary: {
      nodeCount: nodes.length,
      edgeCount: edges.length,
      nodesByScope: { root: 1, compile: nodes.length - 1 },
      nodesByGroupId: { "dev.example": 1, "org.direct": directNodes.length, "org.shared": sharedNodes.length }
    },
    nodes,
    edges,
    paths: []
  };
}

function groupedCommunityDocument(): DepvizDocument {
  const rootId = "dev.example:groups-demo:jar::1.0.0";
  const groups = ["io.quarkus", "io.netty", "io.opentelemetry", "io.smallrye"];
  const nodes = [node(rootId, "dev.example", "groups-demo", true)];

  for (let index = 0; index < 64; index += 1) {
    const group = groups[index % groups.length];
    nodes.push(node(`${group}:artifact-${index}:jar::1.0.0`, group, `artifact-${index}`, false, 1 + (index % 4)));
  }

  const edges = nodes.slice(1, 17).map((current, index) => edge(`root-group-${index}`, rootId, current.id, 1));
  for (let index = 1; index < nodes.length - 4; index += 1) {
    edges.push(edge(`chain-${index}`, nodes[index].id, nodes[index + 4].id, nodes[index + 4].depth));
  }

  return {
    ...document,
    summary: {
      nodeCount: nodes.length,
      edgeCount: edges.length,
      nodesByScope: { root: 1, compile: nodes.length - 1 },
      nodesByGroupId: { "dev.example": 1, "io.quarkus": 16, "io.netty": 16, "io.opentelemetry": 16, "io.smallrye": 16 }
    },
    nodes,
    edges,
    paths: []
  };
}

function manyNamespaceDocument(): DepvizDocument {
  const rootId = "dev.example:many-groups-demo:jar::1.0.0";
  const nodes = [node(rootId, "dev.example", "many-groups-demo", true)];
  const edges = [];

  for (let groupIndex = 0; groupIndex < 28; groupIndex += 1) {
    const groupId = `org.group${groupIndex}`;
    const groupSize = groupIndex < 4 ? 10 - groupIndex : 2;
    let previousId = rootId;
    for (let itemIndex = 0; itemIndex < groupSize; itemIndex += 1) {
      const current = node(`${groupId}:artifact-${itemIndex}:jar::1.0.0`, groupId, `artifact-${itemIndex}`, false, 1 + (itemIndex % 3));
      nodes.push(current);
      edges.push(edge(`edge-${groupIndex}-${itemIndex}`, previousId, current.id, current.depth));
      previousId = current.id;
    }
  }

  return {
    ...document,
    summary: {
      nodeCount: nodes.length,
      edgeCount: edges.length,
      nodesByScope: { root: 1, compile: nodes.length - 1 },
      nodesByGroupId: Object.fromEntries(nodes.map((current) => [current.groupId, nodes.filter((node) => node.groupId === current.groupId).length]))
    },
    nodes,
    edges,
    paths: []
  };
}

function namespaceFamilyDocument(): DepvizDocument {
  const rootId = "dev.example:families-demo:jar::1.0.0";
  const nodes = [
    node(rootId, "dev.example", "families-demo", true),
    node("com.fasterxml.jackson.core:jackson-core:jar::1.0.0", "com.fasterxml.jackson.core", "jackson-core"),
    node("com.fasterxml.jackson.module:jackson-module:jar::1.0.0", "com.fasterxml.jackson.module", "jackson-module"),
    node("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar::1.0.0", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310"),
    node("io.quarkus:quarkus-core:jar::1.0.0", "io.quarkus", "quarkus-core"),
    node("io.quarkus.arc:quarkus-arc:jar::1.0.0", "io.quarkus.arc", "quarkus-arc"),
    node("jakarta.ws.rs:jakarta-ws-rs-api:jar::1.0.0", "jakarta.ws.rs", "jakarta-ws-rs-api"),
    node("jakarta.enterprise:jakarta-enterprise-cdi-api:jar::1.0.0", "jakarta.enterprise", "jakarta-enterprise-cdi-api")
  ];
  const edges = nodes.slice(1).map((current, index) => edge(`root-family-${index}`, rootId, current.id, 1));

  return {
    ...document,
    summary: {
      nodeCount: nodes.length,
      edgeCount: edges.length,
      nodesByScope: { root: 1, compile: nodes.length - 1 },
      nodesByGroupId: Object.fromEntries(nodes.map((current) => [current.groupId, nodes.filter((node) => node.groupId === current.groupId).length]))
    },
    nodes,
    edges,
    paths: []
  };
}

function distance(left: { x: number; y: number }, right: { x: number; y: number }): number {
  return Math.hypot(left.x - right.x, left.y - right.y);
}

function graphCenter(graph: SigmaDependencyGraph): { x: number; y: number } {
  const extent = visibleSigmaNodeExtent(graph);
  expect(extent).not.toBeNull();
  return {
    x: (extent!.minX + extent!.maxX) / 2,
    y: (extent!.minY + extent!.maxY) / 2
  };
}

function averageSameGroupDistance(graph: SigmaDependencyGraph): number {
  const nodesByGroup = nodesGroupedByNamespace(graph);
  const distances: number[] = [];

  for (const nodes of nodesByGroup.values()) {
    for (let leftIndex = 0; leftIndex < nodes.length; leftIndex += 1) {
      for (let rightIndex = leftIndex + 1; rightIndex < nodes.length; rightIndex += 1) {
        distances.push(distance(nodes[leftIndex], nodes[rightIndex]));
      }
    }
  }

  return average(distances);
}

function averageDifferentGroupCentroidDistance(graph: SigmaDependencyGraph): number {
  const centroids = [...nodesGroupedByNamespace(graph).values()].map((nodes) => ({
    x: average(nodes.map((current) => current.x)),
    y: average(nodes.map((current) => current.y))
  }));
  const distances: number[] = [];

  for (let leftIndex = 0; leftIndex < centroids.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < centroids.length; rightIndex += 1) {
      distances.push(distance(centroids[leftIndex], centroids[rightIndex]));
    }
  }

  return average(distances);
}

function nodesGroupedByNamespace(graph: SigmaDependencyGraph): Map<string, Array<{ x: number; y: number }>> {
  const nodesByGroup = new Map<string, Array<{ x: number; y: number }>>();

  graph.forEachNode((_nodeId, attributes) => {
    if (attributes.root) {
      return;
    }
    nodesByGroup.set(attributes.groupId, [...(nodesByGroup.get(attributes.groupId) ?? []), { x: attributes.x, y: attributes.y }]);
  });

  return nodesByGroup;
}

function groupCentroidRadiusSpread(graph: SigmaDependencyGraph): number {
  const radii = [...nodesGroupedByNamespace(graph).values()].map((nodes) => {
    const centroid = {
      x: average(nodes.map((current) => current.x)),
      y: average(nodes.map((current) => current.y))
    };
    return Math.hypot(centroid.x, centroid.y);
  });

  return Math.max(...radii) - Math.min(...radii);
}

function average(values: number[]): number {
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function projectedMinimumNodeGap(graph: SigmaDependencyGraph): number {
  const extent = visibleSigmaNodeExtent(graph);
  expect(extent).not.toBeNull();

  const graphSpan = Math.max(extent!.maxX - extent!.minX, extent!.maxY - extent!.minY, 0.001);
  const usableCanvasSpan = 492 - 52 * 2;
  const coordinateToPixel = usableCanvasSpan / graphSpan;
  const nodes = graph.nodes().map((nodeId) => graph.getNodeAttributes(nodeId));
  let minimumGap = Infinity;

  for (let leftIndex = 0; leftIndex < nodes.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < nodes.length; rightIndex += 1) {
      const left = nodes[leftIndex];
      const right = nodes[rightIndex];
      const projectedDistance = distance(left, right) * coordinateToPixel;
      const requiredDistance = left.baseSize + right.baseSize;
      minimumGap = Math.min(minimumGap, projectedDistance - requiredDistance);
    }
  }

  return minimumGap;
}
