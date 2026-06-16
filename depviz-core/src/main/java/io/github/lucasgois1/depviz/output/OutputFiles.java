package io.github.lucasgois1.depviz.output;

import java.nio.file.Path;

public record OutputFiles(Path outputDirectory, Path htmlFile, Path jsonFile) {
}
