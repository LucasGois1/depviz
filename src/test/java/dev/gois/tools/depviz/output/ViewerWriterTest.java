package dev.gois.tools.depviz.output;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizConfig;
import dev.gois.tools.depviz.graph.ArtifactCoordinate;
import dev.gois.tools.depviz.graph.ExtractedDependencyNode;
import dev.gois.tools.depviz.graph.GraphDocument;
import dev.gois.tools.depviz.graph.GraphDocumentBuilder;
import dev.gois.tools.depviz.graph.ProjectInfo;
import dev.gois.tools.depviz.version.VersionCheckResult;
import dev.gois.tools.depviz.version.VersionInsight;
import dev.gois.tools.depviz.version.VersionSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViewerWriterTest {
    @TempDir
    Path tempDir;

    private static final String MALICIOUS_TEXT = "</ScRiPt><script>alert(1)</script>";

    @Test
    void writesHtmlJsonAndAssetsWithoutCdnReferences() throws Exception {
        DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, tempDir);
        GraphDocument document = new GraphDocumentBuilder().build(
            new ExtractedDependencyNode(new ArtifactCoordinate("com.acme", "app", "jar", "", "1.0.0"), "compile", false, List.of(), List.of()),
            new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", ".", false, List.of()),
            config
        );

        OutputFiles files = new ViewerWriter().write(document, config.outputDirectory());

        assertThat(Files.exists(files.htmlFile())).isTrue();
        assertThat(Files.exists(files.jsonFile())).isTrue();
        String html = Files.readString(files.htmlFile());
        assertThat(html).contains("id=\"graph-root\"");
        assertThat(html).doesNotContain("https://");
        assertThat(html).doesNotContain("http://");
        assertThat(html).containsPattern("href=\"assets/style\\.css\\?v=[a-f0-9]{12}\"");
        assertThat(html).containsPattern("src=\"assets/app\\.js\\?v=[a-f0-9]{12}\"");
        assertThat(Files.exists(files.outputDirectory().resolve("assets/app.js"))).isTrue();
    }

    @Test
    void escapesMaliciousInlineJsonPayload() throws Exception {
        DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, tempDir);
        GraphDocument document = new GraphDocumentBuilder().build(
            new ExtractedDependencyNode(new ArtifactCoordinate("com.acme", MALICIOUS_TEXT, "jar", "", "1.0.0"), "compile", false, List.of(), List.of()),
            new ProjectInfo("com.acme", MALICIOUS_TEXT, "1.0.0", "jar", MALICIOUS_TEXT, ".", false, List.of()),
            config
        );

        OutputFiles files = new ViewerWriter().write(document, config.outputDirectory());

        String html = Files.readString(files.htmlFile());
        String dataBlock = html.substring(
            html.indexOf("<script id=\"depviz-data\""),
            html.indexOf("<script src=\"assets/app.js?v=")
        );
        assertThat(dataBlock).doesNotContain(MALICIOUS_TEXT);
        assertThat(dataBlock).doesNotContain("</ScRiPt>");
        assertThat(dataBlock).doesNotContain("<script>alert(1)");
        assertThat(dataBlock).containsOnlyOnce("</script>");
    }

    @Test
    void serializesVersionMetadataInGraphJson() throws Exception {
        DepvizConfig config = DepvizConfig.fromRaw(null, "false", null, null, null, null, null, tempDir);
        ExtractedDependencyNode root = new ExtractedDependencyNode(
            new ArtifactCoordinate("com.acme", "app", "jar", "", "1.0.0"),
            "compile",
            false,
            List.of(new ExtractedDependencyNode(
                new ArtifactCoordinate("org.example", "lib", "jar", "", "1.0.0"),
                "compile",
                false,
                List.of(),
                List.of()
            )),
            List.of()
        );
        VersionCheckResult versionCheck = new VersionCheckResult(
            Map.of(
                "org.example:lib:jar::1.0.0",
                new VersionInsight("1.0.0", "1.0.1", "patch", "outdated", true, "Patch update available.")
            ),
            new VersionSummary(true, 1, 0, 1, 1, 0, 0, 0, 0),
            List.of()
        );
        GraphDocument document = new GraphDocumentBuilder().build(
            root,
            new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", ".", false, List.of()),
            config,
            versionCheck
        );

        OutputFiles files = new ViewerWriter().write(document, config.outputDirectory());

        String json = Files.readString(files.jsonFile());
        assertThat(json).contains("\"versionInsight\"");
        assertThat(json).contains("\"updateType\" : \"patch\"");
        assertThat(json).contains("\"versionSummary\"");
    }
}
