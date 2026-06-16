package io.github.lucasgois1.depviz;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.graph.ExtractedDependencyNode;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.graph.GraphDocumentBuilder;
import io.github.lucasgois1.depviz.graph.MavenDependencyGraphExtractor;
import io.github.lucasgois1.depviz.graph.ProjectInfo;
import io.github.lucasgois1.depviz.graph.ReactorDependencyGraphExtractor;
import io.github.lucasgois1.depviz.output.BrowserOpener;
import io.github.lucasgois1.depviz.output.OutputFiles;
import io.github.lucasgois1.depviz.output.ViewerWriter;
import io.github.lucasgois1.depviz.security.SecurityCheckResult;
import io.github.lucasgois1.depviz.security.SecurityGraphEnricher;
import io.github.lucasgois1.depviz.security.SnykRunner;
import io.github.lucasgois1.depviz.version.MavenVersionLookup;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import io.github.lucasgois1.depviz.version.VersionUpdateChecker;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;

@Mojo(name = "open", requiresProject = true, threadSafe = true, aggregator = true)
public final class OpenMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Component
    private DependencyCollectorBuilder dependencyCollectorBuilder;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepositories;

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

    @Parameter(property = "depviz.checkUpdates")
    private String checkUpdates;

    @Parameter(property = "depviz.outputDirectory", defaultValue = "${project.build.directory}/depviz")
    private File outputDirectory;

    @Parameter(property = "depviz.snyk")
    private String snyk;

    @Parameter(property = "depviz.snykJson")
    private String snykJson;

    @Parameter(property = "depviz.snykCommand")
    private String snykCommand;

    @Parameter(property = "depviz.snykOrg")
    private String snykOrg;

    @Parameter(property = "depviz.snykAllProjects")
    private String snykAllProjects;

    @Override
    public void execute() throws MojoExecutionException {
        DepvizConfig config = parseConfig();
        ExtractedDependencyNode root = new ReactorDependencyGraphExtractor(
            new MavenDependencyGraphExtractor(dependencyCollectorBuilder)
        ).extract(project, reactorProjects, config);
        VersionCheckResult versionCheck = checkVersions(root, config);
        GraphDocument initialDocument = new GraphDocumentBuilder().build(root, projectInfo(), config, versionCheck);
        SecurityCheckResult securityCheck = checkSecurity(config);
        GraphDocument document = new SecurityGraphEnricher().enrich(initialDocument, securityCheck);
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
                checkUpdates,
                outputDirectory == null ? null : outputDirectory.toPath(),
                snyk,
                snykJson,
                snykCommand,
                snykOrg,
                snykAllProjects
            );
        } catch (IllegalArgumentException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }
    }

    private VersionCheckResult checkVersions(ExtractedDependencyNode root, DepvizConfig config) {
        try {
            return new VersionUpdateChecker(
                new MavenVersionLookup(repositorySystem, repositorySystemSession, remoteRepositories)
            ).check(root, config.checkUpdates());
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
            getLog().warn("Version update check failed: " + message);
            return VersionCheckResult.failed(config.checkUpdates(), message);
        }
    }

    private SecurityCheckResult checkSecurity(DepvizConfig config) {
        return new SnykRunner().run(config, project.getBasedir() == null ? null : project.getBasedir().toPath());
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
