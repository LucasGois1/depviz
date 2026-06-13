import { useMemo, useState } from "react";
import { AlertTriangle } from "lucide-react";
import { buildVisibility, createFilterState, setOptionalMode, setScopeEnabled, toggleCollapsed } from "./filtering";
import { availableScopes, buildAdjacency } from "./graph";
import type { DepvizDocument, FilterState, GraphNode, LayoutName, OptionalMode } from "./types";
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
  const [layout, setLayout] = useState<LayoutName>(normalizeLayout(documentData.viewerConfig.initialLayout));
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(() => documentData.nodes.find((node) => node.root)?.id ?? null);
  const [showLabels, setShowLabels] = useState(documentData.nodes.length <= documentData.viewerConfig.maxInitialLabels);
  const [viewportCommand, setViewportCommand] = useState<"fit" | "reset" | null>(null);
  const [commandNonce, setCommandNonce] = useState(0);

  const nodeById = useMemo(() => new Map(documentData.nodes.map((node) => [node.id, node])), [documentData]);
  const adjacency = useMemo(() => buildAdjacency(documentData), [documentData]);
  const scopes = useMemo(() => availableScopes(documentData).filter((scope) => scope !== "root"), [documentData]);
  const visibility = useMemo(() => buildVisibility(documentData, filters), [documentData, filters]);
  const selectedNode = selectedNodeId ? nodeById.get(selectedNodeId) ?? null : null;

  function selectNode(nodeId: string | null) {
    setSelectedNodeId(nodeId);
  }

  function runViewportCommand(command: "fit" | "reset") {
    setViewportCommand(command);
    setCommandNonce((current) => current + 1);
  }

  function updateSearch(search: string) {
    setFilters((current) => ({ ...current, search }));
  }

  function updateScope(scope: string, enabled: boolean) {
    setFilters((current) => setScopeEnabled(current, scope, enabled));
  }

  function updateOptionalMode(optionalMode: OptionalMode) {
    setFilters((current) => setOptionalMode(current, optionalMode));
  }

  function clearFilters() {
    setFilters(createFilterState());
  }

  function toggleNodeCollapse(node: GraphNode) {
    setFilters((current) => toggleCollapsed(current, node.id));
  }

  return (
    <main className="app-shell">
      <section className="workspace">
        <Toolbar
          document={documentData}
          filters={filters}
          layout={layout}
          scopes={scopes}
          showLabels={showLabels}
          visibleCount={visibility.visibleNodeIds.size}
          onSearchChange={updateSearch}
          onScopeChange={updateScope}
          onOptionalModeChange={updateOptionalMode}
          onLayoutChange={setLayout}
          onShowLabelsChange={setShowLabels}
          onFit={() => runViewportCommand("fit")}
          onReset={() => runViewportCommand("reset")}
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

