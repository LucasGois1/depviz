package dev.gois.tools.depviz.graph;

import dev.gois.tools.depviz.config.DepvizConfig;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public interface DependencyGraphExtractor {
    ExtractedDependencyNode extract(MavenProject project, DepvizConfig config) throws MojoExecutionException;
}
