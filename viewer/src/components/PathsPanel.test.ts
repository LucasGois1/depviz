import { describe, expect, it } from "vitest";
import { pathsForSelectedNode } from "./PathsPanel";
import type { DepvizDocument, GraphNode } from "../types";

describe("pathsForSelectedNode", () => {
  it("returns route cards with clickable steps for the selected dependency", () => {
    const selected = node("org.shared:logging:jar::2.0.0", "org.shared", "logging", 2);
    const document = documentWithPaths(selected);
    const nodeById = new Map(document.nodes.map((current) => [current.id, current]));

    expect(pathsForSelectedNode(document, nodeById, selected)).toEqual([
      {
        id: "org.shared:logging:jar::2.0.0-0",
        depth: 2,
        stepCount: 3,
        steps: [
          expect.objectContaining({ id: "dev.example:demo:jar::1.0.0", label: "dev.example:demo", scope: "root", position: "entry" }),
          expect.objectContaining({ id: "org.alpha:client:jar::1.0.0", label: "org.alpha:client", scope: "compile", position: "middle" }),
          expect.objectContaining({ id: selected.id, label: "org.shared:logging", scope: "compile", position: "target" })
        ]
      }
    ]);
  });

  it("returns no route cards when nothing is selected", () => {
    const selected = node("org.shared:logging:jar::2.0.0", "org.shared", "logging", 2);
    const document = documentWithPaths(selected);
    const nodeById = new Map(document.nodes.map((current) => [current.id, current]));

    expect(pathsForSelectedNode(document, nodeById, null)).toEqual([]);
  });
});

function documentWithPaths(selected: GraphNode): DepvizDocument {
  const root = node("dev.example:demo:jar::1.0.0", "dev.example", "demo", 0, true);
  const client = node("org.alpha:client:jar::1.0.0", "org.alpha", "client", 1);

  return {
    schemaVersion: "1.0",
    generatedAt: "2026-06-16T00:00:00Z",
    project: {
      groupId: "dev.example",
      artifactId: "demo",
      version: "1.0.0",
      packaging: "jar",
      name: "Demo",
      baseDirectory: "/repo",
      multiModule: false,
      modules: []
    },
    summary: {
      nodeCount: 3,
      edgeCount: 2,
      nodesByScope: { root: 1, compile: 2 },
      nodesByGroupId: { "dev.example": 1, "org.alpha": 1, "org.shared": 1 }
    },
    viewerConfig: { initialLayout: "force", maxInitialLabels: 500, nodeMode: "artifact" },
    nodes: [root, client, selected],
    edges: [],
    paths: [{ target: selected.id, nodeIds: [root.id, client.id, selected.id] }],
    diagnostics: []
  };
}

function node(id: string, groupId: string, artifactId: string, depth: number, root = false): GraphNode {
  return {
    id,
    groupId,
    artifactId,
    version: "1.0.0",
    type: "jar",
    classifier: "",
    scope: root ? "root" : "compile",
    optional: false,
    depth,
    root,
    moduleRoot: false,
    label: `${groupId}:${artifactId}`,
    coordinate: id,
    groupColorKey: groupId
  };
}
