package dev.gois.tools.depviz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DepvizConfigTest {
    @Test
    void appliesDefaults() {
        DepvizConfig config = DepvizConfig.fromRaw(null, null, null, null, null, null, null, null);

        assertThat(config.scope()).isEqualTo(DepvizScope.RUNTIME);
        assertThat(config.open()).isTrue();
        assertThat(config.initialLayout()).isEqualTo(DepvizLayout.BREADTHFIRST);
        assertThat(config.nodeMode()).isEqualTo(NodeMode.ARTIFACT);
        assertThat(config.maxInitialLabels()).isEqualTo(500);
        assertThat(config.outputDirectory()).isEqualTo(Path.of("target", "depviz"));
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

    @Test
    void rejectsInvalidOpenFlag() {
        assertThatThrownBy(() -> DepvizConfig.fromRaw(null, "flase", null, null, null, null, null, Path.of("target/depviz")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depviz.open must be true or false.");
    }

    @Test
    void runtimeScopeIncludesCompileAndRuntimeDependencies() {
        assertThat(DepvizScope.RUNTIME.includesDependencyScope(null)).isTrue();
        assertThat(DepvizScope.RUNTIME.includesDependencyScope("")).isTrue();
        assertThat(DepvizScope.RUNTIME.includesDependencyScope("compile")).isTrue();
        assertThat(DepvizScope.RUNTIME.includesDependencyScope("runtime")).isTrue();
        assertThat(DepvizScope.RUNTIME.includesDependencyScope("test")).isFalse();
    }

    @Test
    void allScopeIncludesEveryDependencyScope() {
        assertThat(DepvizScope.ALL.includesDependencyScope(null)).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("compile")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("runtime")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("test")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("provided")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("system")).isTrue();
        assertThat(DepvizScope.ALL.includesDependencyScope("import")).isTrue();
    }

    @Test
    void compileScopeIncludesCompileAndUnscopedDependencies() {
        assertThat(DepvizScope.COMPILE.includesDependencyScope(null)).isTrue();
        assertThat(DepvizScope.COMPILE.includesDependencyScope("")).isTrue();
        assertThat(DepvizScope.COMPILE.includesDependencyScope("compile")).isTrue();
        assertThat(DepvizScope.COMPILE.includesDependencyScope("runtime")).isFalse();
    }

    @Test
    void exactScopesOnlyIncludeMatchingDependencyScope() {
        assertOnlyIncludes(DepvizScope.PROVIDED, "provided");
        assertOnlyIncludes(DepvizScope.SYSTEM, "system");
        assertOnlyIncludes(DepvizScope.IMPORT, "import");
    }

    private static void assertOnlyIncludes(DepvizScope scope, String includedDependencyScope) {
        assertThat(scope.includesDependencyScope(null)).isFalse();
        assertThat(scope.includesDependencyScope("")).isFalse();
        assertThat(scope.includesDependencyScope("compile")).isEqualTo("compile".equals(includedDependencyScope));
        assertThat(scope.includesDependencyScope("runtime")).isEqualTo("runtime".equals(includedDependencyScope));
        assertThat(scope.includesDependencyScope("test")).isEqualTo("test".equals(includedDependencyScope));
        assertThat(scope.includesDependencyScope("provided")).isEqualTo("provided".equals(includedDependencyScope));
        assertThat(scope.includesDependencyScope("system")).isEqualTo("system".equals(includedDependencyScope));
        assertThat(scope.includesDependencyScope("import")).isEqualTo("import".equals(includedDependencyScope));
    }
}
