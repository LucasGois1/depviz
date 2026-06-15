import { MultiDirectedGraph } from "graphology";
import circular from "graphology-layout/circular";
import forceAtlas2 from "graphology-layout-forceatlas2";
import { dependencyFanIn } from "./graph";
import type { Adjacency, DepvizDocument, GraphEdge, GraphNode, LayoutName, VisibilityState } from "./types";

export interface SigmaNodeAttributes {
  id: string;
  label: string;
  baseLabel: string;
  coordinate: string;
  groupId: string;
  artifactId: string;
  version: string;
  type: "circle";
  classifier: string;
  scope: string;
  optional: boolean;
  depth: number;
  root: boolean;
  moduleRoot: boolean;
  groupColorKey: string;
  fanIn: number;
  shared: boolean;
  x: number;
  y: number;
  size: number;
  baseSize: number;
  color: string;
  baseColor: string;
  hidden: boolean;
  highlighted: boolean;
  forceLabel: boolean;
  zIndex: number;
}

export interface SigmaEdgeAttributes {
  id: string;
  source: string;
  target: string;
  scope: string;
  optional: boolean;
  depth: number;
  targetFanIn: number;
  sharedTarget: boolean;
  type: "arrow";
  size: number;
  baseSize: number;
  color: string;
  baseColor: string;
  hidden: boolean;
  zIndex: number;
}

export type SigmaDependencyGraph = MultiDirectedGraph<SigmaNodeAttributes, SigmaEdgeAttributes>;

export interface SigmaGraphStateParams {
  adjacency: Adjacency;
  visibility: VisibilityState;
  selectedNodeId: string | null;
  showLabels: boolean;
}

export function toSigmaGraph(document: DepvizDocument, layout: LayoutName): SigmaDependencyGraph {
  const graph: SigmaDependencyGraph = new MultiDirectedGraph();
  const fanInByNodeId = dependencyFanIn(document);

  for (const node of document.nodes) {
    graph.addNode(node.id, toSigmaNodeAttributes(node, fanInByNodeId.get(node.id) ?? 0));
  }

  for (const edge of document.edges) {
    graph.addDirectedEdgeWithKey(edge.id, edge.source, edge.target, toSigmaEdgeAttributes(edge, fanInByNodeId.get(edge.target) ?? 0));
  }

  applySigmaLayout(graph, layout);
  return graph;
}

export function applySigmaLayout(graph: SigmaDependencyGraph, layout: LayoutName): void {
  if (graph.order === 0) {
    return;
  }

  if (layout === "circle") {
    circular.assign(graph, { center: 0, scale: Math.max(3, graph.order / 2) });
    return;
  }

  if (layout === "force") {
    circular.assign(graph, { center: 0, scale: Math.max(3, graph.order / 2) });
    forceAtlas2.assign(graph, {
      iterations: graph.order < 50 ? 150 : 90,
      settings: {
        adjustSizes: true,
        barnesHutOptimize: graph.order > 80,
        edgeWeightInfluence: 0.45,
        gravity: 0.08,
        scalingRatio: 9,
        slowDown: 4
      }
    });
    return;
  }

  if (layout === "concentric") {
    assignConcentricLayout(graph);
    return;
  }

  assignBreadthfirstLayout(graph);
}

export function applySigmaGraphState(graph: SigmaDependencyGraph, params: SigmaGraphStateParams): void {
  const { adjacency, selectedNodeId, showLabels, visibility } = params;
  const selectedNeighborhood = selectedNodeId ? closedNeighborhood(adjacency, selectedNodeId) : null;

  graph.forEachNode((nodeId, attributes) => {
    const visible = visibility.visibleNodeIds.has(nodeId);
    const selected = selectedNodeId === nodeId;
    const neighbor = Boolean(selectedNeighborhood?.has(nodeId));
    const dimmed = Boolean(selectedNodeId && !neighbor);
    const matched = visibility.matchingNodeIds.has(nodeId);
    const keyLabel = attributes.root || attributes.shared || attributes.depth <= 1;

    graph.mergeNodeAttributes(nodeId, {
      hidden: !visible,
      highlighted: selected || matched,
      color: dimmed ? "rgba(148, 163, 184, 0.24)" : matched && !selected ? "#f59e0b" : attributes.baseColor,
      label: showLabels || selected || matched || keyLabel ? attributes.baseLabel : "",
      forceLabel: selected || matched || keyLabel,
      size: selected ? Math.max(attributes.baseSize + 3, 12) : attributes.baseSize,
      zIndex: selected ? 30 : attributes.shared ? 12 : attributes.root ? 20 : 1
    });
  });

  graph.forEachEdge((edgeId, attributes, source, target) => {
    const visible = visibility.visibleEdgeIds.has(edgeId);
    const neighbor = Boolean(selectedNodeId && (source === selectedNodeId || target === selectedNodeId));
    const dimmed = Boolean(selectedNodeId && !neighbor);

    graph.mergeEdgeAttributes(edgeId, {
      hidden: !visible,
      color: dimmed ? "rgba(148, 163, 184, 0.2)" : neighbor ? "#334155" : attributes.baseColor,
      size: neighbor ? Math.max(attributes.baseSize + 0.8, 2.6) : attributes.baseSize,
      zIndex: neighbor ? 20 : attributes.sharedTarget ? 8 : 1
    });
  });
}

