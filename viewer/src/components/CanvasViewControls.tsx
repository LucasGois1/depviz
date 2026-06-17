import { Eye, EyeOff, Maximize2, RotateCcw, ShieldAlert, SlidersHorizontal, Tags } from "lucide-react";
import type { ReactNode } from "react";
import { layoutDisplayName } from "../graph";
import type { LayoutName } from "../types";
import { Button } from "./ui/button";
import { Select, SelectItem } from "./ui/select";

interface CanvasViewControlsProps {
  layout: LayoutName;
  showLabels: boolean;
  showVersionBadges: boolean;
  showSecurityBadges: boolean;
  onLayoutChange: (layout: LayoutName) => void;
  onShowLabelsChange: (showLabels: boolean) => void;
  onShowVersionBadgesChange: (showVersionBadges: boolean) => void;
  onShowSecurityBadgesChange: (showSecurityBadges: boolean) => void;
  onFit: () => void;
  onReset: () => void;
}

const layouts: LayoutName[] = ["breadthfirst", "force", "circle", "concentric"];

export function CanvasViewControls({
  layout,
  showLabels,
  showVersionBadges,
  showSecurityBadges,
  onLayoutChange,
  onShowLabelsChange,
  onShowVersionBadgesChange,
  onShowSecurityBadgesChange,
  onFit,
  onReset
}: CanvasViewControlsProps) {
  return (
    <div className="canvas-view-controls" aria-label="Canvas view controls">
      <div className="canvas-view-heading">
        <SlidersHorizontal aria-hidden="true" />
        <span>View</span>
      </div>
      <div className="canvas-view-layout">
        <span>Layout</span>
        <Select value={layout} onValueChange={(value) => onLayoutChange(value as LayoutName)} label="Graph layout">
          {layouts.map((option) => (
            <SelectItem key={option} value={option}>
              {layoutDisplayName(option)}
            </SelectItem>
          ))}
        </Select>
      </div>
      <div className="canvas-view-toggles" aria-label="Canvas visibility toggles">
        <CanvasToggle
          active={showLabels}
          icon={showLabels ? <Eye aria-hidden="true" /> : <EyeOff aria-hidden="true" />}
          label="Labels"
          onClick={() => onShowLabelsChange(!showLabels)}
        />
        <CanvasToggle
          active={showVersionBadges}
          icon={<Tags aria-hidden="true" />}
          label="Versions"
          onClick={() => onShowVersionBadgesChange(!showVersionBadges)}
        />
        <CanvasToggle
          active={showSecurityBadges}
          icon={<ShieldAlert aria-hidden="true" />}
          label="Risk"
          onClick={() => onShowSecurityBadgesChange(!showSecurityBadges)}
        />
      </div>
      <div className="canvas-view-actions" aria-label="Canvas viewport actions">
        <Button variant="outline" size="icon" onClick={onFit} title="Fit graph" aria-label="Fit graph">
          <Maximize2 aria-hidden="true" />
        </Button>
        <Button variant="outline" size="icon" onClick={onReset} title="Reset layout" aria-label="Reset layout">
          <RotateCcw aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}

function CanvasToggle({ active, icon, label, onClick }: { active: boolean; icon: ReactNode; label: string; onClick: () => void }) {
  return (
    <button type="button" className="canvas-toggle" aria-pressed={active} onClick={onClick} title={label}>
      {icon}
      <span>{label}</span>
    </button>
  );
}
