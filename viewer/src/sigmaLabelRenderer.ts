import type { NodeHoverDrawingFunction, NodeLabelDrawingFunction } from "sigma/rendering";
import type { SigmaEdgeAttributes, SigmaNodeAttributes, SigmaVersionUpdateType } from "./sigmaGraph";
import type { SecurityInsight, SecuritySeverity, VersionInsight } from "./types";

interface BadgeColor {
  background: string;
  border: string;
  text: string;
}

interface VersionBadge {
  text: string;
  color: BadgeColor;
}

interface LabelBadge extends VersionBadge {
  kind: "security" | "version";
}

type VersionBadgeTone = SigmaVersionUpdateType | "unavailable";

export function labelSideForCanvasPosition(x: number, canvasWidth: number): "left" | "right" {
  return x > canvasWidth * 0.62 ? "left" : "right";
}

export const drawDependencyNodeLabel: NodeLabelDrawingFunction<SigmaNodeAttributes, SigmaEdgeAttributes> = (context, data, settings) => {
  if (!data.label) {
    return;
  }

  const size = settings.labelSize;
  const font = settings.labelFont;
  const weight = settings.labelWeight;
  const color = settings.labelColor.attribute
    ? String((data as Record<string, unknown>)[settings.labelColor.attribute] ?? settings.labelColor.color ?? "#dbeafe")
    : settings.labelColor.color;
  const label = String(data.label);

  context.font = `${weight} ${size}px ${font}`;

  const textWidth = context.measureText(label).width;
  const side = labelSideForCanvasPosition(data.x, context.canvas.width);
  const x = side === "left" ? data.x - data.size - 3 - textWidth : data.x + data.size + 3;
  const y = data.y + size / 3;
  const badges = badgesForNode(data as Partial<SigmaNodeAttributes>);

  context.lineJoin = "round";
  context.lineWidth = 4;
  context.strokeStyle = "rgba(6, 12, 20, 0.82)";
  context.strokeText(label, x, y);
  context.fillStyle = color ?? "#dbeafe";
  context.fillText(label, x, y);

  if (badges.length > 0) {
    let xOffset = 6;
    for (const badge of badges) {
      xOffset = drawBadge(context, {
        badge,
        labelX: x,
        labelY: y,
        labelWidth: textWidth,
        xOffset,
        size,
        font,
        weight
      });
    }
  }
};

export const drawDependencyNodeHover: NodeHoverDrawingFunction<SigmaNodeAttributes, SigmaEdgeAttributes> = (context, data, settings) => {
  const radius = Math.max(data.size + 3, settings.labelSize * 0.72);

  context.beginPath();
  context.arc(data.x, data.y, radius, 0, Math.PI * 2);
  context.closePath();
  context.fillStyle = "rgba(88, 166, 255, 0.18)";
  context.fill();
  context.strokeStyle = "rgba(219, 234, 254, 0.72)";
  context.lineWidth = 2;
  context.stroke();

  drawDependencyNodeLabel(context, data as SigmaNodeAttributes, settings);
};

export function badgeTextForVersionInsight(insight: VersionInsight | null | undefined): string | null {
  if (!insight) {
    return null;
  }
  if (insight.status === "unavailable") {
    return "!";
  }
  if (insight.status !== "outdated") {
    return null;
  }
  if (insight.updateType === "patch") {
    return "P";
  }
  if (insight.updateType === "minor") {
    return "m";
  }
  if (insight.updateType === "major") {
    return "M";
  }
  if (insight.updateType === "unknown") {
    return "?";
  }
  return null;
}

export function badgeColorForUpdate(updateType: VersionBadgeTone): BadgeColor {
  if (updateType === "patch") {
    return { background: "#0f3d3a", border: "#2dd4bf", text: "#99f6e4" };
  }
  if (updateType === "minor") {
    return { background: "#12345c", border: "#58a6ff", text: "#bfdbfe" };
  }
  if (updateType === "major") {
    return { background: "#4a1620", border: "#fb7185", text: "#fecdd3" };
  }
  if (updateType === "unavailable") {
    return { background: "#422006", border: "#fbbf24", text: "#fde68a" };
  }
  return { background: "#1f2937", border: "#475569", text: "#cbd5e1" };
}

