import { useEffect, useMemo, useRef } from "react";
import cytoscape, { type Core, type ElementDefinition, type SingularElementArgument, type StylesheetCSS } from "cytoscape";
import { cytoscapeHslColor } from "../cytoscapeStyle";
import { toCytoscapeElements } from "../graph";
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
  const cyRef = useRef<Core | null>(null);
  const onSelectNodeRef = useRef(onSelectNode);
  const elements = useMemo(() => toCytoscapeElements(document), [document]);

  useEffect(() => {
    onSelectNodeRef.current = onSelectNode;
  }, [onSelectNode]);

  useEffect(() => {
    if (!containerRef.current) {
      return undefined;
    }

    const cy = cytoscape({
      container: containerRef.current,
      elements: elements as ElementDefinition[],
      style: stylesheet(showLabels),
      layout: layoutOptions(layout),
      minZoom: 0.08,
      maxZoom: 3,
      wheelSensitivity: 0.18,
      autoungrabify: false
    });

    cy.on("tap", (event) => {
      if (event.target === cy) {
        onSelectNodeRef.current(null);
      }
    });
    cy.on("tap", "node", (event) => {
      onSelectNodeRef.current(event.target.id());
    });

    cyRef.current = cy;
    return () => {
      cy.destroy();
      cyRef.current = null;
    };
  }, [elements]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) {
      return;
    }
    cy.style(stylesheet(showLabels)).update();
  }, [showLabels]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) {
      return;
    }
    cy.nodes().forEach((node) => {
      node.toggleClass("is-hidden", !visibility.visibleNodeIds.has(node.id()));
      node.toggleClass("is-match", visibility.matchingNodeIds.has(node.id()));
    });
    cy.edges().forEach((edge) => {
      edge.toggleClass("is-hidden", !visibility.visibleEdgeIds.has(edge.id()));
    });
    runLayout(cy, layout);
  }, [visibility, layout]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) {
      return;
    }
    cy.elements().removeClass("is-selected is-neighbor is-dimmed");
    if (!selectedNodeId) {
      return;
    }
    const selected = cy.getElementById(selectedNodeId);
    if (!selected.nonempty()) {
      return;
    }
    const neighborhood = selected.closedNeighborhood();
    cy.elements().difference(neighborhood).addClass("is-dimmed");
    neighborhood.addClass("is-neighbor");
    selected.addClass("is-selected");
  }, [selectedNodeId, visibility]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy || !viewportCommand) {
      return;
    }
    if (viewportCommand === "reset") {
      runLayout(cy, layout);
      cy.fit(cy.elements(":visible"), 48);
      return;
    }
    cy.fit(cy.elements(":visible"), 48);
  }, [viewportCommand, commandNonce, layout]);

  return <div ref={containerRef} className="graph-canvas" aria-label="Dependency graph canvas" />;
}

function runLayout(cy: Core, layout: LayoutName) {
  cy.layout(layoutOptions(layout)).run();
}

function layoutOptions(layout: LayoutName) {
  if (layout === "force") {
    return {
      name: "cose",
      animate: false,
      nodeRepulsion: 14000,
      idealEdgeLength: 118,
      componentSpacing: 88,
      padding: 36
    };
  }
  if (layout === "circle") {
    return { name: "circle", animate: false, padding: 38 };
  }
  if (layout === "concentric") {
    return {
      name: "concentric",
      animate: false,
      padding: 38,
      concentric: (node: SingularElementArgument) => Math.max(Number(node.data("fanIn") ?? 0) * 3, 8 - Number(node.data("depth") ?? 0)),
      levelWidth: () => 2
    };
  }
  return {
    name: "breadthfirst",
    directed: true,
    animate: false,
    spacingFactor: 1.62,
    padding: 38
  };
}

function stylesheet(showLabels: boolean): StylesheetCSS[] {
  return [
    {
      selector: "node",
      css: {
        width: nodeSize,
        height: nodeSize,
        "background-color": (element: SingularElementArgument) => cytoscapeHslColor(Number(element.data("hue") ?? 0)),
        "border-width": 2,
        "border-color": "rgba(255,255,255,0.9)",
        "font-family": "ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
        "font-size": 10.5,
        "font-weight": 650,
        color: "#0f172a",
        label: (element: SingularElementArgument) => nodeLabel(element, showLabels),
        "text-background-color": "rgba(255,255,255,0.88)",
        "text-background-opacity": (element: SingularElementArgument) => (nodeLabel(element, showLabels) ? 1 : 0),
        "text-background-padding": "4px",
        "text-background-shape": "roundrectangle",
        "text-margin-y": 8,
        "text-wrap": "wrap",
        "text-max-width": "136px",
        "overlay-opacity": 0
      }
    },
    {
      selector: "node.root",
      css: {
        shape: "round-rectangle",
        width: 42,
        height: 42,
        "border-width": 4,
        "border-color": "#111827"
      }
    },
    {
      selector: "node.shared",
      css: {
        "border-width": 4,
        "border-color": "#0f766e",
        "z-index": 8
      }
    },
    {
      selector: "node.hub",
      css: {
        "border-width": 5,
        "border-color": "#b45309",
        "z-index": 10
      }
    },
    {
      selector: "node.optional",
      css: {
        "border-style": "dashed"
      }
    },
    {
      selector: "edge",
      css: {
        width: 1.7,
        "curve-style": "bezier",
        "target-arrow-shape": "triangle",
        "target-arrow-color": "#64748b",
        "line-color": "#94a3b8",
        opacity: 0.76,
        "arrow-scale": 0.9,
        "overlay-opacity": 0
      }
    },
    {
      selector: "edge.to-shared",
      css: {
        width: (element: SingularElementArgument) => Math.min(3.4, 1.7 + Number(element.data("targetFanIn") ?? 0) * 0.22),
        "line-color": "#0f766e",
        "target-arrow-color": "#0f766e",
        opacity: 0.9,
        "z-index": 6
      }
    },
    {
      selector: "edge.optional",
      css: {
        "line-style": "dashed",
        opacity: 0.55
      }
    },
    {
      selector: ".is-hidden",
      css: {
        display: "none"
      }
    },
    {
      selector: ".is-dimmed",
      css: {
        opacity: 0.24
      }
    },
    {
      selector: ".is-neighbor",
      css: {
        opacity: 0.95
      }
    },
    {
      selector: "node.is-match",
      css: {
        "border-color": "#f59e0b",
        "border-width": 4
      }
    },
    {
      selector: "node.is-selected",
      css: {
        "border-color": "#020617",
        "border-width": 5,
        width: 54,
        height: 54,
        "z-index": 20
      }
    },
    {
      selector: "edge.is-neighbor",
      css: {
        width: 2.8,
        opacity: 0.95,
        "line-color": "#334155",
        "target-arrow-color": "#334155"
      }
    }
  ];
}

function nodeSize(element: SingularElementArgument): number {
  const fanIn = Number(element.data("fanIn") ?? 0);
  return Math.min(66, 34 + fanIn * 4);
}

function nodeLabel(element: SingularElementArgument, showLabels: boolean): string {
  if (showLabels) {
    return String(element.data("label") ?? "");
  }
  const isKeyNode = Boolean(element.data("root")) || Boolean(element.data("shared")) || Number(element.data("depth") ?? 0) <= 1;
  return isKeyNode ? String(element.data("label") ?? "") : "";
}
