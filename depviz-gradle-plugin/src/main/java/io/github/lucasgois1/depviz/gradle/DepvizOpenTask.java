package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class DepvizOpenTask extends DefaultTask {
    @TaskAction
    public void open() {
        getLogger().lifecycle("Depviz Gradle plugin is registered.");
    }
}
