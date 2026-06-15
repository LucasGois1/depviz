import { useEffect, useMemo, useRef } from "react";
import Sigma from "sigma";
import { buildAdjacency } from "../graph";
import { applySigmaGraphState, applySigmaLayout, toSigmaGraph, type SigmaEdgeAttributes, type SigmaNodeAttributes } from "../sigmaGraph";
import { sigmaRendererSettings } from "../sigmaSettings";
import type { DepvizDocument, LayoutName, VisibilityState } from "../types";

interface GraphCanvasProps {
  document: DepvizDocument;
  layout: LayoutName;
  selectedNodeId: string | null;
  visibility: VisibilityState;
  showLabels: boolean;
  viewportCommand: "fit" | "reset" | null;
  commandNonce: number;
  onSelectNode: (nodeId: string | null) => void;
}

type DepvizSigma = Sigma<SigmaNodeAttributes, SigmaEdgeAttributes>;

export function GraphCanvas({
  document,
  layout,
  selectedNodeId,
  visibility,
  showLabels,
  viewportCommand,
  commandNonce,
  onSelectNode
}: GraphCanvasProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const rendererRef = useRef<DepvizSigma | null>(null);
  const onSelectNodeRef = useRef(onSelectNode);
  const graph = useMemo(() => toSigmaGraph(document, layout), [document, layout]);
  const adjacency = useMemo(() => buildAdjacency(document), [document]);

  useEffect(() => {
    onSelectNodeRef.current = onSelectNode;
  }, [onSelectNode]);

  useEffect(() => {
    if (!containerRef.current) {
      return undefined;
    }

    applySigmaGraphState(graph, { adjacency, selectedNodeId, showLabels, visibility });

    const renderer: DepvizSigma = new Sigma(graph, containerRef.current, sigmaRendererSettings);

    renderer.on("clickStage", () => {
      onSelectNodeRef.current(null);
    });
    renderer.on("clickNode", ({ node }) => {
      onSelectNodeRef.current(node);
    });

    rendererRef.current = renderer;
    renderer.getCamera().animatedReset({ duration: 220 });

    return () => {
      renderer.kill();
      rendererRef.current = null;
    };
  }, [graph]);

  useEffect(() => {
    applySigmaGraphState(graph, { adjacency, selectedNodeId, showLabels, visibility });
    rendererRef.current?.refresh();
  }, [adjacency, graph, selectedNodeId, showLabels, visibility]);

  useEffect(() => {
    const renderer = rendererRef.current;
    if (!renderer || !viewportCommand) {
      return;
    }

    if (viewportCommand === "reset") {
      applySigmaLayout(graph, layout);
      applySigmaGraphState(graph, { adjacency, selectedNodeId, showLabels, visibility });
      renderer.refresh();
    }

    renderer.getCamera().animatedReset({ duration: 220 });
  }, [adjacency, commandNonce, graph, layout, selectedNodeId, showLabels, viewportCommand, visibility]);

  return <div ref={containerRef} className="graph-canvas sigma-canvas" aria-label="Dependency graph canvas" />;
}
