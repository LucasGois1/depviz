export interface DepvizDocument {
  schemaVersion: string;
  generatedAt: string;
  project: ProjectInfo;
  summary: GraphSummary;
  versionSummary?: VersionSummary;
  securitySummary?: SecuritySummary | null;
  viewerConfig: ViewerConfig;
  nodes: GraphNode[];
  edges: GraphEdge[];
  paths: GraphPath[];
  diagnostics: DiagnosticEntry[];
}

export interface ProjectInfo {
  groupId: string;
  artifactId: string;
  version: string;
  packaging: string;
  name: string;
  baseDirectory: string;
  multiModule: boolean;
  modules: string[];
}

export interface GraphSummary {
  nodeCount: number;
  edgeCount: number;
  nodesByScope: Record<string, number>;
  nodesByGroupId: Record<string, number>;
}

export interface ViewerConfig {
  initialLayout: LayoutName;
  maxInitialLabels: number;
  nodeMode: string;
}

export interface GraphNode {
  id: string;
  groupId: string;
  artifactId: string;
  version: string;
  type: string;
  classifier: string;
  scope: string;
  optional: boolean;
  depth: number;
  root: boolean;
  moduleRoot: boolean;
  label: string;
  coordinate: string;
  groupColorKey: string;
  versionInsight?: VersionInsight | null;
  securityInsight?: SecurityInsight | null;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  scope: string;
  optional: boolean;
  depth: number;
}

export interface GraphPath {
  target: string;
  nodeIds: string[];
}

export interface DiagnosticEntry {
  severity: string;
  type: string;
  message: string;
  nodeId: string | null;
}

export type LayoutName = "breadthfirst" | "force" | "circle" | "concentric";

export type OptionalMode = "all" | "required" | "optional";

export type UpdateFilterMode = "all" | "outdated" | "major" | "minor" | "patch" | "unknown" | "unavailable";

export type SecuritySeverity = "critical" | "high" | "medium" | "low";

export type SecurityFilterMode = "all" | "vulnerable" | SecuritySeverity;

export interface VersionInsight {
  currentVersion: string;
  latestVersion: string | null;
  updateType: "patch" | "minor" | "major" | "unknown" | "none";
  status: "current" | "outdated" | "unavailable" | "unchecked";
  checked: boolean;
  message: string | null;
}

export interface VersionSummary {
  enabled: boolean;
  checked: number;
  current: number;
  outdated: number;
  patch: number;
  minor: number;
  major: number;
  unknown: number;
  unavailable: number;
}

export interface SecurityFinding {
  id: string;
  severity: SecuritySeverity;
  title: string;
  packageName: string;
  version: string;
  fixedVersions: string[];
  url: string;
}

export interface SecurityInsight {
  status: "not-vulnerable" | "vulnerable" | "unavailable" | "unchecked";
  maxSeverity: SecuritySeverity;
  vulnerabilityCount: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
  source: string;
  findings: SecurityFinding[];
}

export interface SecuritySummary {
  enabled: boolean;
  source: string;
  checked: boolean;
  vulnerableNodes: number;
  affectedModules: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
  unmappedFindings: number;
}

export interface FilterState {
  search: string;
  scopes: Set<string>;
  optionalMode: OptionalMode;
  updateMode: UpdateFilterMode;
  securityMode: SecurityFilterMode;
  collapsedNodeIds: Set<string>;
}

export interface VisibilityState {
  visibleNodeIds: Set<string>;
  visibleEdgeIds: Set<string>;
  matchingNodeIds: Set<string>;
}

export interface Adjacency {
  parents: Map<string, Set<string>>;
  children: Map<string, Set<string>>;
  incomingEdges: Map<string, GraphEdge[]>;
  outgoingEdges: Map<string, GraphEdge[]>;
}
