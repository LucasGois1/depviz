import { describe, expect, it } from "vitest";
import { versionSummaryStats } from "./Sidebar";
import type { VersionSummary } from "../types";

describe("versionSummaryStats", () => {
  it("includes the unknown update counter in compact summary stats", () => {
    const summary: VersionSummary = {
      enabled: true,
      checked: 12,
      current: 4,
      outdated: 6,
      patch: 1,
      minor: 2,
      major: 3,
      unknown: 5,
      unavailable: 2
    };

    expect(versionSummaryStats(summary)).toEqual([
      { label: "Outdated", value: 6 },
      { label: "Major", value: 3 },
      { label: "Minor", value: 2 },
      { label: "Patch", value: 1 },
      { label: "Unknown", value: 5 },
      { label: "Unavailable", value: 2 }
    ]);
  });
});
