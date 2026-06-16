package io.github.lucasgois1.depviz.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    static final String VERSION = resolveVersion();

    private Main() {}

    public static void main(String[] args) {
        int exitCode = runMain(args, Path.of("").toAbsolutePath().normalize(), System.console() != null, System.err);
        System.exit(exitCode);
    }

    static int runMain(String[] args, Path currentDirectory, boolean interactive, PrintStream err) {
        try {
            return run(args, currentDirectory, interactive);
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage());
            return 2;
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            err.println("Unexpected error: " + message);
            return 1;
        }
    }

    static int run(String[] args, Path currentDirectory, boolean interactive) throws Exception {
        CliOptions options = CliOptions.parse(args, currentDirectory).withDefaultOpen(interactive);
        ProjectDetector.DetectionResult detection = ProjectDetector.detect(options.projectDir());
        BuildTool tool = selectTool(options, detection, interactive, new ConsolePrompter(System.in, System.out));
        CommandRunner runner = new CommandRunner();
        if (tool == BuildTool.MAVEN) {
            List<String> command = new MavenCommandFactory(VERSION).command(options, ExecutableSelector.maven(options.projectDir()));
            return runner.run(options.projectDir(), command);
        }
        Path initScript = new GradleInitScriptWriter(VERSION).write(options.projectDir(), options);
        return runner.run(options.projectDir(), new GradleCommandFactory().command(ExecutableSelector.gradle(options.projectDir()), initScript));
    }

    static String resolveVersion() {
        String configuredVersion = System.getProperty("depviz.version");
        if (configuredVersion != null && !configuredVersion.isBlank()) {
            return configuredVersion;
        }
        String implementationVersion = Main.class.getPackage().getImplementationVersion();
        if (implementationVersion == null || implementationVersion.isBlank()) {
            return "0.1.0-SNAPSHOT";
        }
        return implementationVersion;
    }

    static BuildTool selectTool(
        CliOptions options,
        ProjectDetector.DetectionResult detection,
        boolean interactive,
        ConsolePrompter prompter
    ) {
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
                    + ". Run depviz from a directory containing pom.xml, build.gradle, build.gradle.kts, settings.gradle, or settings.gradle.kts."
            );
        }
        if (detection.tools().size() == 1) {
            return detection.tools().get(0);
        }
        if (!interactive) {
            throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
        }
        return prompter.chooseBuildTool();
    }
}
