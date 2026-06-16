package io.github.lucasgois1.depviz.graph;

import io.github.lucasgois1.depviz.config.DepvizScope;
import io.github.lucasgois1.depviz.util.PatternMatcher;
import java.util.ArrayList;
import java.util.List;

public final class GraphFilters {
    private GraphFilters() {
    }

    public static ExtractedDependencyNode apply(
        ExtractedDependencyNode root,
        DepvizScope scope,
        List<PatternMatcher> includes,
        List<PatternMatcher> excludes
    ) {
        ExtractedDependencyNode scopeFiltered = filterByScope(root, scope, true);
        if (scopeFiltered == null) {
            return copyWithChildren(root, List.of());
        }
        ExtractedDependencyNode excluded = filterExcludes(scopeFiltered, excludes, true);
        if (excluded == null) {
            return copyWithChildren(scopeFiltered, List.of());
        }
        if (includes == null || includes.isEmpty()) {
            return excluded;
        }
        ExtractedDependencyNode included = filterIncludes(excluded, includes, true);
        return included == null ? copyWithChildren(excluded, List.of()) : included;
    }

    private static ExtractedDependencyNode filterByScope(ExtractedDependencyNode node, DepvizScope scope, boolean root) {
        List<ExtractedDependencyNode> children = new ArrayList<>();
        for (ExtractedDependencyNode child : node.children()) {
            ExtractedDependencyNode filtered = filterByScope(child, scope, false);
            if (filtered != null) {
                children.add(filtered);
            }
        }
        if (!root && !scope.includesDependencyScope(node.scope())) {
            return null;
        }
        return copyWithChildren(node, children);
    }

    private static ExtractedDependencyNode filterExcludes(
        ExtractedDependencyNode node,
        List<PatternMatcher> excludes,
        boolean root
    ) {
        if (!root && matchesAny(excludes, node.coordinate())) {
            return null;
        }
        List<ExtractedDependencyNode> children = new ArrayList<>();
        for (ExtractedDependencyNode child : node.children()) {
            ExtractedDependencyNode filtered = filterExcludes(child, excludes, false);
            if (filtered != null) {
                children.add(filtered);
            }
        }
        return copyWithChildren(node, children);
    }

    private static ExtractedDependencyNode filterIncludes(
        ExtractedDependencyNode node,
        List<PatternMatcher> includes,
        boolean root
    ) {
        List<ExtractedDependencyNode> children = new ArrayList<>();
        for (ExtractedDependencyNode child : node.children()) {
            ExtractedDependencyNode filtered = filterIncludes(child, includes, false);
            if (filtered != null) {
                children.add(filtered);
            }
        }
        if (root || matchesAny(includes, node.coordinate()) || !children.isEmpty()) {
            return copyWithChildren(node, children);
        }
        return null;
    }

    private static boolean matchesAny(List<PatternMatcher> patterns, ArtifactCoordinate coordinate) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> pattern.matches(coordinate));
    }

    private static ExtractedDependencyNode copyWithChildren(
        ExtractedDependencyNode node,
        List<ExtractedDependencyNode> children
    ) {
        return new ExtractedDependencyNode(node.coordinate(), node.scope(), node.optional(), children, node.diagnostics());
    }
}
