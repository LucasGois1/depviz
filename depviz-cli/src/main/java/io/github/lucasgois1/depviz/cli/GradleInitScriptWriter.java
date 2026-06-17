package io.github.lucasgois1.depviz.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class GradleInitScriptWriter {
    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY_DIRECTORY =
        PosixFilePermissions.asFileAttribute(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
    private static final FileAttribute<Set<PosixFilePermission>> OWNER_READ_WRITE =
        PosixFilePermissions.asFileAttribute(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

    private final String version;

    public GradleInitScriptWriter(String version) {
        this.version = version;
    }

    public Path write(Path directory, CliOptions options) throws IOException {
        Path scriptDirectory = createPrivateTempDirectory(directory);
        scriptDirectory.toFile().deleteOnExit();
        Path script = createPrivateTempFile(scriptDirectory);
        script.toFile().deleteOnExit();
        Files.writeString(script, scriptText(options));
        return script;
    }

    private static Path createPrivateTempDirectory(Path directory) throws IOException {
        Path baseDirectory = directory.toAbsolutePath().normalize();
        try {
            return Files.createTempDirectory(baseDirectory, ".depviz-", OWNER_ONLY_DIRECTORY);
        } catch (UnsupportedOperationException ignored) {
            return Files.createTempDirectory(baseDirectory, ".depviz-");
        }
    }

    private static Path createPrivateTempFile(Path directory) throws IOException {
        try {
            return Files.createTempFile(directory, "depviz-", ".gradle", OWNER_READ_WRITE);
        } catch (UnsupportedOperationException ignored) {
            return Files.createTempFile(directory, "depviz-", ".gradle");
        }
    }

    private String scriptText(CliOptions options) {
        StringBuilder script = new StringBuilder();
        script.append("""
            initscript {
              repositories { mavenLocal(); gradlePluginPortal(); mavenCentral() }
              dependencies { classpath 'io.github.lucasgois1.depviz:depviz-gradle-plugin:%s' }
            }
            allprojects { project ->
              if (project == project.rootProject) {
                project.apply plugin: io.github.lucasgois1.depviz.gradle.DepvizGradlePlugin
                project.extensions.configure('depviz') { depviz ->
            """.formatted(escape(version)));
        addString(script, "scope", options.scope());
        if (options.open() != null) {
            script.append("      depviz.open.set(").append(options.open()).append(")\n");
        }
        addDirectory(script, "outputDirectory", options.output());
        addString(script, "layout", options.layout());
        addString(script, "snyk", options.snyk());
        addFile(script, "snykJson", options.snykJson());
        addString(script, "snykOrg", options.snykOrg());
        if (options.snykAllProjects()) {
            script.append("      depviz.snykAllProjects.set(true)\n");
        }
        addString(script, "snykCommand", options.snykCommand());
        script.append("""
                }
              }
            }
            """);
        return script.toString();
    }

    private static void addString(StringBuilder script, String property, String value) {
        if (value != null && !value.isBlank()) {
            script.append("      depviz.").append(property).append(".set('").append(escape(value)).append("')\n");
        }
    }

    private static void addDirectory(StringBuilder script, String property, String value) {
        if (value != null && !value.isBlank()) {
            script.append("      depviz.")
                .append(property)
                .append(".set(project.layout.projectDirectory.dir('")
                .append(escape(value))
                .append("'))\n");
        }
    }

    private static void addFile(StringBuilder script, String property, String value) {
        if (value != null && !value.isBlank()) {
            script.append("      depviz.")
                .append(property)
                .append(".set(project.layout.projectDirectory.file('")
                .append(escape(value))
                .append("'))\n");
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
