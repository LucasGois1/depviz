import type { NodeLabelDrawingFunction } from "sigma/rendering";
import type { SigmaEdgeAttributes, SigmaNodeAttributes, SigmaVersionUpdateType } from "./sigmaGraph";
import type { VersionInsight } from "./types";

interface BadgeColor {
  background: string;
  border: string;
  text: string;
}

interface VersionBadge {
  text: string;
  color: BadgeColor;
}

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
  context.lineWidth = 4;
  context.lineJoin = "round";
  context.strokeStyle = "rgba(248, 250, 252, 0.94)";
  context.fillStyle = color ?? "#0f172a";

  const gap = Math.max(data.size + 5, 11);
  const textWidth = context.measureText(label).width;
  const side = labelSideForCanvasPosition(data.x, context.canvas.width);
  const x = side === "left" ? data.x - gap - textWidth : data.x + gap;
  const y = data.y + size / 3;
  const badge = versionBadgeForNode(data as Partial<SigmaNodeAttributes>);

  context.strokeText(label, x, y);
  context.fillText(label, x, y);

  if (badge) {
    drawBadge(context, {
      badge,
      labelX: x,
      labelY: y,
      labelWidth: textWidth,
      side,
      size,
      font,
      weight
    });
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

export function badgeColorForUpdate(updateType: SigmaVersionUpdateType): BadgeColor {
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

export function versionBadgeForNode(node: Pick<Partial<SigmaNodeAttributes>, "updateBadge" | "updateType">): VersionBadge | null {
  if (!node.updateBadge || !node.updateType) {
    return null;
  }
  return {
    text: node.updateBadge,
    color: badgeColorForUpdate(node.updateType)
  };
}

function drawBadge(
  context: CanvasRenderingContext2D,
  params: {
    badge: VersionBadge;
    labelX: number;
    labelY: number;
    labelWidth: number;
    side: "left" | "right";
    size: number;
    font: string;
    weight: string;
  }
): void {
  const { badge, font, labelWidth, labelX, labelY, side, size, weight } = params;
  const badgeFontSize = Math.max(9, size - 2);
  const horizontalPadding = 6;
  const height = Math.max(16, badgeFontSize + 7);
  const gap = 6;

  context.font = `${weight} ${badgeFontSize}px ${font}`;
  const badgeWidth = Math.max(18, context.measureText(badge.text).width + horizontalPadding * 2);
  const x = side === "left" ? labelX - gap - badgeWidth : labelX + labelWidth + gap;
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
