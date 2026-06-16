import { useMemo, useState } from "react";
import { Route } from "lucide-react";
import type { DepvizDocument, GraphNode, GraphPath } from "../types";
import { Button } from "./ui/button";

interface PathsPanelProps {
  document: DepvizDocument;
  nodeById: Map<string, GraphNode>;
  selectedNode: GraphNode | null;
  onSelectNode: (nodeId: string) => void;
}

export function PathsPanel({ document, nodeById, selectedNode, onSelectNode }: PathsPanelProps) {
  const [visibleCount, setVisibleCount] = useState(6);
  const paths = useMemo(() => pathsForSelectedNode(document, nodeById, selectedNode), [document, nodeById, selectedNode]);

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
      <div className="path-panel-header">
        <div>
          <span>Route inspector</span>
          <h2>{selectedNode.artifactId}</h2>
        </div>
        <strong>{paths.length} paths</strong>
      </div>
      {paths.slice(0, visibleCount).map((path, index) => (
        <article className="path-card" key={path.id}>
          <header>
            <span>Path {index + 1}</span>
            <strong>
              {path.stepCount} steps · depth {path.depth}
            </strong>
          </header>
          <ol className="route-steps">
            {path.steps.map((step, stepIndex) => (
              <li className={`route-step route-step-${step.position}`} key={`${path.id}-${step.id}-${stepIndex}`}>
                <button type="button" onClick={() => onSelectNode(step.id)} title={step.coordinate}>
                  <span>{step.label}</span>
                  <small>
                    {step.scope}
                    {step.optional ? " optional" : ""}
                  </small>
                </button>
              </li>
            ))}
          </ol>
        </article>
      ))}
      {visibleCount < paths.length ? (
        <Button variant="outline" size="sm" onClick={() => setVisibleCount((current) => current + 6)}>
          Show more paths
        </Button>
      ) : null}
    </section>
  );
}

interface PathRouteCard {
  id: string;
  depth: number;
  stepCount: number;
  steps: PathRouteStep[];
}

interface PathRouteStep {
  id: string;
  label: string;
  scope: string;
  optional: boolean;
  coordinate: string;
  position: "entry" | "middle" | "target";
}

export function pathsForSelectedNode(document: DepvizDocument, nodeById: Map<string, GraphNode>, selectedNode: GraphNode | null): PathRouteCard[] {
  if (!selectedNode) {
    return [];
  }

  return document.paths
    .filter((path) => path.target === selectedNode.id)
    .map((path, index) => pathRouteCard(path, index, nodeById));
}

function pathRouteCard(path: GraphPath, index: number, nodeById: Map<string, GraphNode>): PathRouteCard {
  return {
    id: `${path.target}-${index}`,
    depth: Math.max(0, path.nodeIds.length - 1),
    stepCount: path.nodeIds.length,
    steps: path.nodeIds.map((nodeId, stepIndex) => {
      const node = nodeById.get(nodeId);
      return {
        id: nodeId,
        label: node?.label ?? nodeId,
        scope: node?.scope ?? "unknown",
        optional: node?.optional ?? false,
        coordinate: node?.coordinate ?? nodeId,
        position: stepPosition(stepIndex, path.nodeIds.length)
      };
    })
  };
}

function stepPosition(index: number, stepCount: number): PathRouteStep["position"] {
  if (index === 0) {
    return "entry";
  }
  if (index === stepCount - 1) {
    return "target";
  }
  return "middle";
}
