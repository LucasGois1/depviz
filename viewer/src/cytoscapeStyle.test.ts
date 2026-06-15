import { describe, expect, it } from "vitest";
import { cytoscapeHslColor } from "./cytoscapeStyle";

describe("cytoscapeHslColor", () => {
  it("uses the comma-separated hsl syntax accepted by Cytoscape", () => {
    expect(cytoscapeHslColor(327)).toBe("hsl(327, 58%, 46%)");
  });
});
