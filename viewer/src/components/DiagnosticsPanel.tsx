import { AlertCircle, AlertTriangle, Info, ShieldCheck } from "lucide-react";
import type { DepvizDocument, GraphNode } from "../types";

interface DiagnosticsPanelProps {
  document: DepvizDocument;
  nodeById: Map<string, GraphNode>;
  selectedNode: GraphNode | null;
  onSelectNode: (nodeId: string) => void;
}

export function DiagnosticsPanel({ document, nodeById, selectedNode, onSelectNode }: DiagnosticsPanelProps) {
  const diagnostics = selectedNode
    ? document.diagnostics.filter((diagnostic) => diagnostic.nodeId === selectedNode.id || diagnostic.nodeId == null)
    : document.diagnostics;

  if (diagnostics.length === 0) {
    return (
      <div className="panel-empty">
        <ShieldCheck aria-hidden="true" />
        <strong>No diagnostics</strong>
        <span>Maven did not report conflict, omission, or anomaly metadata for this graph.</span>
      </div>
    );
  }

  return (
    <section className="diagnostics-panel">
      <div className="panel-title">
        <h2>{selectedNode ? "Selected Diagnostics" : "Diagnostics"}</h2>
        <span>{diagnostics.length} entries</span>
      </div>
      <div className="diagnostic-list">
        {diagnostics.map((diagnostic, index) => {
          const node = diagnostic.nodeId ? nodeById.get(diagnostic.nodeId) : null;
          return (
            <article className={`diagnostic diagnostic-${severityClass(diagnostic.severity)}`} key={`${diagnostic.type}-${diagnostic.nodeId}-${index}`}>
              <div className="diagnostic-icon">{severityIcon(diagnostic.severity)}</div>
              <div>
                <header>
                  <strong>{diagnostic.type}</strong>
                  <span>{diagnostic.severity}</span>
                </header>
                <p>{diagnostic.message}</p>
                {node ? (
                  <button type="button" onClick={() => onSelectNode(node.id)}>
                    {node.label}
                  </button>
                ) : null}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

function severityClass(severity: string): string {
  const normalized = severity.toLowerCase();
  if (normalized.includes("error")) {
    return "error";
  }
  if (normalized.includes("warn")) {
    return "warning";
  }
  return "info";
}

function severityIcon(severity: string) {
  const normalized = severity.toLowerCase();
  if (normalized.includes("error")) {
    return <AlertCircle aria-hidden="true" />;
  }
  if (normalized.includes("warn")) {
    return <AlertTriangle aria-hidden="true" />;
  }
  return <Info aria-hidden="true" />;
}
