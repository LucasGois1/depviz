import { useEffect, useMemo, useState, type KeyboardEvent } from "react";
import { FilterX, Network, Search, X } from "lucide-react";
import { scopeIsEnabled } from "../filtering";
import { nextSuggestionIndex, type SearchSuggestion } from "../search";
import type { DepvizDocument, FilterState, OptionalMode, SecurityFilterMode, UpdateFilterMode, VersionSummary } from "../types";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Select, SelectItem } from "./ui/select";

interface ToolbarProps {
  document: DepvizDocument;
  filters: FilterState;
  scopes: string[];
  searchSuggestions: SearchSuggestion[];
  searchMatchCount: number;
  onSearchChange: (search: string) => void;
  onSearchSuggestionSelect: (nodeId: string) => void;
  onScopeChange: (scope: string, enabled: boolean) => void;
  onOptionalModeChange: (mode: OptionalMode) => void;
  onUpdateModeChange: (mode: UpdateFilterMode) => void;
  onSecurityModeChange: (mode: SecurityFilterMode) => void;
  onClearFilters: () => void;
}

export function Toolbar({
  document,
  filters,
  scopes,
  searchSuggestions,
  searchMatchCount,
  onSearchChange,
  onSearchSuggestionSelect,
  onScopeChange,
  onOptionalModeChange,
  onUpdateModeChange,
  onSecurityModeChange,
  onClearFilters
}: ToolbarProps) {
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
  const searchQuery = filters.search.trim();
  const searchActive = searchQuery.length > 0;
  const hasSuggestions = searchSuggestions.length > 0;
  const activeSuggestion = activeSuggestionIndex >= 0 ? searchSuggestions[activeSuggestionIndex] : null;
  const searchShellClassName = useMemo(
    () =>
      [
        "search-shell",
        searchActive ? "is-searching" : "",
        suggestionsOpen && searchActive ? "is-open" : "",
        searchActive && searchMatchCount === 0 ? "has-no-results" : ""
      ]
        .filter(Boolean)
        .join(" "),
    [searchActive, searchMatchCount, suggestionsOpen]
  );

  useEffect(() => {
    setActiveSuggestionIndex(searchSuggestions.length > 0 ? 0 : -1);
  }, [searchQuery, searchSuggestions.length]);

  const selectSuggestion = (suggestion: SearchSuggestion) => {
    onSearchSuggestionSelect(suggestion.nodeId);
    setSuggestionsOpen(false);
  };

  const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setSuggestionsOpen(searchActive);
      setActiveSuggestionIndex((current) => nextSuggestionIndex(current, searchSuggestions.length, 1));
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      setSuggestionsOpen(searchActive);
      setActiveSuggestionIndex((current) => nextSuggestionIndex(current, searchSuggestions.length, -1));
      return;
    }

    if (event.key === "Enter" && suggestionsOpen && hasSuggestions) {
      event.preventDefault();
      selectSuggestion(activeSuggestion ?? searchSuggestions[0]);
      return;
    }

    if (event.key === "Escape") {
      if (suggestionsOpen) {
        event.preventDefault();
        setSuggestionsOpen(false);
        return;
      }
      if (searchActive) {
        event.preventDefault();
        onSearchChange("");
      }
    }
  };

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

        <div
          className={searchShellClassName}
          onBlur={(event) => {
            const nextFocusedElement = event.relatedTarget instanceof Node ? event.relatedTarget : null;
            if (!nextFocusedElement || !event.currentTarget.contains(nextFocusedElement)) {
              setSuggestionsOpen(false);
            }
          }}
        >
          <div className="search-box">
            <Search aria-hidden="true" />
            <Input
              value={filters.search}
              onChange={(event) => {
                onSearchChange(event.target.value);
                setSuggestionsOpen(Boolean(event.target.value.trim()));
              }}
              onFocus={() => setSuggestionsOpen(searchActive)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Search artifact, group, scope, version..."
              aria-label="Search dependency graph"
              role="combobox"
              aria-expanded={suggestionsOpen && searchActive}
              aria-controls="dependency-search-suggestions"
              aria-activedescendant={activeSuggestion ? `dependency-search-suggestion-${activeSuggestionIndex}` : undefined}
              autoComplete="off"
            />
            {searchActive ? (
              <span className="search-live-indicator" aria-label="Search is filtering live">
                live
              </span>
            ) : null}
            {searchActive ? <span className="search-count">{searchMatchCount === 1 ? "1 match" : `${searchMatchCount} matches`}</span> : null}
            {searchActive ? (
              <button
                className="search-clear"
                type="button"
                onClick={() => {
                  onSearchChange("");
                  setSuggestionsOpen(false);
                }}
                aria-label="Clear search"
                title="Clear search"
              >
                <X aria-hidden="true" />
              </button>
            ) : null}
          </div>

          {searchActive && suggestionsOpen ? (
            <div className="search-popover" id="dependency-search-suggestions" role="listbox" aria-label="Search suggestions">
              <div className="search-popover-header">
                <span>{searchMatchCount === 1 ? "1 dependency match" : `${searchMatchCount} dependency matches`}</span>
                <span>Enter selects</span>
              </div>
              {hasSuggestions ? (
                searchSuggestions.map((suggestion, index) => (
                  <button
                    className="search-suggestion"
                    id={`dependency-search-suggestion-${index}`}
                    key={suggestion.nodeId}
                    type="button"
                    role="option"
                    aria-selected={activeSuggestionIndex === index}
                    onMouseDown={(event) => event.preventDefault()}
                    onMouseEnter={() => setActiveSuggestionIndex(index)}
                    onClick={() => selectSuggestion(suggestion)}
                  >
                    <span className="search-suggestion-main">
                      <strong>
                        <HighlightedQuery text={suggestion.artifactId} query={searchQuery} />
                      </strong>
                      <span>{suggestion.description}</span>
                    </span>
                    <span className="search-suggestion-badges" aria-hidden="true">
                      {suggestion.badges.map((badge) => (
                        <span className={`search-badge search-badge-${badge.tone}`} key={`${suggestion.nodeId}-${badge.label}`}>
                          {badge.label}
                        </span>
                      ))}
                    </span>
                  </button>
                ))
              ) : (
                <div className="search-empty" role="status">
                  <strong>No dependency matches</strong>
                  <span>Try an artifact, group id, scope, classifier, or version.</span>
                </div>
              )}
            </div>
          ) : null}
        </div>

        <div className="filter-actions" aria-label="Filter actions">
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

      </div>
    </header>
  );
}

function updateLabel(label: string, summary: VersionSummary, key: keyof Pick<VersionSummary, "outdated" | "major" | "minor" | "patch" | "unknown" | "unavailable">) {
  return `${label} (${summary[key]})`;
}

function HighlightedQuery({ text, query }: { text: string; query: string }) {
  const start = text.toLowerCase().indexOf(query.toLowerCase());
  if (!query || start < 0) {
    return <>{text}</>;
  }

  const end = start + query.length;
  return (
    <>
      {text.slice(0, start)}
      <mark>{text.slice(start, end)}</mark>
      {text.slice(end)}
    </>
  );
}
