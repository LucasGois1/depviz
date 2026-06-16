package io.github.lucasgois1.depviz.cli;

import java.util.ArrayList;
import java.util.List;

public final class MavenCommandFactory {
    private final String version;

    public MavenCommandFactory(String version) {
        this.version = version;
    }

    public List<String> command(CliOptions options, String executable) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        if (options.refreshDependencies()) {
            command.add("-U");
        }
        command.add("io.github.lucasgois1.depviz:depviz-maven-plugin:" + version + ":open");
        addUnlessDefault(command, "depviz.scope", options.scope(), "runtime");
        if (options.open() != null) {
            add(command, "depviz.open", options.open().toString());
        }
        add(command, "depviz.outputDirectory", options.output());
        add(command, "depviz.layout", options.layout());
        if (options.checkUpdates() != null) {
            add(command, "depviz.checkUpdates", options.checkUpdates().toString());
        }
        addUnlessDefault(command, "depviz.snyk", options.snyk(), "auto");
        add(command, "depviz.snykJson", options.snykJson());
        add(command, "depviz.snykOrg", options.snykOrg());
        if (options.snykAllProjects()) {
            add(command, "depviz.snykAllProjects", "true");
        }
        add(command, "depviz.snykCommand", options.snykCommand());
        return List.copyOf(command);
    }

    private static void add(List<String> command, String key, String value) {
        if (value != null && !value.isBlank()) {
            command.add("-D" + key + "=" + value);
        }
    }

    private static void addUnlessDefault(List<String> command, String key, String value, String defaultValue) {
        if (value != null && !value.isBlank() && !defaultValue.equals(value)) {
            command.add("-D" + key + "=" + value);
        }
    }
}
