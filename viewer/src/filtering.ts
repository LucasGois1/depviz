import { buildAdjacency, nodeSearchText } from "./graph";
import type {
  DepvizDocument,
  FilterState,
  GraphEdge,
  GraphNode,
  OptionalMode,
  SecurityFilterMode,
  UpdateFilterMode,
  VisibilityState
} from "./types";

export function createFilterState(): FilterState {
  return {
    search: "",
    scopes: new Set(),
    optionalMode: "all",
    updateMode: "all",
    securityMode: "all",
    collapsedNodeIds: new Set()
  };
}

export function buildVisibility(document: DepvizDocument, filters: FilterState): VisibilityState {
  const nodeById = new Map(document.nodes.map((node) => [node.id, node]));
  const adjacency = buildAdjacency(document);
  const baseAllowed = new Set(
    document.nodes.filter((node) => node.root || nodePassesOptionalFilter(node, filters)).map((node) => node.id)
  );
  const reachable = reachableFromRoots(document, filters, baseAllowed, nodeById);
  const updateFilteringEnabled = document.versionSummary?.enabled === true && filters.updateMode !== "all";
  const securityFilteringEnabled = document.securitySummary?.checked === true && filters.securityMode !== "all";
  const matchingNodeIds = matchingReachableNodes(document, filters, reachable, updateFilteringEnabled, securityFilteringEnabled);
  const hasNodeMatchingFilter = Boolean(filters.search.trim()) || updateFilteringEnabled || securityFilteringEnabled;
  const visibleNodeIds = hasNodeMatchingFilter
    ? expandSearchContext(matchingNodeIds, adjacency, reachable)
    : reachable;

  const visibleEdgeIds = new Set(
    document.edges
      .filter((edge) => edgePassesFilters(edge, filters))
      .filter((edge) => visibleNodeIds.has(edge.source) && visibleNodeIds.has(edge.target))
      .map((edge) => edge.id)
  );

  return { visibleNodeIds, visibleEdgeIds, matchingNodeIds };
}

export function toggleCollapsed(filters: FilterState, nodeId: string): FilterState {
  const collapsedNodeIds = new Set(filters.collapsedNodeIds);
  if (collapsedNodeIds.has(nodeId)) {
    collapsedNodeIds.delete(nodeId);
  } else {
    collapsedNodeIds.add(nodeId);
  }
  return { ...filters, collapsedNodeIds };
}

export function setOptionalMode(filters: FilterState, optionalMode: OptionalMode): FilterState {
  return { ...filters, optionalMode };
}

export function setUpdateMode(filters: FilterState, updateMode: UpdateFilterMode): FilterState {
  return { ...filters, updateMode };
}

export function setSecurityMode(filters: FilterState, securityMode: SecurityFilterMode): FilterState {
  return { ...filters, securityMode };
}

export function setScopeEnabled(filters: FilterState, scope: string, enabled: boolean): FilterState {
  const scopes = new Set(filters.scopes);
  if (enabled) {
    scopes.add(scope);
  } else {
    scopes.delete(scope);
  }
  return { ...filters, scopes };
}

function nodePassesOptionalFilter(node: GraphNode, filters: FilterState): boolean {
  if (filters.optionalMode === "required" && node.optional) {
    return false;
  }
  if (filters.optionalMode === "optional" && !node.optional) {
    return false;
  }
  return true;
}

function edgePassesFilters(edge: GraphEdge, filters: FilterState): boolean {
  if (edge.scope === "root" || edge.scope === "module") {
    return true;
  }
  if (filters.scopes.size > 0 && !filters.scopes.has(edge.scope)) {
    return false;
  }
  if (filters.optionalMode === "required" && edge.optional) {
    return false;
  }
  if (filters.optionalMode === "optional" && !edge.optional) {
    return false;
  }
  return true;
}

export function nodeMatchesUpdateMode(node: GraphNode, updateMode: UpdateFilterMode): boolean {
  if (updateMode === "all") {
    return true;
  }

  const insight = node.versionInsight;
  if (!insight) {
    return false;
  }

  if (updateMode === "outdated") {
    return insight.status === "outdated";
  }

  if (updateMode === "unavailable") {
    return insight.status === "unavailable";
  }

  if (updateMode === "unknown") {
    return insight.status !== "unavailable" && insight.updateType === "unknown";
  }

  return insight.status === "outdated" && insight.updateType === updateMode;
}

export function nodeMatchesSecurityMode(node: GraphNode, securityMode: SecurityFilterMode): boolean {
  if (securityMode === "all") {
    return true;
  }
  const insight = node.securityInsight;
  if (!insight || insight.status !== "vulnerable") {
    return false;
  }
  if (securityMode === "vulnerable") {
    return true;
  }
  return insight.maxSeverity === securityMode;
}

function reachableFromRoots(
  document: DepvizDocument,
  filters: FilterState,
  baseAllowed: Set<string>,
  nodeById: Map<string, GraphNode>
): Set<string> {
  const adjacency = buildAdjacency(document);
  const roots = document.nodes.filter((node) => node.root || node.depth === 0).map((node) => node.id);
  const visible = new Set<string>();
  const queue = [...roots.filter((rootId) => baseAllowed.has(rootId))];

  while (queue.length > 0) {
    const current = queue.shift();
    if (!current || visible.has(current)) {
      continue;
    }
    visible.add(current);
    if (filters.collapsedNodeIds.has(current)) {
      continue;
    }
    for (const edge of adjacency.outgoingEdges.get(current) ?? []) {
      const targetNode = nodeById.get(edge.target);
      if (
        !edgePassesFilters(edge, filters) ||
        !baseAllowed.has(edge.target) ||
        !targetNode ||
        !nodePassesOptionalFilter(targetNode, filters)
      ) {
        continue;
      }
      queue.push(edge.target);
    }
  }

  return visible;
}

function matchingReachableNodes(
  document: DepvizDocument,
  filters: FilterState,
  reachable: Set<string>,
  updateFilteringEnabled: boolean,
  securityFilteringEnabled: boolean
): Set<string> {
  const normalizedSearch = filters.search.trim().toLowerCase();
  if (!normalizedSearch && !updateFilteringEnabled && !securityFilteringEnabled) {
    return new Set();
  }
  return new Set(
    document.nodes
      .filter((node) => reachable.has(node.id))
      .filter((node) => !normalizedSearch || nodeSearchText(node).includes(normalizedSearch))
      .filter((node) => !updateFilteringEnabled || nodeMatchesUpdateMode(node, filters.updateMode))
      .filter((node) => !securityFilteringEnabled || nodeMatchesSecurityMode(node, filters.securityMode))
      .map((node) => node.id)
  );
}

function expandSearchContext(
  matchingNodeIds: Set<string>,
  adjacency: ReturnType<typeof buildAdjacency>,
  reachable: Set<string>
): Set<string> {
  const expanded = new Set<string>();
  for (const nodeId of matchingNodeIds) {
    expanded.add(nodeId);
    collect(nodeId, adjacency.parents, reachable, expanded);
    collect(nodeId, adjacency.children, reachable, expanded);
  }
  return expanded;
}

function collect(
  startId: string,
  graph: Map<string, Set<string>>,
  reachable: Set<string>,
  target: Set<string>
): void {
  const queue = [...(graph.get(startId) ?? [])];
  while (queue.length > 0) {
    const current = queue.shift();
    if (!current || target.has(current) || !reachable.has(current)) {
      continue;
    }
    target.add(current);
    queue.push(...(graph.get(current) ?? []));
  }
}
