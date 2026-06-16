import { describe, expect, it, vi } from "vitest";

vi.mock("sigma", () => ({
  default: class SigmaMock {}
}));

import { nextSelectedNodeId } from "./GraphCanvas";

describe("nextSelectedNodeId", () => {
  it("clears the selection when the clicked node is already selected", () => {
    expect(nextSelectedNodeId("com.example:shared", "com.example:shared")).toBeNull();
  });

  it("selects the clicked node when a different node is selected", () => {
    expect(nextSelectedNodeId("com.example:root", "com.example:shared")).toBe("com.example:shared");
  });
});
