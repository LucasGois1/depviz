package dev.gois.tools.depviz.security;

import dev.gois.tools.depviz.config.DepvizConfig;
import dev.gois.tools.depviz.config.SnykMode;
import dev.gois.tools.depviz.graph.DiagnosticEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class SnykRunner {
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final TempFileFactory tempFileFactory;

    public SnykRunner() {
        this(Files::createTempFile);
    }

    SnykRunner(TempFileFactory tempFileFactory) {
        this.tempFileFactory = Objects.requireNonNull(tempFileFactory, "tempFileFactory");
    }

    public SecurityCheckResult run(DepvizConfig config, Path workingDirectory) {
        if (config.snykMode() == SnykMode.FALSE) {
            return SecurityCheckResult.disabled();
        }
        if (config.snykJson() != null) {
            return readJson(config.snykJson());
        }

        Path output;
        try {
            output = tempFileFactory.create("depviz-snyk-", ".json");
        } catch (IOException exception) {
            return SecurityCheckResult.unavailable(
                failureType(config),
                "Unable to create Snyk output file: " + exception.getMessage()
            );
        }

        List<String> command = command(config, output);
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return SecurityCheckResult.unavailable(
                    "snyk-scan-failed",
                    "Snyk CLI timed out after " + TIMEOUT.toSeconds() + " seconds."
                );
            }
            if (Files.exists(output) && Files.size(output) > 0) {
                return new SnykReportParser().parse(Files.readString(output, StandardCharsets.UTF_8));
            }
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return SecurityCheckResult.unavailable(
                classifyFailure(stderr),
                stderr.isBlank() ? "Snyk CLI did not produce JSON output." : stderr.trim()
            );
        } catch (IOException exception) {
            return SecurityCheckResult.unavailable(failureType(config), exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SecurityCheckResult.unavailable("snyk-scan-failed", "Snyk CLI was interrupted.");
        } finally {
            deleteIfExists(output);
        }
    }

    public static List<String> command(DepvizConfig config, Path output) {
        List<String> command = new ArrayList<>();
        command.add(config.snykCommand());
        command.add("test");
        command.add("--json-file-output=" + output);
        if (config.snykOrg() != null) {
            command.add("--org=" + config.snykOrg());
        }
        if (config.snykAllProjects()) {
            command.add("--all-projects");
        }
        return command;
    }

    private static SecurityCheckResult readJson(Path path) {
        try {
            return new SnykReportParser().parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return new SecurityCheckResult(
                true,
                false,
                List.of(),
                List.of(new DiagnosticEntry("error", "snyk-json-invalid", exception.getMessage(), null))
            );
        }
    }

    private static String classifyFailure(String stderr) {
        String normalized = stderr == null ? "" : stderr.toLowerCase(Locale.ROOT);
        if (normalized.contains("auth") || normalized.contains("token") || normalized.contains("unauthorized")) {
            return "snyk-auth-failed";
        }
        return "snyk-scan-failed";
    }

    private static String failureType(DepvizConfig config) {
        return config.snykMode() == SnykMode.AUTO ? "snyk-unavailable" : "snyk-scan-failed";
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            // Best-effort cleanup only.
        }
    }

    @FunctionalInterface
    interface TempFileFactory {
        Path create(String prefix, String suffix) throws IOException;
    }
}
