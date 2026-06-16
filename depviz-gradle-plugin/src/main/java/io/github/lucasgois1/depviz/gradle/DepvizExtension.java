package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public abstract class DepvizExtension {
    public DepvizExtension(ObjectFactory objects) {
        getScope().convention("runtime");
        getOpen().convention(true);
        getSnyk().convention("auto");
        getSnykCommand().convention("snyk");
        getSnykAllProjects().convention(false);
        getLayout().convention("breadthfirst");
    }

    public abstract Property<String> getScope();

    public abstract Property<Boolean> getOpen();

    public abstract Property<String> getSnyk();

    public abstract Property<String> getSnykCommand();

    public abstract Property<String> getSnykOrg();

    public abstract Property<Boolean> getSnykAllProjects();

    public abstract Property<String> getLayout();

    public abstract DirectoryProperty getOutputDirectory();

    public abstract RegularFileProperty getSnykJson();
}
