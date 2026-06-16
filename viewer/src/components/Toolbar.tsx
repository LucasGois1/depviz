import { Eye, EyeOff, FilterX, Maximize2, Network, RotateCcw, Search } from "lucide-react";
import { scopeIsEnabled } from "../filtering";
import { layoutDisplayName } from "../graph";
import type { DepvizDocument, FilterState, LayoutName, OptionalMode, SecurityFilterMode, UpdateFilterMode, VersionSummary } from "../types";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Select, SelectItem } from "./ui/select";

interface ToolbarProps {
  document: DepvizDocument;
  filters: FilterState;
  layout: LayoutName;
  scopes: string[];
  showLabels: boolean;
  onSearchChange: (search: string) => void;
  onScopeChange: (scope: string, enabled: boolean) => void;
  onOptionalModeChange: (mode: OptionalMode) => void;
  onUpdateModeChange: (mode: UpdateFilterMode) => void;
  onSecurityModeChange: (mode: SecurityFilterMode) => void;
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
  onSearchChange,
  onScopeChange,
  onOptionalModeChange,
  onUpdateModeChange,
  onSecurityModeChange,
  onLayoutChange,
  onShowLabelsChange,
  onFit,
  onReset,
  onClearFilters
}: ToolbarProps) {
  return (
    <header className="toolbar">
      <div className="toolbar-primary">
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

        <label className="search-box">
          <Search aria-hidden="true" />
          <Input
            value={filters.search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search coordinates, scope, version..."
            aria-label="Search dependency graph"
          />
        </label>

        <div className="canvas-actions" aria-label="Canvas actions">
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
      </div>

      <div className="toolbar-secondary">
        <section className="command-group command-group-scopes">
          <span className="command-label">Scopes</span>
          <div className="control-group" aria-label="Scope filters">
            {scopes.map((scope) => (
              <label className="check-chip" key={scope}>
                <input
                  type="checkbox"
                  checked={scopeIsEnabled(filters, scope)}
                  onChange={(event) => onScopeChange(scope, event.target.checked)}
                />
                <span>{scope}</span>
              </label>
            ))}
          </div>
        </section>

        <section className="command-group">
          <div className="control-field">
            <span>Deps</span>
            <Select value={filters.optionalMode} onValueChange={(value) => onOptionalModeChange(value as OptionalMode)} label="Optional dependency filter">
              <SelectItem value="all">All deps</SelectItem>
              <SelectItem value="required">Required only</SelectItem>
              <SelectItem value="optional">Optional only</SelectItem>
            </Select>
          </div>
        </section>

        {document.versionSummary?.enabled ? (
          <section className="command-group">
            <div className="control-field">
              <span>Updates</span>
              <Select value={filters.updateMode} onValueChange={(value) => onUpdateModeChange(value as UpdateFilterMode)} label="Update filter">
                <SelectItem value="all">All dependencies</SelectItem>
                <SelectItem value="outdated">{updateLabel("Outdated", document.versionSummary, "outdated")}</SelectItem>
                <SelectItem value="major">{updateLabel("Major", document.versionSummary, "major")}</SelectItem>
                <SelectItem value="minor">{updateLabel("Minor", document.versionSummary, "minor")}</SelectItem>
                <SelectItem value="patch">{updateLabel("Patch", document.versionSummary, "patch")}</SelectItem>
                <SelectItem value="unknown">{updateLabel("Unknown", document.versionSummary, "unknown")}</SelectItem>
                <SelectItem value="unavailable">{updateLabel("Unavailable", document.versionSummary, "unavailable")}</SelectItem>
              </Select>
            </div>
          </section>
        ) : null}

        {document.securitySummary?.checked ? (
          <section className="command-group">
            <div className="control-field">
              <span>Risk</span>
              <Select value={filters.securityMode} onValueChange={(value) => onSecurityModeChange(value as SecurityFilterMode)} label="Security filter">
                <SelectItem value="all">All security</SelectItem>
                <SelectItem value="vulnerable">Vulnerable</SelectItem>
                <SelectItem value="critical">Critical</SelectItem>
                <SelectItem value="high">High</SelectItem>
                <SelectItem value="medium">Medium</SelectItem>
                <SelectItem value="low">Low</SelectItem>
              </Select>
            </div>
          </section>
        ) : null}

        <section className="command-group">
          <div className="control-field">
            <span>View</span>
            <Select value={layout} onValueChange={(value) => onLayoutChange(value as LayoutName)} label="Graph layout">
              {layouts.map((option) => (
                <SelectItem key={option} value={option}>
                  {layoutDisplayName(option)}
                </SelectItem>
              ))}
            </Select>
          </div>
        </section>

        <section className="command-group">
          <span className="command-label">Labels</span>
          <div className="label-mode" aria-label="Label density">
            <button type="button" aria-pressed={!showLabels} onClick={() => onShowLabelsChange(false)} title="Show key labels only">
              <EyeOff aria-hidden="true" />
              Key
            </button>
            <button type="button" aria-pressed={showLabels} onClick={() => onShowLabelsChange(true)} title="Show all labels">
              <Eye aria-hidden="true" />
              All
            </button>
          </div>
        </section>
      </div>
    </header>
  );
}

function updateLabel(label: string, summary: VersionSummary, key: keyof Pick<VersionSummary, "outdated" | "major" | "minor" | "patch" | "unknown" | "unavailable">) {
  return `${label} (${summary[key]})`;
}
