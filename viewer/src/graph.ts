import type { Adjacency, DepvizDocument, GraphNode, LayoutName } from "./types";

export interface CytoscapeElement {
  group: "nodes" | "edges";
  data: Record<string, string | number | boolean | null>;
  classes?: string;
}

export function toCytoscapeElements(document: DepvizDocument): CytoscapeElement[] {
  const nodeElements = document.nodes.map((node) => ({
    group: "nodes" as const,
    data: {
      id: node.id,
      label: node.label,
      coordinate: node.coordinate,
      groupId: node.groupId,
      artifactId: node.artifactId,
      version: node.version,
      type: node.type,
      classifier: node.classifier,
      scope: node.scope,
      optional: node.optional,
      depth: node.depth,
      root: node.root,
      moduleRoot: node.moduleRoot,
      groupColorKey: node.groupColorKey,
      hue: colorHue(node.groupColorKey)
    },
    classes: [
      node.root ? "root" : "",
      node.moduleRoot ? "module-root" : "",
      node.optional ? "optional" : "",
      `scope-${safeClassName(node.scope)}`
    ]
      .filter(Boolean)
      .join(" ")
  }));

  const edgeElements = document.edges.map((edge) => ({
    group: "edges" as const,
    data: {
      id: edge.id,
      source: edge.source,
      target: edge.target,
      scope: edge.scope,
      optional: edge.optional,
      depth: edge.depth
    },
    classes: [edge.optional ? "optional" : "", `scope-${safeClassName(edge.scope)}`].filter(Boolean).join(" ")
  }));

  return [...nodeElements, ...edgeElements];
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
      return "Force";
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

function colorHue(input: string): number {
  let hash = 0;
  for (let index = 0; index < input.length; index += 1) {
    hash = (hash * 31 + input.charCodeAt(index)) % 360;
  }
  return hash;
}

function safeClassName(input: string): string {
  return input.toLowerCase().replace(/[^a-z0-9_-]+/g, "-");
}
