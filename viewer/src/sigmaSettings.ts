import { drawDependencyNodeLabel } from "./sigmaLabelRenderer";

export const sigmaRendererSettings = {
  allowInvalidContainer: true,
  defaultEdgeType: "arrow",
  defaultNodeType: "circle",
  defaultDrawNodeLabel: drawDependencyNodeLabel,
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
} as const;
