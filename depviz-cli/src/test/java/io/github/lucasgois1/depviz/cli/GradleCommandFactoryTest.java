package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GradleCommandFactoryTest {
    @Test
    void buildsCommandUsingInitScriptAndDepvizOpenTask() {
        assertThat(new GradleCommandFactory().command("gradle", Path.of("/tmp/depviz.gradle"))).containsExactly(
            "gradle",
            "--init-script",
            "/tmp/depviz.gradle",
            ":depvizOpen"
        );
    }
}
