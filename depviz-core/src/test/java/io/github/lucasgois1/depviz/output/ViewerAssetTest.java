package io.github.lucasgois1.depviz.output;

import static org.assertj.core.api.Assertions.assertThat;

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
}
