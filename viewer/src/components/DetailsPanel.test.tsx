import { describe, expect, it } from "vitest";
import { versionStatusText, versionUpdateText } from "./DetailsPanel";
import type { VersionInsight } from "../types";

describe("version display helpers", () => {
  it("shows unavailable status separately from the actual update type", () => {
    const insight: VersionInsight = {
      currentVersion: "1.0.0",
      latestVersion: null,
      updateType: "unknown",
      status: "unavailable",
      checked: true,
      message: "Repository lookup failed"
    };

    expect(versionStatusText(insight)).toBe("unavailable");
    expect(versionUpdateText(insight)).toBe("unknown");
  });
});
