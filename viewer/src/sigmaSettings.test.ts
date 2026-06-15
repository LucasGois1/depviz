import { describe, expect, it } from "vitest";
import { sigmaRendererSettings } from "./sigmaSettings";
import { drawDependencyNodeLabel } from "./sigmaLabelRenderer";

describe("sigmaRendererSettings", () => {
  it("keeps renderer node sizing tied to graph positions", () => {
    expect(sigmaRendererSettings.itemSizesReference).toBe("positions");
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
});
