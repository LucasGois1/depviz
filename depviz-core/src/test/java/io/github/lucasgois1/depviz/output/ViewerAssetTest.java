package io.github.lucasgois1.depviz.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ViewerAssetTest {
    @Test
    void coreJarResourcesExposeViewerTemplateAndAssets() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        assertThat(loader.getResource("depviz/dependency-graph.html.tpl")).isNotNull();
        assertThat(loader.getResource("depviz/assets/app.js")).isNotNull();
        assertThat(loader.getResource("depviz/assets/style.css")).isNotNull();
        assertThat(loader.getResource("depviz/assets/LICENSES.txt")).isNotNull();
    }

    @Test
    void viewerAssetsComeFromCoreGeneratedResources() {
        Path generatedAssetsDirectory = Path.of("target/generated-resources/depviz/assets");
        Path sourceAssetsDirectory = Path.of("src/main/resources/depviz/assets");

        assertThat(generatedAssetsDirectory.resolve("app.js")).isRegularFile();
        assertThat(generatedAssetsDirectory.resolve("style.css")).isRegularFile();
        assertThat(generatedAssetsDirectory.resolve("LICENSES.txt")).isRegularFile();

        assertThat(sourceAssetsDirectory.resolve("app.js")).doesNotExist();
        assertThat(sourceAssetsDirectory.resolve("style.css")).doesNotExist();
        assertThat(sourceAssetsDirectory.resolve("LICENSES.txt")).doesNotExist();
    }
}
