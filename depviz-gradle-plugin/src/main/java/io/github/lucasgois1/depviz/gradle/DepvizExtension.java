package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public abstract class DepvizExtension {
    public DepvizExtension(ObjectFactory objects) {
        getScope().convention("runtime");
        getOpen().convention(true);
        getSnyk().convention("auto");
        getLayout().convention("breadthfirst");
    }

    public abstract Property<String> getScope();

    public abstract Property<Boolean> getOpen();

    public abstract Property<String> getSnyk();

    public abstract Property<String> getLayout();

    public abstract RegularFileProperty getSnykJson();
}
