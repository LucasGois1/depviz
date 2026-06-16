package io.github.lucasgois1.depviz.gradle;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.graph.DependencyNodeInput;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.graph.GraphDocumentBuilder;
import io.github.lucasgois1.depviz.graph.ProjectInfo;
import io.github.lucasgois1.depviz.output.BrowserOpener;
import io.github.lucasgois1.depviz.output.OutputFiles;
import io.github.lucasgois1.depviz.output.ViewerWriter;
import io.github.lucasgois1.depviz.security.SecurityCheckResult;
import io.github.lucasgois1.depviz.security.SecurityGraphEnricher;
import io.github.lucasgois1.depviz.security.SnykRunner;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public abstract class DepvizOpenTask extends DefaultTask {
    @TaskAction
    public void open() {
        Project project = getProject();
        DepvizExtension extension = project.getExtensions().getByType(DepvizExtension.class);
        DepvizConfig config = DepvizConfig.fromRaw(
            extension.getScope().get(),
            Boolean.toString(extension.getOpen().get()),
            null,
            extension.getLayout().get(),
            null,
            null,
            null,
            "false",
            extension.getOutputDirectory().get().getAsFile().toPath(),
            extension.getSnyk().get(),
            extension.getSnykJson().isPresent() ? extension.getSnykJson().get().getAsFile().toPath().toString() : null,
            null,
            null,
            null
        );
        Project rootProject = project.getRootProject();
        DependencyNodeInput root = new GradleDependencyGraphExtractor().extractAggregate(rootProject, config.scope().value());
        List<String> modules = rootProject.getSubprojects().stream()
            .sorted(Comparator.comparing(Project::getPath))
            .map(Project::getPath)
            .toList();
        ProjectInfo projectInfo = new ProjectInfo(
            root.coordinate().groupId(),
            root.coordinate().artifactId(),
            root.coordinate().version(),
            "gradle",
            rootProject.getName(),
            rootProject.getProjectDir().toPath().toAbsolutePath().normalize().toString(),
            !modules.isEmpty(),
            modules
        );
        GraphDocument initialDocument = new GraphDocumentBuilder().build(root, projectInfo, config, VersionCheckResult.disabled());
        SecurityCheckResult securityCheck = new SnykRunner().run(config, rootProject.getProjectDir().toPath());
        GraphDocument document = new SecurityGraphEnricher().enrich(initialDocument, securityCheck);
        try {
            OutputFiles output = new ViewerWriter().write(document, config.outputDirectory());
            URI htmlUri = output.htmlFile().toAbsolutePath().normalize().toUri();

            getLogger().lifecycle("Generated depviz viewer: {}", output.htmlFile().toAbsolutePath().normalize());
            if (config.open()) {
                if (!new BrowserOpener().open(htmlUri)) {
                    getLogger().warn("Unable to open browser automatically. Open this URI manually: {}", htmlUri);
                }
                return;
            }
            getLogger().lifecycle("Open this URI manually: {}", htmlUri);
        } catch (IOException exception) {
            throw new GradleException("Failed to write depviz viewer: " + exception.getMessage(), exception);
        }
    }
}
