import { Check, ChevronRight, Clipboard, Package, Split, X } from "lucide-react";
import { useState } from "react";
import { copyTextToClipboard, type ClipboardCopyState } from "../clipboard";
import type { Adjacency, FilterState, GraphNode } from "../types";
import { Button } from "./ui/button";

interface DetailsPanelProps {
  adjacency: Adjacency;
  nodeById: Map<string, GraphNode>;
  selectedNode: GraphNode | null;
  filters: FilterState;
  onSelectNode: (nodeId: string) => void;
  onToggleCollapse: (node: GraphNode) => void;
}

export function DetailsPanel({ adjacency, nodeById, selectedNode, filters, onSelectNode, onToggleCollapse }: DetailsPanelProps) {
  const [copyState, setCopyState] = useState<ClipboardCopyState | "idle">("idle");

  if (!selectedNode) {
    return (
      <div className="panel-empty">
        <Package aria-hidden="true" />
        <strong>Select a dependency</strong>
        <span>Click a node to inspect coordinates, immediate neighbors, and paths.</span>
      </div>
    );
  }

  const parents = idsToNodes(adjacency.parents.get(selectedNode.id), nodeById);
  const children = idsToNodes(adjacency.children.get(selectedNode.id), nodeById);
  const collapsed = filters.collapsedNodeIds.has(selectedNode.id);

  async function copyCoordinate() {
    if (!selectedNode) {
      return;
    }
    const nextState = await copyTextToClipboard(selectedNode.coordinate, navigator.clipboard);
    setCopyState(nextState);
    window.setTimeout(() => setCopyState("idle"), 1400);
  }

  return (
    <div className="details-panel">
      <section className="selected-header">
        <div>
          <span className="node-scope">{selectedNode.scope}</span>
          <h2>{selectedNode.label}</h2>
          <p>{selectedNode.coordinate}</p>
        </div>
        <div className="header-actions">
          <Button
            variant="outline"
            size="icon"
            onClick={copyCoordinate}
            title={copyState === "blocked" ? "Clipboard permission blocked" : "Copy coordinate"}
            aria-label="Copy coordinate"
          >
            {copyState === "copied" ? <Check aria-hidden="true" /> : <Clipboard aria-hidden="true" />}
          </Button>
          <Button
            variant={collapsed ? "secondary" : "outline"}
            size="icon"
            onClick={() => onToggleCollapse(selectedNode)}
            title="Collapse or expand outgoing branch"
            aria-label={collapsed ? "Expand outgoing branch" : "Collapse outgoing branch"}
          >
            {collapsed ? <ChevronRight aria-hidden="true" /> : <Split aria-hidden="true" />}
          </Button>
          {copyState !== "idle" ? (
            <span className={`copy-feedback copy-feedback-${copyState}`} role="status">
              {copyState === "copied" ? "Copied" : "Copy blocked"}
            </span>
          ) : null}
        </div>
      </section>

      <section className="property-grid">
        <Property label="Group" value={selectedNode.groupId} />
        <Property label="Artifact" value={selectedNode.artifactId} />
        <Property label="Version" value={selectedNode.version} />
        <Property label="Type" value={selectedNode.type} />
        <Property label="Classifier" value={selectedNode.classifier || "-"} />
        <Property label="Depth" value={String(selectedNode.depth)} />
        <Property label="Optional" value={selectedNode.optional ? "yes" : "no"} />
        <Property label="Root" value={selectedNode.root ? "yes" : "no"} />
      </section>

      <NeighborList title="Depended on by" nodes={parents} empty="No visible parents. This is a root or detached node." onSelectNode={onSelectNode} />
      <NeighborList title="Depends on" nodes={children} empty="No child dependencies recorded." onSelectNode={onSelectNode} />

      <section className="node-flags">
        {selectedNode.optional ? <Flag tone="warning" label="optional" /> : <Flag tone="success" label="required" />}
        {parents.length > 1 ? <Flag tone="neutral" label={`${parents.length} parents`} /> : null}
        {selectedNode.root ? <Flag tone="neutral" label="root" /> : null}
        {selectedNode.moduleRoot ? <Flag tone="neutral" label="module root" /> : null}
        {collapsed ? <Flag tone="warning" label="collapsed" /> : null}
      </section>
    </div>
  );
}

function Property({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function NeighborList({
  title,
  nodes,
  empty,
  onSelectNode
}: {
  title: string;
  nodes: GraphNode[];
  empty: string;
  onSelectNode: (nodeId: string) => void;
}) {
  return (
    <section className="neighbor-section">
      <h3>{title}</h3>
      {nodes.length === 0 ? (
        <p className="muted-row">{empty}</p>
      ) : (
        <div className="neighbor-list">
          {nodes.map((node) => (
            <button key={node.id} type="button" onClick={() => onSelectNode(node.id)}>
              <span>
                {node.label}
                {node.optional ? <em> optional</em> : null}
              </span>
              <small>{node.version}</small>
            </button>
          ))}
        </div>
      )}
    </section>
  );
}

function Flag({ tone, label }: { tone: "success" | "warning" | "neutral"; label: string }) {
  return (
    <span className={`flag flag-${tone}`}>
      {tone === "success" ? <Check aria-hidden="true" /> : tone === "warning" ? <X aria-hidden="true" /> : null}
      {label}
    </span>
  );
}

function idsToNodes(ids: Set<string> | undefined, nodeById: Map<string, GraphNode>): GraphNode[] {
  return [...(ids ?? [])].map((id) => nodeById.get(id)).filter((node): node is GraphNode => Boolean(node));
}
