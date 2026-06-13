export interface DepvizDocument {
  schemaVersion: string;
  generatedAt: string;
  project: ProjectInfo;
  summary: GraphSummary;
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

export interface FilterState {
  search: string;
  scopes: Set<string>;
  optionalMode: OptionalMode;
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
