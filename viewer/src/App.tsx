import { useCallback, useMemo, useState } from "react";
import { AlertTriangle } from "lucide-react";
import { buildVisibility, createFilterState, setOptionalMode, setScopeEnabled, setSecurityMode, setUpdateMode, toggleCollapsed } from "./filtering";
import { availableScopes, buildAdjacency, recommendedInitialLayout, shouldShowAllLabelsInitially } from "./graph";
import type { DepvizDocument, FilterState, GraphNode, LayoutName, OptionalMode, SecurityFilterMode, UpdateFilterMode } from "./types";
import { GraphCanvas } from "./components/GraphCanvas";
import { Sidebar } from "./components/Sidebar";
import { Toolbar } from "./components/Toolbar";
import { Button } from "./components/ui/button";

const fallbackDocument: DepvizDocument = {
  schemaVersion: "empty",
  generatedAt: new Date(0).toISOString(),
  project: {
    groupId: "unknown",
    artifactId: "dependency-graph",
    version: "0.0.0",
    packaging: "jar",
    name: "Dependency Graph",
    baseDirectory: "",
    multiModule: false,
    modules: []
  },
  summary: { nodeCount: 0, edgeCount: 0, nodesByScope: {}, nodesByGroupId: {} },
  viewerConfig: { initialLayout: "breadthfirst", maxInitialLabels: 500, nodeMode: "artifact" },
  nodes: [],
  edges: [],
  paths: [],
  diagnostics: [
    {
      severity: "warning",
      type: "viewer-data",
      message: "No embedded dependency graph data was found in #depviz-data.",
      nodeId: null
    }
  ]
};

export default function App() {
  const documentData = useMemo(readEmbeddedDocument, []);
  const [filters, setFilters] = useState<FilterState>(() => createFilterState());
  const [layout, setLayout] = useState<LayoutName>(() => recommendedInitialLayout(documentData, normalizeLayout(documentData.viewerConfig.initialLayout)));
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [showLabels, setShowLabels] = useState(() => shouldShowAllLabelsInitially(documentData));
  const [viewportCommand, setViewportCommand] = useState<"fit" | "reset" | null>(null);
  const [commandNonce, setCommandNonce] = useState(0);

  const nodeById = useMemo(() => new Map(documentData.nodes.map((node) => [node.id, node])), [documentData]);
  const adjacency = useMemo(() => buildAdjacency(documentData), [documentData]);
  const scopes = useMemo(() => availableScopes(documentData), [documentData]);
  const visibility = useMemo(() => buildVisibility(documentData, filters), [documentData, filters]);
  const selectedNode = selectedNodeId ? nodeById.get(selectedNodeId) ?? null : null;

  const selectNode = useCallback((nodeId: string | null) => {
    setSelectedNodeId(nodeId);
  }, []);

  const runViewportCommand = useCallback((command: "fit" | "reset") => {
    setViewportCommand(command);
    setCommandNonce((current) => current + 1);
  }, []);

  const updateSearch = useCallback((search: string) => {
    setFilters((current) => ({ ...current, search }));
  }, []);

  const updateScope = useCallback((scope: string, enabled: boolean) => {
    setFilters((current) => setScopeEnabled(current, scope, enabled, scopes));
  }, [scopes]);

  const updateOptionalMode = useCallback((optionalMode: OptionalMode) => {
    setFilters((current) => setOptionalMode(current, optionalMode));
  }, []);

  const updateUpdateMode = useCallback((updateMode: UpdateFilterMode) => {
    setFilters((current) => setUpdateMode(current, updateMode));
  }, []);

  const updateSecurityMode = useCallback((securityMode: SecurityFilterMode) => {
    setFilters((current) => setSecurityMode(current, securityMode));
  }, []);

  const clearFilters = useCallback(() => {
    setFilters(createFilterState());
  }, []);

  const toggleNodeCollapse = useCallback((node: GraphNode) => {
    setFilters((current) => toggleCollapsed(current, node.id));
  }, []);
  const fitGraph = useCallback(() => runViewportCommand("fit"), [runViewportCommand]);
  const resetGraph = useCallback(() => runViewportCommand("reset"), [runViewportCommand]);

  return (
    <main className="app-shell">
      <section className="workspace">
        <Toolbar
          document={documentData}
          filters={filters}
          layout={layout}
          scopes={scopes}
          showLabels={showLabels}
          onSearchChange={updateSearch}
          onScopeChange={updateScope}
          onOptionalModeChange={updateOptionalMode}
          onUpdateModeChange={updateUpdateMode}
          onSecurityModeChange={updateSecurityMode}
          onLayoutChange={setLayout}
          onShowLabelsChange={setShowLabels}
          onFit={fitGraph}
          onReset={resetGraph}
          onClearFilters={clearFilters}
        />
        <div className="canvas-frame">
          {documentData.nodes.length === 0 ? (
            <div className="empty-canvas">
              <AlertTriangle aria-hidden="true" />
              <strong>No graph data</strong>
              <span>The generated HTML did not include nodes to render.</span>
            </div>
          ) : (
            <GraphCanvas
              document={documentData}
              layout={layout}
              selectedNodeId={selectedNodeId}
              visibility={visibility}
              showLabels={showLabels}
              viewportCommand={viewportCommand}
              commandNonce={commandNonce}
              onSelectNode={selectNode}
            />
          )}
          <div className="canvas-status" aria-live="polite">
            <span>{visibility.visibleNodeIds.size} nodes visible</span>
            <span>{visibility.visibleEdgeIds.size} edges visible</span>
            {filters.search.trim() ? <span>{visibility.matchingNodeIds.size} matches</span> : null}
          </div>
        </div>
      </section>
      <Sidebar
        document={documentData}
        adjacency={adjacency}
        nodeById={nodeById}
        selectedNode={selectedNode}
        filters={filters}
        visibility={visibility}
        onSelectNode={selectNode}
        onToggleCollapse={toggleNodeCollapse}
      />
    </main>
  );
}

function readEmbeddedDocument(): DepvizDocument {
  const script = window.document.getElementById("depviz-data");
  if (!script?.textContent?.trim()) {
    return fallbackDocument;
  }
  try {
    return JSON.parse(script.textContent) as DepvizDocument;
  } catch (error) {
    return {
      ...fallbackDocument,
      diagnostics: [
        {
          severity: "error",
          type: "viewer-data",
          message: `Unable to parse embedded dependency graph data: ${error instanceof Error ? error.message : String(error)}`,
          nodeId: null
        }
      ]
    };
  }
}

function normalizeLayout(layout: string): LayoutName {
  if (layout === "force" || layout === "circle" || layout === "concentric" || layout === "breadthfirst") {
    return layout;
  }
  return "breadthfirst";
}
