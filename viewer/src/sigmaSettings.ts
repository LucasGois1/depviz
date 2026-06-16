import { drawDependencyNodeHover, drawDependencyNodeLabel } from "./sigmaLabelRenderer";

export const sigmaRendererSettings = {
  allowInvalidContainer: true,
  defaultEdgeType: "arrow",
  defaultNodeType: "circle",
  defaultDrawNodeHover: drawDependencyNodeHover,
  defaultDrawNodeLabel: drawDependencyNodeLabel,
  enableEdgeEvents: false,
  hideEdgesOnMove: false,
  hideLabelsOnMove: true,
  labelColor: { color: "#dbeafe" },
  labelDensity: 0.14,
  labelFont: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
  labelGridCellSize: 104,
  labelRenderedSizeThreshold: 8,
  labelSize: 11,
  labelWeight: "680",
  minCameraRatio: 0.04,
  minEdgeThickness: 0.72,
  renderEdgeLabels: false,
  renderLabels: true,
  stagePadding: 52,
  zIndex: true
} as const;
