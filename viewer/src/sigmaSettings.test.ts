import { describe, expect, it } from "vitest";
import { sigmaRendererSettings } from "./sigmaSettings";
import { drawDependencyNodeLabel } from "./sigmaLabelRenderer";

describe("sigmaRendererSettings", () => {
  it("keeps renderer node sizing tied to graph positions", () => {
    expect(sigmaRendererSettings.itemSizesReference).toBe("positions");
  });

  it("keeps enough viewport padding for labels near the canvas edge", () => {
    expect(sigmaRendererSettings.stagePadding).toBeGreaterThanOrEqual(88);
    expect(sigmaRendererSettings.stagePadding).toBeLessThanOrEqual(120);
  });

  it("keeps labels visible during camera movement for first-load readability", () => {
    expect(sigmaRendererSettings.hideLabelsOnMove).toBe(false);
  });

  it("uses the dependency label renderer to keep edge labels inside the canvas", () => {
    expect(sigmaRendererSettings.defaultDrawNodeLabel).toBe(drawDependencyNodeLabel);
  });
});
