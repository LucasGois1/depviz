import { Circle, Crosshair, Eye, EyeOff, FilterX, GitBranch, Maximize2, Network, RotateCcw, Search } from "lucide-react";
import { layoutDisplayName } from "../graph";
import type { DepvizDocument, FilterState, LayoutName, OptionalMode, UpdateFilterMode, VersionSummary } from "../types";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Select, SelectItem } from "./ui/select";

interface ToolbarProps {
  document: DepvizDocument;
  filters: FilterState;
  layout: LayoutName;
  scopes: string[];
  showLabels: boolean;
  visibleCount: number;
  onSearchChange: (search: string) => void;
  onScopeChange: (scope: string, enabled: boolean) => void;
  onOptionalModeChange: (mode: OptionalMode) => void;
  onUpdateModeChange: (mode: UpdateFilterMode) => void;
  onLayoutChange: (layout: LayoutName) => void;
  onShowLabelsChange: (showLabels: boolean) => void;
  onFit: () => void;
  onReset: () => void;
  onClearFilters: () => void;
}

const layouts: LayoutName[] = ["breadthfirst", "force", "circle", "concentric"];

export function Toolbar({
  document,
  filters,
  layout,
  scopes,
  showLabels,
  visibleCount,
  onSearchChange,
  onScopeChange,
  onOptionalModeChange,
  onUpdateModeChange,
  onLayoutChange,
  onShowLabelsChange,
  onFit,
  onReset,
  onClearFilters
}: ToolbarProps) {
  return (
    <header className="toolbar">
      <div className="title-cluster">
        <div className="app-mark">
          <Network aria-hidden="true" />
        </div>
        <div>
          <h1>{document.project.artifactId}</h1>
          <p>
            {document.project.groupId}:{document.project.version}
          </p>
        </div>
      </div>

      <div className="toolbar-controls">
        <label className="search-box">
          <Search aria-hidden="true" />
          <Input
            value={filters.search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search coordinates, scope, version..."
            aria-label="Search dependency graph"
          />
        </label>

        <div className="control-group" aria-label="Scope filters">
          {scopes.map((scope) => (
            <label className="check-chip" key={scope}>
              <input
                type="checkbox"
                checked={filters.scopes.has(scope)}
                onChange={(event) => onScopeChange(scope, event.target.checked)}
              />
              <span>{scope}</span>
            </label>
          ))}
        </div>

        <Select value={filters.optionalMode} onValueChange={(value) => onOptionalModeChange(value as OptionalMode)} label="Optional dependency filter">
          <SelectItem value="all">All deps</SelectItem>
          <SelectItem value="required">Required only</SelectItem>
          <SelectItem value="optional">Optional only</SelectItem>
        </Select>

        {document.versionSummary?.enabled ? (
          <Select value={filters.updateMode} onValueChange={(value) => onUpdateModeChange(value as UpdateFilterMode)} label="Update filter">
            <SelectItem value="all">All updates</SelectItem>
            <SelectItem value="outdated">{updateLabel("Outdated", document.versionSummary, "outdated")}</SelectItem>
            <SelectItem value="major">{updateLabel("Major", document.versionSummary, "major")}</SelectItem>
            <SelectItem value="minor">{updateLabel("Minor", document.versionSummary, "minor")}</SelectItem>
            <SelectItem value="patch">{updateLabel("Patch", document.versionSummary, "patch")}</SelectItem>
            <SelectItem value="unknown">{updateLabel("Unknown", document.versionSummary, "unknown")}</SelectItem>
            <SelectItem value="unavailable">{updateLabel("Unavailable", document.versionSummary, "unavailable")}</SelectItem>
          </Select>
        ) : null}

        <Select value={layout} onValueChange={(value) => onLayoutChange(value as LayoutName)} label="Graph layout">
          {layouts.map((option) => (
            <SelectItem key={option} value={option}>
              {layoutDisplayName(option)}
            </SelectItem>
          ))}
        </Select>

        <Button variant="outline" size="sm" onClick={() => onShowLabelsChange(!showLabels)} title="Toggle all labels">
          {showLabels ? <Eye aria-hidden="true" data-icon="inline-start" /> : <EyeOff aria-hidden="true" data-icon="inline-start" />}
          All labels
        </Button>
        <Button variant="outline" size="icon" onClick={onFit} title="Fit graph" aria-label="Fit graph">
          <Maximize2 aria-hidden="true" />
        </Button>
        <Button variant="outline" size="icon" onClick={onReset} title="Reset layout" aria-label="Reset layout">
          <RotateCcw aria-hidden="true" />
        </Button>
        <Button variant="ghost" size="icon" onClick={onClearFilters} title="Clear filters" aria-label="Clear filters">
          <FilterX aria-hidden="true" />
        </Button>
      </div>

      <div className="toolbar-metrics" aria-label="Graph summary">
        <Metric icon={<Circle aria-hidden="true" />} label="Nodes" value={`${visibleCount}/${document.summary.nodeCount}`} />
        <Metric icon={<GitBranch aria-hidden="true" />} label="Edges" value={`${document.summary.edgeCount}`} />
        <Metric icon={<Crosshair aria-hidden="true" />} label="Layout" value={layoutDisplayName(layout)} />
      </div>
    </header>
  );
}

function updateLabel(label: string, summary: VersionSummary, key: keyof Pick<VersionSummary, "outdated" | "major" | "minor" | "patch" | "unknown" | "unavailable">) {
  return `${label} (${summary[key]})`;
}

function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="metric">
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
