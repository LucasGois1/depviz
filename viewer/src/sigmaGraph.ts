import { MultiDirectedGraph } from "graphology";
import forceAtlas2 from "graphology-layout-forceatlas2";
import noverlap from "graphology-layout-noverlap";
import { dependencyFanIn } from "./graph";
import { badgeTextForVersionInsight, securityBadgeText } from "./sigmaLabelRenderer";
import type { Adjacency, DepvizDocument, GraphEdge, GraphNode, LayoutName, SecuritySeverity, VersionInsight, VisibilityState } from "./types";

export type SigmaVersionUpdateType = VersionInsight["updateType"];

export interface SigmaNodeAttributes {
  id: string;
  label: string;
  baseLabel: string;
  labelTextVisible: boolean;
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
  versionStatus?: VersionInsight["status"];
  updateType?: SigmaVersionUpdateType;
  updateBadge?: string;
  versionBadgeVisible: boolean;
  securitySeverity?: SecuritySeverity;
  securityBadge?: string;
  securityBadgeVisible: boolean;
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

const FORCE_LAYOUT_THRESHOLD = 32;
const DENSE_LABEL_THRESHOLD = 120;
const PROJECTED_CANVAS_SPAN = 388;
const PROJECTED_NODE_MARGIN = 1.5;

export interface SigmaNodeExtent {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
}

export interface SigmaGraphStateParams {
  adjacency: Adjacency;
  visibility: VisibilityState;
  selectedNodeId: string | null;
  showLabels: boolean;
  showVersionBadges?: boolean;
  showSecurityBadges?: boolean;
  searchActive?: boolean;
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

