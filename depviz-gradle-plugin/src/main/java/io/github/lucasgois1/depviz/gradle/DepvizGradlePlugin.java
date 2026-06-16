package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class DepvizGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        DepvizExtension extension = project.getExtensions().create("depviz", DepvizExtension.class, project.getObjects());
        extension.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("depviz"));
        project.getTasks().register("depvizOpen", DepvizOpenTask.class, task -> {
            task.setGroup("Depviz");
            task.setDescription("Generates and opens the Depviz dependency graph viewer.");
        });
    }
}
