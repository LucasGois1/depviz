package io.github.lucasgois1.depviz.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class CommandRunner {
    public int run(Path directory, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }
}
