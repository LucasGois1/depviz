package dev.gois.tools.depviz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DepvizConfigTest {
    @Test
    void appliesDefaults() {
        DepvizConfig config = DepvizConfig.fromRaw(null, null, null, null, null, null, null, Path.of("target/depviz"));

        assertThat(config.scope()).isEqualTo(DepvizScope.RUNTIME);
        assertThat(config.open()).isTrue();
        assertThat(config.initialLayout()).isEqualTo(DepvizLayout.BREADTHFIRST);
        assertThat(config.nodeMode()).isEqualTo(NodeMode.ARTIFACT);
        assertThat(config.maxInitialLabels()).isEqualTo(500);
        assertThat(config.outputDirectory()).isEqualTo(Path.of("target/depviz"));
    }

    @Test
    void rejectsInvalidScope() {
        assertThatThrownBy(() -> DepvizConfig.fromRaw("production", null, null, null, null, null, null, Path.of("target/depviz")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid depviz.scope 'production'");
    }

    @Test
    void rejectsInvalidLayout() {
        assertThatThrownBy(() -> DepvizConfig.fromRaw(null, null, null, "grid", null, null, null, Path.of("target/depviz")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid depviz.layout 'grid'");
    }

    @Test
    void rejectsOccurrenceNodeModeUntilViewerSupportsIt() {
        assertThatThrownBy(() -> DepvizConfig.fromRaw(null, null, null, null, "occurrence", null, null, Path.of("target/depviz")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depviz.nodeMode=occurrence is not implemented yet");
    }

    @Test
    void rejectsNonPositiveMaxInitialLabels() {
        assertThatThrownBy(() -> DepvizConfig.fromRaw(null, null, null, null, null, "0", null, Path.of("target/depviz")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depviz.maxInitialLabels must be greater than zero");
    }
}
