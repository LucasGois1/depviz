package io.github.lucasgois1.depviz.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectDetector {
    private ProjectDetector() {}

    public static DetectionResult detect(Path directory) {
        List<BuildTool> tools = new ArrayList<>();
        if (Files.isRegularFile(directory.resolve("pom.xml"))) {
            tools.add(BuildTool.MAVEN);
        }
        if (hasGradleBuild(directory)) {
            tools.add(BuildTool.GRADLE);
        }
        return new DetectionResult(List.copyOf(tools));
    }

    private static boolean hasGradleBuild(Path directory) {
        return Files.isRegularFile(directory.resolve("settings.gradle"))
            || Files.isRegularFile(directory.resolve("settings.gradle.kts"))
            || Files.isRegularFile(directory.resolve("build.gradle"))
            || Files.isRegularFile(directory.resolve("build.gradle.kts"));
    }

    public record DetectionResult(List<BuildTool> tools) {}
}
