import type { Adjacency, DepvizDocument, GraphNode, LayoutName } from "./types";

export function hasSharedDependencies(document: DepvizDocument): boolean {
  return [...dependencyFanIn(document).values()].some((fanIn) => fanIn > 1);
}

export function recommendedInitialLayout(document: DepvizDocument, configuredLayout: LayoutName): LayoutName {
  if (configuredLayout === "breadthfirst" && hasSharedDependencies(document)) {
    return "force";
  }
  return configuredLayout;
}

export function shouldShowAllLabelsInitially(document: DepvizDocument): boolean {
  return document.nodes.length <= document.viewerConfig.maxInitialLabels && !hasSharedDependencies(document);
}

export function buildAdjacency(document: DepvizDocument): Adjacency {
  const parents = new Map<string, Set<string>>();
  const children = new Map<string, Set<string>>();
  const incomingEdges = new Map<string, typeof document.edges>();
  const outgoingEdges = new Map<string, typeof document.edges>();

  for (const node of document.nodes) {
    parents.set(node.id, new Set());
    children.set(node.id, new Set());
    incomingEdges.set(node.id, []);
    outgoingEdges.set(node.id, []);
  }

  for (const edge of document.edges) {
    if (!parents.has(edge.target)) {
      parents.set(edge.target, new Set());
    }
    if (!children.has(edge.source)) {
      children.set(edge.source, new Set());
    }
    parents.get(edge.target)?.add(edge.source);
    children.get(edge.source)?.add(edge.target);
    incomingEdges.get(edge.target)?.push(edge);
    outgoingEdges.get(edge.source)?.push(edge);
  }

  return { parents, children, incomingEdges, outgoingEdges };
}

export function dependencyFanIn(document: DepvizDocument): Map<string, number> {
  const incomingSources = new Map<string, Set<string>>();

  for (const node of document.nodes) {
    incomingSources.set(node.id, new Set());
  }

  for (const edge of document.edges) {
    if (!incomingSources.has(edge.target)) {
      incomingSources.set(edge.target, new Set());
    }
    incomingSources.get(edge.target)?.add(edge.source);
  }

  return new Map([...incomingSources.entries()].map(([nodeId, sources]) => [nodeId, sources.size]));
}

export function nodeSearchText(node: GraphNode): string {
  return [
    node.id,
    node.coordinate,
    node.groupId,
    node.artifactId,
    node.version,
    node.scope,
    node.type,
    node.classifier,
    node.label
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

export function layoutDisplayName(layout: LayoutName): string {
  switch (layout) {
    case "breadthfirst":
      return "Breadthfirst";
    case "force":
      return "Map";
    case "circle":
      return "Circle";
    case "concentric":
      return "Concentric";
  }
}

export function availableScopes(document: DepvizDocument): string[] {
  return Object.keys(document.summary.nodesByScope).sort((left, right) => {
    if (left === "root") return -1;
    if (right === "root") return 1;
    return left.localeCompare(right);
  });
}
