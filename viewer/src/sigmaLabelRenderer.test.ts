import { describe, expect, it, vi } from "vitest";
import {
  badgeColorForUpdate,
  badgeTextForVersionInsight,
  badgesForNode,
  drawDependencyNodeLabel,
  labelSideForCanvasPosition,
  securityBadgeText,
  versionBadgeForNode
} from "./sigmaLabelRenderer";
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
    ["patch", { background: "#0f3d3a", border: "#2dd4bf", text: "#99f6e4" }],
    ["minor", { background: "#12345c", border: "#58a6ff", text: "#bfdbfe" }],
    ["major", { background: "#4a1620", border: "#fb7185", text: "#fecdd3" }],
    ["unknown", { background: "#1f2937", border: "#475569", text: "#cbd5e1" }],
    ["unavailable", { background: "#422006", border: "#fbbf24", text: "#fde68a" }]
  ] as const)("maps %s badges to dark theme colors", (updateType, expected) => {
    expect(badgeColorForUpdate(updateType)).toEqual(expected);
  });
});

describe("securityBadgeText", () => {
  it("maps security severities to compact badge text", () => {
    expect(securityBadgeText({ maxSeverity: "critical", status: "vulnerable" })).toBe("C");
    expect(securityBadgeText({ maxSeverity: "high", status: "vulnerable" })).toBe("H");
    expect(securityBadgeText({ maxSeverity: "medium", status: "vulnerable" })).toBe("M");
    expect(securityBadgeText({ maxSeverity: "low", status: "vulnerable" })).toBe("L");
  });

  it("does not badge unavailable, unchecked, or not-vulnerable security insight", () => {
    expect(securityBadgeText({ maxSeverity: "high", status: "not-vulnerable" })).toBeNull();
    expect(securityBadgeText({ maxSeverity: "high", status: "unavailable" })).toBeNull();
    expect(securityBadgeText({ maxSeverity: "high", status: "unchecked" })).toBeNull();
    expect(securityBadgeText(null)).toBeNull();
  });
});

describe("versionBadgeForNode", () => {
  it("combines serialized badge text and color metadata for the renderer", () => {
    expect(versionBadgeForNode({ updateType: "major", updateBadge: "M" } as SigmaNodeAttributes)).toEqual({
      text: "M",
      color: { background: "#4a1620", border: "#fb7185", text: "#fecdd3" }
    });
  });

  it("skips nodes without badge metadata", () => {
    expect(versionBadgeForNode({ updateType: "major" } as SigmaNodeAttributes)).toBeNull();
  });

  it("uses unavailable badge colors from status even when update type is unknown", () => {
    expect(versionBadgeForNode({ versionStatus: "unavailable", updateType: "unknown", updateBadge: "!" } as SigmaNodeAttributes)).toEqual({
      text: "!",
      color: { background: "#422006", border: "#fbbf24", text: "#fde68a" }
    });
  });
});

describe("badgesForNode", () => {
  it("returns security and version badges as separate ordered badges", () => {
    expect(
      badgesForNode({
        securityBadge: "H",
        securitySeverity: "high",
        updateBadge: "P",
        updateType: "patch",
        versionStatus: "outdated"
      })
    ).toEqual([
      {
        text: "H",
        kind: "security",
        color: { background: "#4a1620", border: "#fb7185", text: "#fecdd3" }
      },
      {
        text: "P",
        kind: "version",
        color: { background: "#0f3d3a", border: "#2dd4bf", text: "#99f6e4" }
      }
    ]);
  });
});

describe("drawDependencyNodeLabel", () => {
  it("draws unbadged labels with a dark halo at adaptive Sigma coordinates", () => {
    const context = canvasContext();

    drawDependencyNodeLabel(
      context,
      { label: "client", x: 100, y: 50, size: 7 } as SigmaNodeAttributes,
      labelSettings()
    );

    expect(context.fillText).toHaveBeenCalledWith("client", 110, 54);
    expect(context.strokeText).toHaveBeenCalledWith("client", 110, 54);
    expect(context.strokeStyle).toBe("rgba(6, 12, 20, 0.82)");
  });

  it("draws labels on the left when nodes are near the right canvas edge", () => {
    const context = canvasContext();

    drawDependencyNodeLabel(
      context,
      { label: "client", x: 720, y: 50, size: 7 } as SigmaNodeAttributes,
      labelSettings()
    );

    expect(context.strokeText).toHaveBeenCalledWith("client", 674, 54);
    expect(context.fillText).toHaveBeenCalledWith("client", 674, 54);
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

  it("draws multiple badges with accumulated x offsets", () => {
    const context = canvasContext();

    drawDependencyNodeLabel(
      context,
      {
        label: "client",
        x: 100,
        y: 50,
        size: 7,
        securityBadge: "H",
        securitySeverity: "high",
        updateType: "patch",
        updateBadge: "P"
      } as SigmaNodeAttributes,
      labelSettings()
    );

    const hCall = fillTextCall(context, "H");
    const pCall = fillTextCall(context, "P");

    expect(context.fillText).toHaveBeenCalledWith("client", 110, 54);
    expect(context.beginPath).toHaveBeenCalledTimes(2);
    expect(hCall).toBeDefined();
    expect(pCall).toBeDefined();
    expect(Number(pCall?.[1])).toBeGreaterThan(Number(hCall?.[1]));
  });
});

function fillTextCall(context: CanvasRenderingContext2D, text: string): Parameters<CanvasRenderingContext2D["fillText"]> | undefined {
  return vi.mocked(context.fillText).mock.calls.find((call) => call[0] === text);
}

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
    labelColor: { color: "#dbeafe" }
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
