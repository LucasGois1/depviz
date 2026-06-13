import { useState } from "react";
import { AlertTriangle, Boxes, GitFork, Info } from "lucide-react";
import type { Adjacency, DepvizDocument, FilterState, GraphNode, VisibilityState } from "../types";
import { DetailsPanel } from "./DetailsPanel";
import { DiagnosticsPanel } from "./DiagnosticsPanel";
import { PathsPanel } from "./PathsPanel";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";

interface SidebarProps {
  document: DepvizDocument;
  adjacency: Adjacency;
  nodeById: Map<string, GraphNode>;
  selectedNode: GraphNode | null;
  filters: FilterState;
  visibility: VisibilityState;
  onSelectNode: (nodeId: string | null) => void;
  onToggleCollapse: (node: GraphNode) => void;
}

export function Sidebar({
  document,
  adjacency,
  nodeById,
  selectedNode,
  filters,
  visibility,
  onSelectNode,
  onToggleCollapse
}: SidebarProps) {
  const [tab, setTab] = useState("details");
  const topGroups = Object.entries(document.summary.nodesByGroupId)
    .sort((left, right) => right[1] - left[1])
    .slice(0, 6);

  return (
    <aside className="sidebar">
      <section className="summary-card">
        <div className="summary-heading">
          <Boxes aria-hidden="true" />
          <div>
            <h2>Graph Summary</h2>
            <p>{new Date(document.generatedAt).toLocaleString()}</p>
          </div>
        </div>
        <div className="summary-grid">
          <SummaryStat label="Nodes" value={document.summary.nodeCount} />
          <SummaryStat label="Edges" value={document.summary.edgeCount} />
          <SummaryStat label="Visible" value={visibility.visibleNodeIds.size} />
          <SummaryStat label="Paths" value={document.paths.length} />
        </div>
        <div className="scope-list">
          {Object.entries(document.summary.nodesByScope).map(([scope, count]) => (
            <span key={scope}>
              {scope} <strong>{count}</strong>
            </span>
          ))}
        </div>
        <div className="top-groups">
          {topGroups.map(([groupId, count]) => (
            <div key={groupId}>
              <span>{groupId}</span>
              <strong>{count}</strong>
            </div>
          ))}
        </div>
      </section>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="details">
            <Info aria-hidden="true" /> Details
          </TabsTrigger>
          <TabsTrigger value="paths">
            <GitFork aria-hidden="true" /> Paths
          </TabsTrigger>
          <TabsTrigger value="diagnostics">
            <AlertTriangle aria-hidden="true" /> Diagnostics
          </TabsTrigger>
        </TabsList>
        <TabsContent value="details">
          <DetailsPanel
            adjacency={adjacency}
            nodeById={nodeById}
            selectedNode={selectedNode}
            filters={filters}
            onSelectNode={onSelectNode}
            onToggleCollapse={onToggleCollapse}
          />
        </TabsContent>
        <TabsContent value="paths">
          <PathsPanel document={document} nodeById={nodeById} selectedNode={selectedNode} onSelectNode={onSelectNode} />
        </TabsContent>
        <TabsContent value="diagnostics">
          <DiagnosticsPanel document={document} nodeById={nodeById} selectedNode={selectedNode} onSelectNode={onSelectNode} />
        </TabsContent>
      </Tabs>
    </aside>
  );
}

function SummaryStat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value.toLocaleString()}</strong>
    </div>
  );
}
