package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleInitScriptWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesInitScriptApplyingMatchingPluginVersionAndFlags() throws Exception {
        CliOptions options = new CliOptions(tempDir, BuildTool.GRADLE, "runtime", false, "custom", "force", "true", "snyk.json", "org", true, "/opt/bin/snyk");

        Path script = new GradleInitScriptWriter("0.1.0-SNAPSHOT").write(tempDir, options);

        assertThat(script.normalize().startsWith(tempDir.normalize())).isFalse();
        String text = Files.readString(script);
        assertThat(text).contains("classpath 'io.github.lucasgois1.depviz:depviz-gradle-plugin:0.1.0-SNAPSHOT'");
        assertThat(text).contains("project.apply plugin: io.github.lucasgois1.depviz.gradle.DepvizGradlePlugin");
        assertThat(text).contains("scope.set('runtime')");
        assertThat(text).contains("open.set(false)");
        assertThat(text).contains("outputDirectory.set(project.layout.projectDirectory.dir('custom'))");
        assertThat(text).contains("snyk.set('true')");
        assertThat(text).contains("snykJson.set(project.layout.projectDirectory.file('snyk.json'))");
    }

    @Test
    void escapesSingleQuotesAndBackslashesInStringValues() throws Exception {
        CliOptions options = new CliOptions(
            tempDir,
            BuildTool.GRADLE,
            "run'time",
            null,
            "custom\\dir",
            "for'ce",
            "tr\\ue",
            "reports\\snyk's.json",
            null,
            false,
            null
        );

        Path script = new GradleInitScriptWriter("0.1.0-SNAPSHOT").write(tempDir, options);

        String text = Files.readString(script);
        assertThat(text).contains("scope.set('run\\'time')");
        assertThat(text).contains("outputDirectory.set(project.layout.projectDirectory.dir('custom\\\\dir'))");
        assertThat(text).contains("layout.set('for\\'ce')");
        assertThat(text).contains("snyk.set('tr\\\\ue')");
        assertThat(text).contains("snykJson.set(project.layout.projectDirectory.file('reports\\\\snyk\\'s.json'))");
    }
}
