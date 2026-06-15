import { describe, expect, it, vi } from "vitest";
import { badgeColorForUpdate, badgeTextForVersionInsight, drawDependencyNodeLabel, labelSideForCanvasPosition, versionBadgeForNode } from "./sigmaLabelRenderer";
import type { SigmaNodeAttributes } from "./sigmaGraph";
import type { VersionInsight } from "./types";

describe("labelSideForCanvasPosition", () => {
  it("draws labels on the left when nodes are near the right edge", () => {
    expect(labelSideForCanvasPosition(720, 800)).toBe("left");
  });

  it("draws labels on the right for central and left-side nodes", () => {
    expect(labelSideForCanvasPosition(320, 800)).toBe("right");
  });
});

describe("badgeTextForVersionInsight", () => {
  it.each([
    ["patch", "outdated", "P"],
    ["minor", "outdated", "m"],
    ["major", "outdated", "M"],
    ["unknown", "outdated", "?"],
    ["unknown", "unavailable", "!"]
  ] as const)("maps %s %s insights to compact badge text", (updateType, status, expected) => {
    expect(badgeTextForVersionInsight(insight(updateType, status))).toBe(expected);
  });

  it("does not badge current dependencies", () => {
    expect(badgeTextForVersionInsight(insight("none", "current"))).toBeNull();
  });
});

describe("badgeColorForUpdate", () => {
  it.each([
    ["patch", { background: "#ccfbf1", border: "#5eead4", text: "#0f766e" }],
    ["minor", { background: "#dbeafe", border: "#93c5fd", text: "#1d4ed8" }],
    ["major", { background: "#fee2e2", border: "#fca5a5", text: "#b91c1c" }],
    ["unknown", { background: "#e5e7eb", border: "#cbd5e1", text: "#475569" }],
    ["unavailable", { background: "#fef3c7", border: "#fcd34d", text: "#b45309" }]
  ] as const)("maps %s badges to restrained colors", (updateType, expected) => {
    expect(badgeColorForUpdate(updateType)).toEqual(expected);
  });
});

describe("versionBadgeForNode", () => {
  it("combines serialized badge text and color metadata for the renderer", () => {
    expect(versionBadgeForNode({ updateType: "major", updateBadge: "M" } as SigmaNodeAttributes)).toEqual({
      text: "M",
      color: { background: "#fee2e2", border: "#fca5a5", text: "#b91c1c" }
    });
  });

  it("skips nodes without badge metadata", () => {
    expect(versionBadgeForNode({ updateType: "major" } as SigmaNodeAttributes)).toBeNull();
  });

  it("uses unavailable badge colors from status even when update type is unknown", () => {
    expect(versionBadgeForNode({ versionStatus: "unavailable", updateType: "unknown", updateBadge: "!" } as SigmaNodeAttributes)).toEqual({
      text: "!",
      color: { background: "#fef3c7", border: "#fcd34d", text: "#b45309" }
    });
  });
});

describe("drawDependencyNodeLabel", () => {
  it("draws unbadged labels at base Sigma coordinates without a stroke outline", () => {
    const context = canvasContext();

    drawDependencyNodeLabel(
      context,
      { label: "client", x: 100, y: 50, size: 7 } as SigmaNodeAttributes,
      labelSettings()
    );

    expect(context.fillText).toHaveBeenCalledWith("client", 110, 54);
    expect(context.strokeText).not.toHaveBeenCalled();
  });

  it("draws a badge pill and badge text for badged labels", () => {
    const context = canvasContext();

    drawDependencyNodeLabel(
      context,
      { label: "client", x: 100, y: 50, size: 7, updateType: "major", updateBadge: "M" } as SigmaNodeAttributes,
      labelSettings()
    );

    expect(context.fillText).toHaveBeenCalledWith("client", 110, 54);
    expect(context.beginPath).toHaveBeenCalled();
    expect(context.fill).toHaveBeenCalled();
    expect(context.stroke).toHaveBeenCalled();
    expect(context.fillText).toHaveBeenCalledWith("M", expect.any(Number), expect.any(Number));
  });
});

function insight(updateType: VersionInsight["updateType"], status: VersionInsight["status"]): VersionInsight {
  return {
    currentVersion: "1.0.0",
    latestVersion: status === "unavailable" ? null : "2.0.0",
    updateType,
    status,
    checked: true,
    message: null
  };
}

function labelSettings() {
  return {
    labelSize: 12,
    labelFont: "Inter",
    labelWeight: "650",
    labelColor: { color: "#0f172a" }
  } as Parameters<typeof drawDependencyNodeLabel>[2];
}

function canvasContext() {
  return {
    canvas: { width: 800 },
    measureText: vi.fn((text: string) => ({ width: text.length * 6 })),
    strokeText: vi.fn(),
    fillText: vi.fn(),
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    quadraticCurveTo: vi.fn(),
    closePath: vi.fn(),
    fill: vi.fn(),
    stroke: vi.fn(),
    font: "",
    lineWidth: 0,
    lineJoin: "round",
    strokeStyle: "",
    fillStyle: "",
    textAlign: "start",
    textBaseline: "alphabetic"
  } as unknown as CanvasRenderingContext2D;
}
