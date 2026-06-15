import { useEffect, useMemo, useRef } from "react";
import Sigma from "sigma";
import { buildAdjacency } from "../graph";
import { applySigmaGraphState, applySigmaLayout, toSigmaGraph, type SigmaEdgeAttributes, type SigmaNodeAttributes } from "../sigmaGraph";
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

    const renderer: DepvizSigma = new Sigma(graph, containerRef.current, {
      allowInvalidContainer: true,
      defaultEdgeType: "arrow",
      defaultNodeType: "circle",
      enableEdgeEvents: false,
      hideEdgesOnMove: false,
      hideLabelsOnMove: true,
      itemSizesReference: "positions",
      labelColor: { color: "#0f172a" },
      labelDensity: 0.16,
      labelFont: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
      labelGridCellSize: 92,
      labelRenderedSizeThreshold: 7,
      labelSize: 12,
      labelWeight: "650",
      minCameraRatio: 0.04,
      minEdgeThickness: 0.55,
      renderEdgeLabels: false,
      renderLabels: true,
      stagePadding: 52,
      zIndex: true
    });

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
