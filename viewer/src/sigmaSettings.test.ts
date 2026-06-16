import { describe, expect, it } from "vitest";
import { sigmaRendererSettings } from "./sigmaSettings";
import { drawDependencyNodeHover, drawDependencyNodeLabel } from "./sigmaLabelRenderer";

describe("sigmaRendererSettings", () => {
  it("keeps renderer node sizing in pixels instead of graph coordinates", () => {
    expect(sigmaRendererSettings).not.toHaveProperty("itemSizesReference", "positions");
  });

  it("preserves the base viewport padding", () => {
    expect(sigmaRendererSettings.stagePadding).toBe(52);
  });

  it("preserves the base label movement behavior", () => {
    expect(sigmaRendererSettings.hideLabelsOnMove).toBe(true);
  });

  it("uses the dependency label renderer to keep edge labels inside the canvas", () => {
    expect(sigmaRendererSettings.defaultDrawNodeLabel).toBe(drawDependencyNodeLabel);
  });

  it("uses the dark dependency hover renderer instead of Sigma's light hover box", () => {
    expect(sigmaRendererSettings.defaultDrawNodeHover).toBeTypeOf("function");
    expect(sigmaRendererSettings.defaultDrawNodeHover).toBe(drawDependencyNodeHover);
  });

  it("keeps graph edges visible against the canvas grid", () => {
    expect(sigmaRendererSettings.minEdgeThickness).toBeGreaterThanOrEqual(0.72);
  });

  it("uses a light label color for the dark graph canvas", () => {
    expect(sigmaRendererSettings.labelColor).toEqual({ color: "#dbeafe" });
  });
});
