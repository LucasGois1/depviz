import type { NodeLabelDrawingFunction } from "sigma/rendering";
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
    ? String((data as Record<string, unknown>)[settings.labelColor.attribute] ?? settings.labelColor.color ?? "#0f172a")
    : settings.labelColor.color;
  const label = String(data.label);

  context.font = `${weight} ${size}px ${font}`;
  context.fillStyle = color ?? "#0f172a";

  const x = data.x + data.size + 3;
  const y = data.y + size / 3;
  const badges = badgesForNode(data as Partial<SigmaNodeAttributes>);

  context.fillText(label, x, y);

  if (badges.length > 0) {
    const textWidth = context.measureText(label).width;
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
    return { background: "#ccfbf1", border: "#5eead4", text: "#0f766e" };
  }
  if (updateType === "minor") {
    return { background: "#dbeafe", border: "#93c5fd", text: "#1d4ed8" };
  }
  if (updateType === "major") {
    return { background: "#fee2e2", border: "#fca5a5", text: "#b91c1c" };
  }
  if (updateType === "unavailable") {
    return { background: "#fef3c7", border: "#fcd34d", text: "#b45309" };
  }
  return { background: "#e5e7eb", border: "#cbd5e1", text: "#475569" };
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
    return { background: "#fecaca", border: "#f87171", text: "#7f1d1d" };
  }
  if (severity === "high") {
    return { background: "#fee2e2", border: "#fca5a5", text: "#b91c1c" };
  }
  if (severity === "medium") {
    return { background: "#fef3c7", border: "#fcd34d", text: "#b45309" };
  }
  return { background: "#dbeafe", border: "#93c5fd", text: "#1d4ed8" };
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
