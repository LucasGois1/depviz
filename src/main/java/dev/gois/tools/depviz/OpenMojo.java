package dev.gois.tools.depviz;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "open", requiresProject = true, threadSafe = true)
public final class OpenMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Depviz plugin skeleton is available.");
    }
}
