package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.List;

public final class Main {
    static final String VERSION = "0.1.0-SNAPSHOT";

    private Main() {}

    public static void main(String[] args) throws Exception {
        int exitCode = run(args, Path.of("").toAbsolutePath().normalize(), System.console() != null);
        System.exit(exitCode);
    }

    static int run(String[] args, Path currentDirectory, boolean interactive) throws Exception {
        CliOptions options = CliOptions.parse(args, currentDirectory).withDefaultOpen(interactive);
        ProjectDetector.DetectionResult detection = ProjectDetector.detect(options.projectDir());
        BuildTool tool = selectTool(options, detection, interactive);
        CommandRunner runner = new CommandRunner();
        if (tool == BuildTool.MAVEN) {
            List<String> command = new MavenCommandFactory(VERSION).command(options, ExecutableSelector.maven(options.projectDir()));
            return runner.run(options.projectDir(), command);
        }
        Path initScript = new GradleInitScriptWriter(VERSION).write(options.projectDir(), options);
        return runner.run(options.projectDir(), new GradleCommandFactory().command(ExecutableSelector.gradle(options.projectDir()), initScript));
    }

    private static BuildTool selectTool(CliOptions options, ProjectDetector.DetectionResult detection, boolean interactive) {
        if (options.tool() != null) {
            if (!detection.tools().contains(options.tool())) {
                throw new IllegalArgumentException("Requested build tool was not detected in " + options.projectDir());
            }
            return options.tool();
        }
        if (detection.tools().isEmpty()) {
            throw new IllegalArgumentException(
                "No Maven or Gradle project found in "
                    + options.projectDir()
                    + ". Run depviz from a directory containing pom.xml, build.gradle, or settings.gradle."
            );
        }
        if (detection.tools().size() == 1) {
            return detection.tools().get(0);
        }
        if (!interactive) {
            throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
        }
        throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
    }
}