function toSigmaNodeAttributes(node: GraphNode, fanIn: number): SigmaNodeAttributes {
  const shared = fanIn > 1;
  const hub = fanIn >= 4;
  const baseSize = node.root ? 10.5 : shared ? Math.min(19, 9 + fanIn * 1.8) : 7;
  const baseColor = node.root ? "#111827" : hub ? "#b45309" : shared ? "#0f766e" : groupColor(node.groupColorKey);

  return {
    id: node.id,
    label: node.label,
    baseLabel: node.label,
    coordinate: node.coordinate,
    groupId: node.groupId,
    artifactId: node.artifactId,
    version: node.version,
    type: "circle",
    classifier: node.classifier,
    scope: node.scope,
    optional: node.optional,
    depth: node.depth,
    root: node.root,
    moduleRoot: node.moduleRoot,
    groupColorKey: node.groupColorKey,
    fanIn,
    shared,
    x: 0,
    y: 0,
    size: baseSize,
    baseSize,
    color: baseColor,
    baseColor,
    hidden: false,
    highlighted: false,
    forceLabel: node.root || shared || node.depth <= 1,
    zIndex: node.root ? 20 : shared ? 12 : 1
  };
}

function toSigmaEdgeAttributes(edge: GraphEdge, targetFanIn: number): SigmaEdgeAttributes {
  const sharedTarget = targetFanIn > 1;
  const baseSize = sharedTarget ? Math.min(2.8, 1.2 + targetFanIn * 0.16) : 0.8;
  const baseColor = sharedTarget ? "#0f766e" : "#94a3b8";

  return {
    id: edge.id,
    source: edge.source,
    target: edge.target,
    scope: edge.scope,
    optional: edge.optional,
    depth: edge.depth,
    targetFanIn,
    sharedTarget,
    type: "arrow",
    size: baseSize,
    baseSize,
    color: baseColor,
    baseColor,
    hidden: false,
    zIndex: sharedTarget ? 8 : 1
  };
}

function assignBreadthfirstLayout(graph: SigmaDependencyGraph): void {
  const byDepth = new Map<number, Array<{ id: string; label: string }>>();

  graph.forEachNode((id, attributes) => {
    const depth = Number(attributes.depth ?? 0);
    byDepth.set(depth, [...(byDepth.get(depth) ?? []), { id, label: attributes.baseLabel }]);
  });

  for (const [depth, nodes] of byDepth.entries()) {
    const sorted = nodes.sort((left, right) => left.label.localeCompare(right.label));
    sorted.forEach((node, index) => {
      graph.mergeNodeAttributes(node.id, {
        x: depth * 2.2,
        y: (index - (sorted.length - 1) / 2) * 1.25
      });
    });
  }
}

function assignConcentricLayout(graph: SigmaDependencyGraph): void {
  const rings = new Map<number, Array<{ id: string; label: string }>>();

  graph.forEachNode((id, attributes) => {
    const ring = attributes.root ? 0 : attributes.shared ? 1 : Math.max(2, attributes.depth + 1);
    rings.set(ring, [...(rings.get(ring) ?? []), { id, label: attributes.baseLabel }]);
  });

  for (const [ring, nodes] of rings.entries()) {
    const sorted = nodes.sort((left, right) => left.label.localeCompare(right.label));
    const radius = ring * 1.45;
    sorted.forEach((node, index) => {
      const angle = ring === 0 ? 0 : (Math.PI * 2 * index) / sorted.length;
      graph.mergeNodeAttributes(node.id, {
        x: ring === 0 ? 0 : Math.cos(angle) * radius,
        y: ring === 0 ? 0 : Math.sin(angle) * radius
      });
    });
  }
}

function closedNeighborhood(adjacency: Adjacency, nodeId: string): Set<string> {
  return new Set([nodeId, ...(adjacency.parents.get(nodeId) ?? []), ...(adjacency.children.get(nodeId) ?? [])]);
}

function groupColor(input: string): string {
  return `hsl(${colorHue(input)}, 58%, 46%)`;
}

function colorHue(input: string): number {
  let hash = 0;
  for (let index = 0; index < input.length; index += 1) {
    hash = (hash * 31 + input.charCodeAt(index)) % 360;
  }
  return hash;
}
