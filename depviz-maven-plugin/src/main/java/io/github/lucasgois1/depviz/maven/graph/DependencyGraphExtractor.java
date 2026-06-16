package io.github.lucasgois1.depviz.maven.graph;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.graph.ExtractedDependencyNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public interface DependencyGraphExtractor {
    ExtractedDependencyNode extract(MavenProject project, DepvizConfig config) throws MojoExecutionException;
}
