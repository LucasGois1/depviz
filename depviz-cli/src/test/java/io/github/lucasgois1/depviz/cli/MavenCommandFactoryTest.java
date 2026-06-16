package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MavenCommandFactoryTest {
    @Test
    void omitsDefaultParserProperties() {
        CliOptions options = CliOptions.parse(new String[] {"open"}, Path.of("/repo"));

        assertThat(new MavenCommandFactory("0.1.0-SNAPSHOT").command(options, "mvn")).containsExactly(
            "mvn",
            "io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0-SNAPSHOT:open"
        );
    }

    @Test
    void buildsFullCoordinateMavenCommandWithDepvizProperties() {
        CliOptions options = new CliOptions(
            Path.of("/repo"),
            BuildTool.MAVEN,
            "compile",
            false,
            true,
            false,
            "target/custom",
            "force",
            "true",
            "snyk.json",
            "my-org",
            true,
            "/opt/bin/snyk"
        );

        assertThat(new MavenCommandFactory("0.1.0-SNAPSHOT").command(options, "mvn")).containsExactly(
            "mvn",
            "-U",
            "io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0-SNAPSHOT:open",
            "-Ddepviz.scope=compile",
            "-Ddepviz.open=false",
            "-Ddepviz.outputDirectory=target/custom",
            "-Ddepviz.layout=force",
            "-Ddepviz.checkUpdates=false",
            "-Ddepviz.snyk=true",
            "-Ddepviz.snykJson=snyk.json",
            "-Ddepviz.snykOrg=my-org",
            "-Ddepviz.snykAllProjects=true",
            "-Ddepviz.snykCommand=/opt/bin/snyk"
        );
    }
}
