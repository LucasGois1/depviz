package io.github.lucasgois1.depviz.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.util.JsonEscaper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public class ViewerWriter {
    private static final String TEMPLATE_RESOURCE = "depviz/dependency-graph.html.tpl";
    private static final String DATA_PLACEHOLDER = "{{DEPVIZ_DATA}}";
    private static final String APP_ASSET_PLACEHOLDER = "{{APP_ASSET}}";
    private static final String STYLE_ASSET_PLACEHOLDER = "{{STYLE_ASSET}}";
    private static final List<String> ASSETS = List.of(
        "app.js",
        "style.css",
        "LICENSES.txt"
    );

    private final ObjectMapper objectMapper;

    public ViewerWriter() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public OutputFiles write(GraphDocument document, Path outputDirectory) throws IOException {
        Objects.requireNonNull(document, "document is required.");
        Objects.requireNonNull(outputDirectory, "outputDirectory is required.");

        Files.createDirectories(outputDirectory);
        Path assetsDirectory = outputDirectory.resolve("assets");
        Files.createDirectories(assetsDirectory);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        Path jsonFile = outputDirectory.resolve("dependency-graph.json");
        Files.writeString(jsonFile, json, StandardCharsets.UTF_8);

        copyAssets(assetsDirectory);

        String html = template()
            .replace(STYLE_ASSET_PLACEHOLDER, assetReference(assetsDirectory, "style.css"))
            .replace(APP_ASSET_PLACEHOLDER, assetReference(assetsDirectory, "app.js"))
            .replace(DATA_PLACEHOLDER, JsonEscaper.forInlineScript(json));
        Path htmlFile = outputDirectory.resolve("dependency-graph.html");
        Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

        return new OutputFiles(outputDirectory, htmlFile, jsonFile);
    }

    private static String template() throws IOException {
        try (InputStream inputStream = resourceStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Missing viewer template resource: " + TEMPLATE_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void copyAssets(Path assetsDirectory) throws IOException {
        for (String asset : ASSETS) {
            String resource = "depviz/assets/" + asset;
            try (InputStream inputStream = resourceStream(resource)) {
                if (inputStream == null) {
                    throw new IOException("Missing viewer asset resource: " + resource);
                }
                Files.copy(inputStream, assetsDirectory.resolve(asset), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static InputStream resourceStream(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            InputStream inputStream = loader.getResourceAsStream(resource);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return ViewerWriter.class.getClassLoader().getResourceAsStream(resource);
    }

    private static String assetReference(Path assetsDirectory, String asset) throws IOException {
        return "assets/" + asset + "?v=" + assetHash(assetsDirectory.resolve(asset));
    }

    private static String assetHash(Path assetPath) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(assetPath));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is not available for viewer asset hash generation.", exception);
        }
    }
}
