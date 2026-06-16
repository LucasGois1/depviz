package io.github.lucasgois1.depviz.graph;

import java.util.Objects;

public record DependencyGraphInput(
    DependencyNodeInput root,
    ProjectInfo project
) {
    public DependencyGraphInput {
        root = Objects.requireNonNull(root, "root is required.");
        project = Objects.requireNonNull(project, "project is required.");
    }
}
