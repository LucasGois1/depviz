package io.github.lucasgois1.depviz.cli;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExecutableSelector {
    private ExecutableSelector() {}

    public static String maven(Path directory) {
        return executable(directory.resolve("mvnw")) ? "./mvnw" : "mvn";
    }

    public static String gradle(Path directory) {
        return executable(directory.resolve("gradlew")) ? "./gradlew" : "gradle";
    }

    private static boolean executable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }
}
