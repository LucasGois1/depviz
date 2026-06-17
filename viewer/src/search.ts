import { dependencyFanIn, nodeSearchText } from "./graph";
import type { DepvizDocument, GraphNode, SecuritySeverity, VisibilityState } from "./types";

export interface SearchSuggestionBadge {
  label: string;
  tone: "scope" | "direct" | "shared" | "update" | "security";
}

export interface SearchSuggestion {
  nodeId: string;
  artifactId: string;
  coordinate: string;
  description: string;
  badges: SearchSuggestionBadge[];
  score: number;
}

const DEFAULT_SUGGESTION_LIMIT = 6;

export function buildSearchSuggestions(
  document: DepvizDocument,
  visibility: VisibilityState,
  query: string,
  limit = DEFAULT_SUGGESTION_LIMIT
): SearchSuggestion[] {
  const normalizedQuery = query.trim().toLowerCase();
  if (!normalizedQuery) {
    return [];
  }

  const fanInByNodeId = dependencyFanIn(document);

  return document.nodes
    .filter((node) => visibility.matchingNodeIds.has(node.id) && visibility.visibleNodeIds.has(node.id))
    .filter((node) => nodeSearchText(node).includes(normalizedQuery))
    .map((node) => toSearchSuggestion(node, normalizedQuery, fanInByNodeId.get(node.id) ?? 0))
    .sort((left, right) => right.score - left.score || left.artifactId.localeCompare(right.artifactId) || left.coordinate.localeCompare(right.coordinate))
    .slice(0, Math.max(0, limit));
}

export function nextSuggestionIndex(currentIndex: number, suggestionCount: number, direction: -1 | 1): number {
  if (suggestionCount <= 0) {
    return -1;
  }
  if (currentIndex < 0 || currentIndex >= suggestionCount) {
    return direction > 0 ? 0 : suggestionCount - 1;
  }
  return (currentIndex + direction + suggestionCount) % suggestionCount;
}

function toSearchSuggestion(node: GraphNode, normalizedQuery: string, fanIn: number): SearchSuggestion {
  const direct = node.root || node.moduleRoot || node.depth <= 1;
  const shared = fanIn > 1;
  const badges = suggestionBadges(node, direct, shared);

  return {
    nodeId: node.id,
    artifactId: node.artifactId,
    coordinate: node.coordinate,
    description: suggestionDescription(node, direct, shared, fanIn),
    badges,
    score: suggestionScore(node, normalizedQuery, direct, shared)
  };
}

function suggestionScore(node: GraphNode, normalizedQuery: string, direct: boolean, shared: boolean): number {
  const artifactId = node.artifactId.toLowerCase();
  const coordinate = node.coordinate.toLowerCase();
  const groupId = node.groupId.toLowerCase();
  let score = 0;

  if (artifactId === normalizedQuery) {
    score += 900;
  } else if (artifactId.startsWith(normalizedQuery)) {
    score += 700;
  } else if (artifactId.includes(normalizedQuery)) {
    score += 520;
  } else if (coordinate.includes(normalizedQuery)) {
    score += 360;
  } else if (groupId.includes(normalizedQuery)) {
    score += 260;
  }

  if (direct) {
    score += 120;
  }
  if (shared) {
    score += 70;
  }
  if (node.securityInsight?.status === "vulnerable") {
    score += severityScore(node.securityInsight.maxSeverity);
  }
  if (node.versionInsight?.status === "outdated") {
    score += updateScore(node.versionInsight.updateType);
  }

  return score;
}

function suggestionBadges(node: GraphNode, direct: boolean, shared: boolean): SearchSuggestionBadge[] {
  const badges: SearchSuggestionBadge[] = [{ label: node.scope, tone: "scope" }];

  if (direct) {
    badges.push({ label: "direct", tone: "direct" });
  } else if (shared) {
    badges.push({ label: "shared", tone: "shared" });
  }

  if (node.securityInsight?.status === "vulnerable") {
    badges.push({ label: node.securityInsight.maxSeverity, tone: "security" });
  }
  if (node.versionInsight?.status === "outdated" && node.versionInsight.updateType !== "none") {
    badges.push({ label: node.versionInsight.updateType, tone: "update" });
  }

  return badges.slice(0, 4);
}

function suggestionDescription(node: GraphNode, direct: boolean, shared: boolean, fanIn: number): string {
  const relation = direct ? "direct dependency" : shared ? `shared by ${fanIn} dependencies` : "transitive dependency";
  return `${node.groupId} - ${node.version} - ${relation}`;
}

function updateScore(updateType: string): number {
  if (updateType === "major") {
    return 55;
  }
  if (updateType === "minor") {
    return 35;
  }
  if (updateType === "patch") {
    return 20;
  }
  return 10;
}

function severityScore(severity: SecuritySeverity): number {
  if (severity === "critical") {
    return 60;
  }
  if (severity === "high") {
    return 45;
  }
  if (severity === "medium") {
    return 30;
  }
  return 15;
}
