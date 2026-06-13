import { useMemo, useState } from "react";
import { Route } from "lucide-react";
import type { DepvizDocument, GraphNode } from "../types";
import { Button } from "./ui/button";

interface PathsPanelProps {
  document: DepvizDocument;
  nodeById: Map<string, GraphNode>;
  selectedNode: GraphNode | null;
  onSelectNode: (nodeId: string) => void;
}

export function PathsPanel({ document, nodeById, selectedNode, onSelectNode }: PathsPanelProps) {
  const [visibleCount, setVisibleCount] = useState(6);
  const paths = useMemo(() => {
    if (!selectedNode) {
      return [];
    }
    return document.paths.filter((path) => path.target === selectedNode.id);
  }, [document.paths, selectedNode]);

  if (!selectedNode) {
    return (
      <div className="panel-empty">
        <Route aria-hidden="true" />
        <strong>No dependency selected</strong>
        <span>Select a node to inspect every recorded path to that artifact.</span>
      </div>
    );
  }

  if (paths.length === 0) {
    return (
      <div className="panel-empty">
        <Route aria-hidden="true" />
        <strong>No path records</strong>
        <span>The graph document did not include path metadata for this node.</span>
      </div>
    );
  }

  return (
    <section className="paths-panel">
      <div className="panel-title">
        <h2>Paths to {selectedNode.label}</h2>
        <span>{paths.length} total</span>
      </div>
      {paths.slice(0, visibleCount).map((path, index) => (
        <ol className="path-list" key={`${path.target}-${index}`}>
          {path.nodeIds.map((nodeId) => {
            const node = nodeById.get(nodeId);
            return (
              <li key={nodeId}>
                <button type="button" onClick={() => onSelectNode(nodeId)}>
                  <span>{node?.label ?? nodeId}</span>
                  <small>{node?.scope ?? "unknown"}</small>
                </button>
              </li>
            );
          })}
        </ol>
      ))}
      {visibleCount < paths.length ? (
        <Button variant="outline" size="sm" onClick={() => setVisibleCount((current) => current + 6)}>
          Show more paths
        </Button>
      ) : null}
    </section>
  );
}
