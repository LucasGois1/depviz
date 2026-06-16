package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CliOptionsTest {
    @Test
    void defaultsBrowserOpenFromInteractiveMode() {
        CliOptions parsed = CliOptions.parse(new String[] {"open"}, Path.of("/tmp/project"));
        assertThat(parsed.open()).isNull();
        assertThat(parsed.withDefaultOpen(true).open()).isTrue();
        assertThat(parsed.withDefaultOpen(false).open()).isFalse();
    }

    @Test
    void explicitBrowserFlagOverridesInteractiveDefault() {
        CliOptions parsed = CliOptions.parse(new String[] {"open", "--no-browser"}, Path.of("/tmp/project"));
        assertThat(parsed.withDefaultOpen(true).open()).isFalse();
    }

    @Test
    void parsesProjectDirAndSnykFlags() {
        CliOptions parsed = CliOptions.parse(
            new String[] {
                "open",
                "--project-dir", "/repo/app",
                "--tool", "gradle",
                "--scope", "all",
                "--snyk",
                "--snyk-json", "snyk.json",
                "--snyk-org", "acme",
                "--snyk-all-projects",
                "--snyk-command", "/opt/bin/snyk"
            },
            Path.of("/tmp/project")
        );
        assertThat(parsed.projectDir()).isEqualTo(Path.of("/repo/app"));
        assertThat(parsed.tool()).isEqualTo(BuildTool.GRADLE);
        assertThat(parsed.scope()).isEqualTo("all");
        assertThat(parsed.snyk()).isEqualTo("true");
        assertThat(parsed.snykJson()).isEqualTo("snyk.json");
        assertThat(parsed.snykOrg()).isEqualTo("acme");
        assertThat(parsed.snykAllProjects()).isTrue();
        assertThat(parsed.snykCommand()).isEqualTo("/opt/bin/snyk");
    }

    @Test
    void resolvesRelativeProjectDirAgainstCurrentDirectory() {
        CliOptions parsed = CliOptions.parse(new String[] {"open", "--project-dir", "app"}, Path.of("/tmp/project"));
        assertThat(parsed.projectDir()).isEqualTo(Path.of("/tmp/project/app").normalize());
    }

    @Test
    void rejectsMissingValueForValueTakingOption() {
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"open", "--output"}, Path.of("/tmp/project")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("--output requires a value.");
    }

    @Test
    void rejectsOptionTokenAsValue() {
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"open", "--output", "--no-browser"}, Path.of("/tmp/project")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("--output requires a value.");
    }

    @Test
    void rejectsUnknownOption() {
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"open", "--wat"}, Path.of("/tmp/project")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown option: --wat");
    }

    @Test
    void rejectsInvalidTool() {
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"open", "--tool", "ant"}, Path.of("/tmp/project")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("--tool must be maven or gradle.");
    }
}
