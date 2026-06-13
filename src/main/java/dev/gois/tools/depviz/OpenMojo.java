package dev.gois.tools.depviz;

import dev.gois.tools.depviz.config.DepvizConfig;
import dev.gois.tools.depviz.graph.ExtractedDependencyNode;
import dev.gois.tools.depviz.graph.GraphDocument;
import dev.gois.tools.depviz.graph.GraphDocumentBuilder;
import dev.gois.tools.depviz.graph.MavenDependencyGraphExtractor;
import dev.gois.tools.depviz.graph.ProjectInfo;
import dev.gois.tools.depviz.output.BrowserOpener;
import dev.gois.tools.depviz.output.OutputFiles;
import dev.gois.tools.depviz.output.ViewerWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

@Mojo(name = "open", requiresProject = true, threadSafe = true)
public final class OpenMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(property = "depviz.scope")
    private String scope;

    @Parameter(property = "depviz.open")
    private String open;

    @Parameter(property = "depviz.includes")
    private String includes;

    @Parameter(property = "depviz.excludes")
    private String excludes;

    @Parameter(property = "depviz.layout")
    private String layout;

    @Parameter(property = "depviz.nodeMode")
    private String nodeMode;

    @Parameter(property = "depviz.maxInitialLabels")
    private String maxInitialLabels;

    @Parameter(property = "depviz.outputDirectory")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        DepvizConfig config = parseConfig();
        ExtractedDependencyNode root = new MavenDependencyGraphExtractor(dependencyGraphBuilder).extract(project, config);
        GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo(), config);
        OutputFiles outputFiles = write(document, config);
        URI htmlUri = outputFiles.htmlFile().toAbsolutePath().normalize().toUri();

        getLog().info("Generated depviz viewer: " + outputFiles.htmlFile().toAbsolutePath().normalize());
        if (config.open()) {
            if (!new BrowserOpener().open(htmlUri)) {
                getLog().warn("Unable to open browser automatically. Open this URI manually: " + htmlUri);
            }
            return;
        }
        getLog().info("Open this URI manually: " + htmlUri);
    }

    private DepvizConfig parseConfig() throws MojoExecutionException {
        try {
            return DepvizConfig.fromRaw(
                scope,
                open,
                includes,
                layout,
                nodeMode,
                maxInitialLabels,
                excludes,
                outputDirectory == null ? null : outputDirectory.toPath()
            );
        } catch (IllegalArgumentException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }
    }

    private OutputFiles write(GraphDocument document, DepvizConfig config) throws MojoExecutionException {
        try {
            return new ViewerWriter().write(document, config.outputDirectory());
        } catch (IOException exception) {
            throw new MojoExecutionException("Failed to write depviz viewer: " + exception.getMessage(), exception);
        }
    }

    private ProjectInfo projectInfo() {
        List<String> modules = project.getModules() == null ? List.of() : List.copyOf(project.getModules());
        File basedir = project.getBasedir();
        return new ProjectInfo(
            project.getGroupId(),
            project.getArtifactId(),
            project.getVersion(),
            project.getPackaging(),
            project.getName(),
            basedir == null ? null : basedir.toPath().toAbsolutePath().normalize().toString(),
            !modules.isEmpty(),
            modules
        );
    }
}
