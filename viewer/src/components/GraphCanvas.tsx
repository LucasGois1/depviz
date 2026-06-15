import { useEffect, useMemo, useRef } from "react";
import Sigma from "sigma";
import { buildAdjacency } from "../graph";
import {
  applySigmaGraphState,
  applySigmaLayout,
  toSigmaGraph,
  visibleSigmaNodeExtent,
  type SigmaEdgeAttributes,
  type SigmaNodeAttributes,
  type SigmaNodeExtent
} from "../sigmaGraph";
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

const FIT_MIN_RATIO = 0.18;
const FIT_DURATION_MS = 220;

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

    if (viewportCommand === "fit") {
      fitVisibleGraph(renderer, graph);
      return;
    }

    if (viewportCommand === "reset") {
      applySigmaLayout(graph, layout);
      applySigmaGraphState(graph, { adjacency, selectedNodeId, showLabels, visibility });
      renderer.refresh();
    }

    void renderer.getCamera().animatedReset({ duration: FIT_DURATION_MS });
  }, [adjacency, commandNonce, graph, layout, selectedNodeId, showLabels, viewportCommand, visibility]);

  return <div ref={containerRef} className="graph-canvas sigma-canvas" aria-label="Dependency graph canvas" />;
}

function fitVisibleGraph(renderer: DepvizSigma, graph: ReturnType<typeof toSigmaGraph>): void {
  const extent = visibleSigmaNodeExtent(graph);
  if (!extent) {
    void renderer.getCamera().animatedReset({ duration: FIT_DURATION_MS });
    return;
  }

  renderer.resize();
  const { width, height } = renderer.getDimensions();
  const inset = Math.min(96, Math.max(24, Math.min(width, height) * 0.08));
  const availableWidth = Math.max(1, width - inset * 2);
  const availableHeight = Math.max(1, height - inset * 2);
  const resetCamera = { x: 0.5, y: 0.5, ratio: 1, angle: 0 };
  const viewportExtent = viewportExtentForGraphExtent(renderer, extent, resetCamera);
  const viewportWidth = Math.max(1, viewportExtent.maxX - viewportExtent.minX);
  const viewportHeight = Math.max(1, viewportExtent.maxY - viewportExtent.minY);
  const configuredMinRatio = renderer.getSetting("minCameraRatio") ?? 0;
  const ratio = Math.max(viewportWidth / availableWidth, viewportHeight / availableHeight, configuredMinRatio, FIT_MIN_RATIO);
  const centerViewport = renderer.graphToViewport(
    {
      x: (extent.minX + extent.maxX) / 2,
      y: (extent.minY + extent.maxY) / 2
    },
    { cameraState: resetCamera }
  );
  const center = renderer.viewportToFramedGraph(centerViewport, { cameraState: resetCamera });

  void renderer.getCamera().animate({ x: center.x, y: center.y, ratio, angle: 0 }, { duration: FIT_DURATION_MS });
}

function viewportExtentForGraphExtent(
  renderer: DepvizSigma,
  extent: SigmaNodeExtent,
  cameraState: { x: number; y: number; ratio: number; angle: number }
): SigmaNodeExtent {
  const corners = [
    { x: extent.minX, y: extent.minY },
    { x: extent.maxX, y: extent.minY },
    { x: extent.minX, y: extent.maxY },
    { x: extent.maxX, y: extent.maxY }
  ].map((point) => renderer.graphToViewport(point, { cameraState }));

  return {
    minX: Math.min(...corners.map((point) => point.x)),
    maxX: Math.max(...corners.map((point) => point.x)),
    minY: Math.min(...corners.map((point) => point.y)),
    maxY: Math.max(...corners.map((point) => point.y))
  };
}
