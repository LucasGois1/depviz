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
        command.add("io.github.lucasgois1.depviz:depviz-maven-plugin:" + version + ":open");
        add(command, "depviz.scope", options.scope());
        if (options.open() != null) {
            add(command, "depviz.open", options.open().toString());
        }
        add(command, "depviz.outputDirectory", options.output());
        add(command, "depviz.layout", options.layout());
        add(command, "depviz.snyk", options.snyk());
        add(command, "depviz.snykJson", options.snykJson());
        add(command, "depviz.snykOrg", options.snykOrg());
        if (options.snykAllProjects()) {
            add(command, "depviz.snykAllProjects", "true");
        }
        add(command, "depviz.snykCommand", options.snykCommand());
        return command;
    }

    private static void add(List<String> command, String key, String value) {
        if (value != null && !value.isBlank()) {
            command.add("-D" + key + "=" + value);
        }
    }
}