export function securityBadgeText(insight: Pick<SecurityInsight, "maxSeverity" | "status"> | null | undefined): string | null {
  if (!insight || insight.status !== "vulnerable") {
    return null;
  }
  if (insight.maxSeverity === "critical") {
    return "C";
  }
  if (insight.maxSeverity === "high") {
    return "H";
  }
  if (insight.maxSeverity === "medium") {
    return "M";
  }
  if (insight.maxSeverity === "low") {
    return "L";
  }
  return null;
}

export function badgesForNode(
  node: Pick<Partial<SigmaNodeAttributes>, "securityBadge" | "securitySeverity" | "updateBadge" | "updateType" | "versionStatus">
): LabelBadge[] {
  const securityBadge = securityBadgeForNode(node);
  const versionBadge = versionBadgeForNode(node);
  return [securityBadge, versionBadge ? { ...versionBadge, kind: "version" as const } : null].filter((badge): badge is LabelBadge => Boolean(badge));
}

export function versionBadgeForNode(node: Pick<Partial<SigmaNodeAttributes>, "updateBadge" | "updateType" | "versionStatus">): VersionBadge | null {
  if (!node.updateBadge || !node.updateType) {
    return null;
  }
  const badgeTone = node.versionStatus === "unavailable" ? "unavailable" : node.updateType;
  return {
    text: node.updateBadge,
    color: badgeColorForUpdate(badgeTone)
  };
}

function securityBadgeForNode(node: Pick<Partial<SigmaNodeAttributes>, "securityBadge" | "securitySeverity">): LabelBadge | null {
  if (!node.securityBadge || !node.securitySeverity) {
    return null;
  }
  return {
    text: node.securityBadge,
    kind: "security",
    color: badgeColorForSeverity(node.securitySeverity)
  };
}

function badgeColorForSeverity(severity: SecuritySeverity): BadgeColor {
  if (severity === "critical") {
    return { background: "#5f1220", border: "#f43f5e", text: "#ffe4e6" };
  }
  if (severity === "high") {
    return { background: "#4a1620", border: "#fb7185", text: "#fecdd3" };
  }
  if (severity === "medium") {
    return { background: "#422006", border: "#fbbf24", text: "#fde68a" };
  }
  return { background: "#0f2f4a", border: "#38bdf8", text: "#bae6fd" };
}

function drawBadge(
  context: CanvasRenderingContext2D,
  params: {
    badge: LabelBadge;
    labelX: number;
    labelY: number;
    labelWidth: number;
    xOffset: number;
    size: number;
    font: string;
    weight: string;
  }
): number {
  const { badge, font, labelWidth, labelX, labelY, size, weight, xOffset } = params;
  const badgeFontSize = Math.max(9, size - 2);
  const horizontalPadding = 6;
  const height = Math.max(16, badgeFontSize + 7);
  const gap = 6;

  context.font = `${weight} ${badgeFontSize}px ${font}`;
  const badgeWidth = Math.max(18, context.measureText(badge.text).width + horizontalPadding * 2);
  const x = labelX + labelWidth + xOffset;
  const y = labelY - height + 4;
  const radius = height / 2;

  context.beginPath();
  roundedRect(context, x, y, badgeWidth, height, radius);
  context.fillStyle = badge.color.background;
  context.fill();
  context.strokeStyle = badge.color.border;
  context.lineWidth = 1;
  context.stroke();
  context.fillStyle = badge.color.text;
  context.textAlign = "center";
  context.textBaseline = "middle";
  context.fillText(badge.text, x + badgeWidth / 2, y + height / 2 + 0.5);
  context.textAlign = "start";
  context.textBaseline = "alphabetic";
  return xOffset + badgeWidth + gap;
}

function roundedRect(context: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number): void {
  context.moveTo(x + radius, y);
  context.lineTo(x + width - radius, y);
  context.quadraticCurveTo(x + width, y, x + width, y + radius);
  context.lineTo(x + width, y + height - radius);
  context.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  context.lineTo(x + radius, y + height);
  context.quadraticCurveTo(x, y + height, x, y + height - radius);
  context.lineTo(x, y + radius);
  context.quadraticCurveTo(x, y, x + radius, y);
  context.closePath();
}