  applyDensitySizing(graph, layout);
  applySigmaLayout(graph, layout);
  return graph;
}

export function applySigmaLayout(graph: SigmaDependencyGraph, layout: LayoutName): void {
  if (graph.order === 0) {
    return;
  }

  if (layout === "circle") {
    assignGroupLayout(graph);
    return;
  }

  if (layout === "force") {
    assignDependencyMapLayout(graph);
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
  const showVersionBadges = params.showVersionBadges ?? true;
  const showSecurityBadges = params.showSecurityBadges ?? true;
  const selectedNeighborhood = selectedNodeId ? closedNeighborhood(adjacency, selectedNodeId) : null;
  const denseGraph = graph.order >= DENSE_LABEL_THRESHOLD;
  const searchActive = params.searchActive === true && visibility.matchingNodeIds.size > 0;

  graph.forEachNode((nodeId, attributes) => {
    const visible = visibility.visibleNodeIds.has(nodeId);
    const selected = selectedNodeId === nodeId;
    const neighbor = Boolean(selectedNeighborhood?.has(nodeId));
    const dimmed = Boolean(selectedNodeId && !neighbor);
    const matched = visibility.matchingNodeIds.has(nodeId);
    const searchContext = searchActive && visible && !matched;
    const hubLabel = attributes.fanIn >= (denseGraph ? 8 : 4);
    const keyLabel = attributes.root || attributes.moduleRoot || hubLabel || (!denseGraph && (attributes.shared || attributes.depth <= 1));
    const versionLabel = attributes.versionStatus === "outdated" || attributes.versionStatus === "unavailable";
    const securityLabel = Boolean(attributes.securityBadge);
    const forceLabel = selected || matched || keyLabel || versionLabel || securityLabel;
    const visibleBadge = (showVersionBadges && Boolean(attributes.updateBadge)) || (showSecurityBadges && Boolean(attributes.securityBadge));

    graph.mergeNodeAttributes(nodeId, {
      hidden: !visible,
      highlighted: selected || matched,
      color: dimmed ? "rgba(71, 85, 105, 0.3)" : matched && !selected ? "#fbbf24" : searchContext ? searchContextNodeColor(attributes) : attributes.baseColor,
      label: showLabels || visibleBadge ? attributes.baseLabel : "",
      labelTextVisible: showLabels,
      forceLabel,
      versionBadgeVisible: showVersionBadges,
      securityBadgeVisible: showSecurityBadges,
      size: selected ? Math.max(attributes.baseSize + 3, 12) : matched ? Math.max(attributes.baseSize + 2.2, 10.5) : attributes.baseSize,
      zIndex: selected ? 30 : matched ? 24 : attributes.shared ? 12 : attributes.root ? 20 : 1
    });
  });

  graph.forEachEdge((edgeId, attributes, source, target) => {
    const visible = visibility.visibleEdgeIds.has(edgeId);
    const neighbor = Boolean(selectedNodeId && (source === selectedNodeId || target === selectedNodeId));
    const dimmed = Boolean(selectedNodeId && !neighbor);
    const searchMatch = searchActive && (visibility.matchingNodeIds.has(source) || visibility.matchingNodeIds.has(target));
    const searchContext = searchActive && visible && !searchMatch;

    graph.mergeEdgeAttributes(edgeId, {
      hidden: !visible,
      color: dimmed ? "rgba(71, 85, 105, 0.24)" : neighbor ? "#dbeafe" : searchMatch ? "#fbbf24" : searchContext ? "rgba(88, 166, 255, 0.5)" : attributes.baseColor,
      size: neighbor ? Math.max(attributes.baseSize + 1.1, 3.1) : searchMatch ? Math.max(attributes.baseSize + 0.95, 2.55) : searchContext ? Math.max(attributes.baseSize + 0.32, 1.35) : attributes.baseSize,
      zIndex: neighbor ? 20 : searchMatch ? 16 : searchContext ? 10 : attributes.sharedTarget ? 8 : 1
    });
  });
}

function searchContextNodeColor(attributes: SigmaNodeAttributes): string {
  if (attributes.root || attributes.moduleRoot || attributes.depth <= 1) {
    return "rgba(88, 166, 255, 0.64)";
  }
  if (attributes.shared) {
    return "rgba(45, 212, 191, 0.56)";
  }
  return "rgba(127, 142, 163, 0.54)";
}

export function visibleSigmaNodeExtent(graph: SigmaDependencyGraph): SigmaNodeExtent | null {
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;

  graph.forEachNode((_nodeId, attributes) => {
    if (attributes.hidden) {
      return;
    }

    minX = Math.min(minX, attributes.x);
    maxX = Math.max(maxX, attributes.x);
    minY = Math.min(minY, attributes.y);
    maxY = Math.max(maxY, attributes.y);
  });

  if (!Number.isFinite(minX) || !Number.isFinite(maxX) || !Number.isFinite(minY) || !Number.isFinite(maxY)) {
    return null;
  }

  return { minX, maxX, minY, maxY };
}

function toSigmaNodeAttributes(node: GraphNode, fanIn: number): SigmaNodeAttributes {
  const shared = fanIn > 1;
  const hub = fanIn >= 6;
  const baseSize = node.root ? 10.5 : shared ? Math.min(19, 9 + fanIn * 1.8) : 7;
  const baseColor = node.root || node.moduleRoot ? "#f8fafc" : hub ? "#a78bfa" : shared ? "#2dd4bf" : node.depth <= 1 ? "#58a6ff" : "#7f8ea3";
  const canvasLabel = compactCanvasLabel(node);
  const updateBadge = badgeTextForVersionInsight(node.versionInsight ?? null) ?? undefined;
  const versionStatus = node.versionInsight?.status;
  const updateType = node.versionInsight?.updateType;
  const securityBadge = securityBadgeText(node.securityInsight ?? null) ?? undefined;
  const securitySeverity = securityBadge ? node.securityInsight?.maxSeverity : undefined;
  const forceLabel =
    node.root || shared || node.depth <= 1 || versionStatus === "outdated" || versionStatus === "unavailable" || Boolean(securityBadge);

  return {
    id: node.id,
    label: canvasLabel,
    baseLabel: canvasLabel,
    labelTextVisible: true,
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
    forceLabel,
    ...(versionStatus ? { versionStatus } : {}),
    ...(node.versionInsight ? { updateType } : {}),
    ...(updateBadge ? { updateBadge } : {}),
    versionBadgeVisible: true,
    ...(securitySeverity ? { securitySeverity } : {}),
    ...(securityBadge ? { securityBadge } : {}),
    securityBadgeVisible: true,
    zIndex: node.root ? 20 : shared ? 12 : 1
  };
}

function compactCanvasLabel(node: GraphNode): string {
  return node.artifactId;
}

function toSigmaEdgeAttributes(edge: GraphEdge, targetFanIn: number): SigmaEdgeAttributes {
  const sharedTarget = targetFanIn > 1;
  const hubTarget = targetFanIn >= 6;
  const baseSize = hubTarget ? Math.min(2.35, 1.18 + targetFanIn * 0.08) : sharedTarget ? Math.min(1.65, 0.86 + targetFanIn * 0.07) : 0.72;
  const baseColor = hubTarget ? "rgba(167, 139, 250, 0.64)" : sharedTarget ? "rgba(45, 212, 191, 0.58)" : "rgba(148, 163, 184, 0.34)";

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

function applyDensitySizing(graph: SigmaDependencyGraph, layout: LayoutName): void {
  const scale = densitySizeScale(graph.order, layout);
  if (scale === 1) {
    return;
  }

  graph.forEachNode((nodeId, attributes) => {
    const baseSize = Math.max(attributes.root ? 7.5 : 4.5, attributes.baseSize * scale);
    graph.mergeNodeAttributes(nodeId, {
      baseSize,
      size: baseSize
    });
  });
}

function densitySizeScale(order: number, layout: LayoutName): number {
  if (order < 120) {
    return 1;
  }
  if (layout !== "force") {
    if (order < 240) {
      return 0.62;
    }
    if (order < 420) {
      return 0.54;
    }
    return 0.48;
  }
  if (order < 240) {
    return 0.72;
  }
  if (order < 420) {
    return 0.62;
  }
  return 0.54;
}

function assignBreadthfirstLayout(graph: SigmaDependencyGraph): void {
  const byDepth = new Map<number, Array<{ id: string; label: string; fanIn: number; outDegree: number; root: boolean }>>();

  graph.forEachNode((id, attributes) => {
    const depth = Number(attributes.depth ?? 0);
    byDepth.set(depth, [
      ...(byDepth.get(depth) ?? []),
      { id, label: attributes.baseLabel, fanIn: attributes.fanIn, outDegree: graph.outDegree(id), root: attributes.root }
    ]);
  });

  const depthKeys = [...byDepth.keys()].sort((left, right) => left - right);
  let nextLaneX = 0;

  for (const depth of depthKeys) {
    const sorted = [...(byDepth.get(depth) ?? [])].sort((left, right) => {
      if (left.root !== right.root) return left.root ? -1 : 1;
      if (left.outDegree !== right.outDegree) return right.outDegree - left.outDegree;
      if (left.fanIn !== right.fanIn) return right.fanIn - left.fanIn;
      return left.label.localeCompare(right.label);
    });
    const columns = depth === 0 ? 1 : Math.max(3, Math.ceil(Math.sqrt(sorted.length / 1.85)));
    const rows = Math.max(1, Math.ceil(sorted.length / columns));
    const columnSpacing = depth === 0 ? 0 : 10.2;
    const rowSpacing = 9.4;

    sorted.forEach((node, index) => {
      const column = index % columns;
      const row = Math.floor(index / columns);
      graph.mergeNodeAttributes(node.id, {
        x: nextLaneX + column * columnSpacing,
        y: (row - (rows - 1) / 2) * rowSpacing
      });
    });

    nextLaneX += Math.max(20, (columns - 1) * columnSpacing + 22);
  }
}

function assignDependencyMapLayout(graph: SigmaDependencyGraph): void {
  assignDependencyLaneLayout(graph);

  if (graph.order >= FORCE_LAYOUT_THRESHOLD) {
    relaxDenseDependencyMapLayout(graph);
  }
}

function assignDependencyLaneLayout(graph: SigmaDependencyGraph): void {
  const lanes = new Map<number, Array<{ id: string; label: string; fanIn: number; root: boolean; shared: boolean }>>();

  graph.forEachNode((id, attributes) => {
    const lane = dependencyLane(attributes);
    lanes.set(lane, [
      ...(lanes.get(lane) ?? []),
      {
        id,
        label: attributes.baseLabel,
        fanIn: attributes.fanIn,
        root: attributes.root,
        shared: attributes.shared
      }
    ]);
  });

  const laneKeys = [...lanes.keys()].sort((left, right) => left - right);
  const maxLane = Math.max(...laneKeys);
  const horizontalSpacing = 3.2;
  const verticalSpacing = 2.2;

  for (const lane of laneKeys) {
    const nodes = [...(lanes.get(lane) ?? [])].sort((left, right) => {
      if (left.root !== right.root) return left.root ? -1 : 1;
      if (left.shared !== right.shared) return left.shared ? -1 : 1;
      if (left.fanIn !== right.fanIn) return right.fanIn - left.fanIn;
      return left.label.localeCompare(right.label);
    });

    nodes.forEach((node, index) => {
      graph.mergeNodeAttributes(node.id, {
        x: (lane - maxLane / 2) * horizontalSpacing,
        y: centerOutOffset(index) * verticalSpacing
      });
    });
  }
}

function relaxDenseDependencyMapLayout(graph: SigmaDependencyGraph): void {
  const settings = forceAtlas2.inferSettings(graph);
  forceAtlas2.assign(graph, {
    iterations: forceAtlasIterations(graph.order),
    settings: {
      ...settings,
      adjustSizes: false,
      barnesHutOptimize: graph.order >= 120,
      edgeWeightInfluence: 0.55,
      gravity: 0.85,
      scalingRatio: Math.max(settings.scalingRatio ?? 1, Math.sqrt(graph.order) * 1.6),
      slowDown: 2.8
    }
  });
  noverlap.assign(graph, {
    maxIterations: 320,
    inputReducer: (_key, attributes) => ({
      x: attributes.x,
      y: attributes.y,
      size: Math.max(3, Number((attributes as SigmaNodeAttributes).baseSize ?? attributes.size ?? 7) * 1.05)
    }),
    settings: {
      expansion: 1.55,
      gridSize: Math.max(20, Math.ceil(Math.sqrt(graph.order))),
      margin: 3,
      ratio: 1.55,
      speed: 2.2
    }
  });
  relaxProjectedNodeOverlaps(graph);
  expandLayoutToMinimumExtent(graph, Math.max(28, Math.sqrt(graph.order) * 7));
}

function forceAtlasIterations(order: number): number {
  if (order >= 400) {
    return 260;
  }
  if (order >= 160) {
    return 220;
  }
  return 160;
}

function expandLayoutToMinimumExtent(graph: SigmaDependencyGraph, minimumExtent: number): void {
  const extent = visibleSigmaNodeExtent(graph);
  if (!extent) {
    return;
  }

  const width = Math.max(0.001, extent.maxX - extent.minX);
  const height = Math.max(0.001, extent.maxY - extent.minY);
  const scale = Math.max(1, minimumExtent / width, minimumExtent / height);
  if (scale <= 1) {
    return;
  }

  const centerX = (extent.minX + extent.maxX) / 2;
  const centerY = (extent.minY + extent.maxY) / 2;
  graph.forEachNode((nodeId, attributes) => {
    graph.mergeNodeAttributes(nodeId, {
      x: centerX + (attributes.x - centerX) * scale,
      y: centerY + (attributes.y - centerY) * scale
    });
  });
}

function relaxProjectedNodeOverlaps(graph: SigmaDependencyGraph): void {
  const positions = graph.nodes().map((nodeId) => {
    const attributes = graph.getNodeAttributes(nodeId);
    return {
      nodeId,
      x: attributes.x,
      y: attributes.y,
      size: attributes.baseSize
    };
  });
  const iterations = graph.order >= 300 ? 90 : 140;

  for (let iteration = 0; iteration < iterations; iteration += 1) {
    const coordinateToPixel = projectedCoordinateToPixelScale(positions);
    const deltas = positions.map(() => ({ x: 0, y: 0 }));
    let maxOverlap = 0;

    for (let leftIndex = 0; leftIndex < positions.length; leftIndex += 1) {
      for (let rightIndex = leftIndex + 1; rightIndex < positions.length; rightIndex += 1) {
        const left = positions[leftIndex];
        const right = positions[rightIndex];
        let deltaX = right.x - left.x;
        let deltaY = right.y - left.y;
        let distance = Math.hypot(deltaX, deltaY);

        if (distance < 0.001) {
          const angle = ((leftIndex * 37 + rightIndex * 17) % 360) * (Math.PI / 180);
          deltaX = Math.cos(angle);
          deltaY = Math.sin(angle);
          distance = 1;
        }

        const requiredDistance = left.size + right.size + PROJECTED_NODE_MARGIN;
        const overlap = requiredDistance - distance * coordinateToPixel;
        if (overlap <= 0) {
          continue;
        }

        maxOverlap = Math.max(maxOverlap, overlap);
        const push = (overlap / coordinateToPixel) * 0.52;
        const unitX = deltaX / distance;
        const unitY = deltaY / distance;
        deltas[leftIndex].x -= unitX * push;
        deltas[leftIndex].y -= unitY * push;
        deltas[rightIndex].x += unitX * push;
        deltas[rightIndex].y += unitY * push;
      }
    }

    for (let index = 0; index < positions.length; index += 1) {
      positions[index].x += deltas[index].x * 0.58;
      positions[index].y += deltas[index].y * 0.58;
    }

    if (maxOverlap < 0.25) {
      break;
    }
  }

  for (const position of positions) {
    graph.mergeNodeAttributes(position.nodeId, {
      x: position.x,
      y: position.y
    });
  }
}

function projectedCoordinateToPixelScale(positions: Array<{ x: number; y: number }>): number {
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;

  for (const position of positions) {
    minX = Math.min(minX, position.x);
    maxX = Math.max(maxX, position.x);
    minY = Math.min(minY, position.y);
    maxY = Math.max(maxY, position.y);
  }

  const span = Math.max(maxX - minX, maxY - minY, 0.001);
  return PROJECTED_CANVAS_SPAN / span;
}

function dependencyLane(attributes: SigmaNodeAttributes): number {
  if (attributes.root) {
    return 0;
  }
  if (attributes.shared) {
    return Math.max(2, attributes.depth + 1);
  }
  return Math.max(1, attributes.depth);
}

function centerOutOffset(index: number): number {
  if (index === 0) {
    return 0;
  }
  const magnitude = Math.ceil(index / 2);
  return index % 2 === 1 ? magnitude : -magnitude;
}

function assignConcentricLayout(graph: SigmaDependencyGraph): void {
  const rings = new Map<number, Array<{ id: string; label: string; fanIn: number; outDegree: number; groupId: string }>>();

  graph.forEachNode((id, attributes) => {
    const outDegree = graph.outDegree(id);
    const ring = hubRing(attributes, outDegree);
    rings.set(ring, [...(rings.get(ring) ?? []), { id, label: attributes.baseLabel, fanIn: attributes.fanIn, outDegree, groupId: attributes.groupId }]);
  });

  for (const [ring, nodes] of [...rings.entries()].sort((left, right) => left[0] - right[0])) {
    const sorted = nodes.sort((left, right) => {
      if (left.fanIn !== right.fanIn) return right.fanIn - left.fanIn;
      if (left.outDegree !== right.outDegree) return right.outDegree - left.outDegree;
      if (left.groupId !== right.groupId) return left.groupId.localeCompare(right.groupId);
      return left.label.localeCompare(right.label);
    });
    sorted.forEach((node, index) => {
      const baseRadius = ring === 0 ? 0 : hubRingRadius(ring);
      const nodesPerCircle = nodesPerHubCircle(baseRadius);
      const subring = ring === 0 ? 0 : Math.floor(index / nodesPerCircle);
      const indexInSubring = ring === 0 ? 0 : index % nodesPerCircle;
      const nodesInSubring = ring === 0 ? 1 : Math.min(nodesPerCircle, sorted.length - subring * nodesPerCircle);
      const radius = ring === 0 ? 0 : baseRadius + subring * hubSubringSpacing(ring);
      const angle = ring === 0 ? 0 : (Math.PI * 2 * indexInSubring) / nodesInSubring + subring * 0.19;
      graph.mergeNodeAttributes(node.id, {
        x: ring === 0 ? 0 : Math.cos(angle) * radius,
        y: ring === 0 ? 0 : Math.sin(angle) * radius
      });
    });
  }
}

function hubRingRadius(ring: number): number {
  if (ring === 1) {
    return 24;
  }
  if (ring === 2) {
    return 42;
  }
  if (ring === 3) {
    return 76;
  }
  return 110;
}

function nodesPerHubCircle(radius: number): number {
  return Math.max(8, Math.floor((Math.PI * 2 * Math.max(radius, 12)) / 8.4));
}

function hubSubringSpacing(ring: number): number {
  if (ring === 1) {
    return 9;
  }
  return 10;
}

function hubRing(attributes: SigmaNodeAttributes, outDegree: number): number {
  if (attributes.root || attributes.moduleRoot) {
    return 0;
  }
  if (attributes.fanIn >= 8 || outDegree >= 12) {
    return 1;
  }
  if (attributes.fanIn > 1 || outDegree >= 6) {
    return 2;
  }
  if (attributes.depth <= 1) {
    return 3;
  }
  return 4;
}

function assignGroupLayout(graph: SigmaDependencyGraph): void {
  const groups = new Map<string, Array<{ id: string; label: string; fanIn: number; outDegree: number; root: boolean }>>();

  graph.forEachNode((id, attributes) => {
    const key = attributes.root ? "__root" : visualCommunityId(attributes.groupId);
    groups.set(key, [
      ...(groups.get(key) ?? []),
      { id, label: attributes.baseLabel, fanIn: attributes.fanIn, outDegree: graph.outDegree(id), root: attributes.root }
    ]);
  });

  const rootNodes = groups.get("__root") ?? [];
  for (const node of rootNodes) {
    graph.mergeNodeAttributes(node.id, { x: 0, y: 0 });
  }
  groups.delete("__root");

  const sortedGroups = [...groups.entries()].sort((left, right) => {
    if (left[1].length !== right[1].length) return right[1].length - left[1].length;
    return left[0].localeCompare(right[0]);
  });
  const placedCommunities = [{ x: 0, y: 0, radius: 9 }];

  sortedGroups.forEach(([communityId, nodes], groupIndex) => {
    const radius = communityRadius(nodes.length);
    const center = placeCommunity(placedCommunities, radius, groupIndex, communityId);
    placedCommunities.push({ ...center, radius });
    const sorted = [...nodes].sort((left, right) => {
      if (left.fanIn !== right.fanIn) return right.fanIn - left.fanIn;
      if (left.outDegree !== right.outDegree) return right.outDegree - left.outDegree;
      return left.label.localeCompare(right.label);
    });
    const nodeColumns = Math.max(1, Math.ceil(Math.sqrt(sorted.length * 1.2)));
    const rows = Math.max(1, Math.ceil(sorted.length / nodeColumns));
    const spacing = sorted.length === 1 ? 0 : Math.max(4.3, Math.min(6.2, (radius * 1.55) / Math.max(nodeColumns, rows)));

    sorted.forEach((node, index) => {
      const column = index % nodeColumns;
      const row = Math.floor(index / nodeColumns);
      const localX = (column - (nodeColumns - 1) / 2) * spacing;
      const localY = (row - (rows - 1) / 2) * spacing;
      graph.mergeNodeAttributes(node.id, {
        x: center.x + localX,
        y: center.y + localY
      });
    });
  });
}

function visualCommunityId(groupId: string): string {
  const sanitized = groupId.trim().replace(/\.$/, "");
  if (!sanitized.includes(".")) {
    return sanitized;
  }

  const parts = sanitized.split(".").filter(Boolean);
  if (parts.length <= 2) {
    return sanitized;
  }

  const [root, vendor, product] = parts;
  if (root === "jakarta") {
    return "jakarta";
  }
  if (root === "io") {
    return [root, vendor].join(".");
  }
  if (root === "com") {
    if (vendor === "fasterxml" && product === "jackson") {
      return "com.fasterxml.jackson";
    }
    if (vendor === "google" || vendor === "squareup") {
      return [root, vendor].join(".");
    }
    return [root, vendor, product].join(".");
  }
  if (root === "org") {
    if (vendor === "eclipse" && product === "microprofile") {
      return "org.eclipse.microprofile";
    }
    return [root, vendor].join(".");
  }

  return [root, vendor].join(".");
}

function communityRadius(nodeCount: number): number {
  return Math.max(11, Math.sqrt(nodeCount) * 4.8 + 5);
}

function placeCommunity(
  placedCommunities: Array<{ x: number; y: number; radius: number }>,
  radius: number,
  groupIndex: number,
  communityId: string
): { x: number; y: number } {
  const goldenAngle = Math.PI * (3 - Math.sqrt(5));
  const idOffset = stableCommunityOffset(communityId);

  for (let candidateIndex = 0; candidateIndex < 2400; candidateIndex += 1) {
    const spiralIndex = candidateIndex + 1;
    const angle = spiralIndex * goldenAngle + idOffset + groupIndex * 0.14;
    const distanceFromRoot = 10 + Math.sqrt(spiralIndex) * 5.7;
    const candidate = {
      x: Math.cos(angle) * distanceFromRoot,
      y: Math.sin(angle) * distanceFromRoot
    };

    if (communityDoesNotOverlap(candidate, radius, placedCommunities)) {
      return candidate;
    }
  }

  const fallbackAngle = groupIndex * goldenAngle + idOffset;
  const fallbackDistance = 40 + groupIndex * (radius + 8);
  return {
    x: Math.cos(fallbackAngle) * fallbackDistance,
    y: Math.sin(fallbackAngle) * fallbackDistance
  };
}

function communityDoesNotOverlap(
  candidate: { x: number; y: number },
  radius: number,
  placedCommunities: Array<{ x: number; y: number; radius: number }>
): boolean {
  return placedCommunities.every((placed) => Math.hypot(candidate.x - placed.x, candidate.y - placed.y) >= radius + placed.radius + 4.8);
}

function stableCommunityOffset(communityId: string): number {
  let hash = 0;
  for (let index = 0; index < communityId.length; index += 1) {
    hash = (hash * 31 + communityId.charCodeAt(index)) >>> 0;
  }
  return ((hash % 360) * Math.PI) / 180;
}

function closedNeighborhood(adjacency: Adjacency, nodeId: string): Set<string> {
  return new Set([nodeId, ...(adjacency.parents.get(nodeId) ?? []), ...(adjacency.children.get(nodeId) ?? [])]);
}
