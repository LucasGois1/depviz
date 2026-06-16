package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutableSelectorTest {
    @TempDir
    Path dir;

    @Test
    void prefersMavenWrapperWhenExecutable() throws Exception {
        Path wrapper = dir.resolve("mvnw");
        Files.writeString(wrapper, "#!/bin/sh\n");
        wrapper.toFile().setExecutable(true);
        assertThat(ExecutableSelector.maven(dir)).isEqualTo("./mvnw");
    }

    @Test
    void fallsBackToMavenWhenWrapperMissing() {
        assertThat(ExecutableSelector.maven(dir)).isEqualTo("mvn");
    }

    @Test
    void prefersGradleWrapperWhenExecutable() throws Exception {
        Path wrapper = dir.resolve("gradlew");
        Files.writeString(wrapper, "#!/bin/sh\n");
        wrapper.toFile().setExecutable(true);
        assertThat(ExecutableSelector.gradle(dir)).isEqualTo("./gradlew");
    }
}
