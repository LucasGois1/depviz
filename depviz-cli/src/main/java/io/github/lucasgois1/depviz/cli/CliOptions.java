package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record CliOptions(
    Path projectDir,
    BuildTool tool,
    String scope,
    Boolean open,
    String output,
    String layout,
    String snyk,
    String snykJson,
    String snykOrg,
    boolean snykAllProjects,
    String snykCommand
) {
    public CliOptions withDefaultOpen(boolean interactive) {
        if (open != null) {
            return this;
        }
        return new CliOptions(projectDir, tool, scope, interactive, output, layout, snyk, snykJson, snykOrg, snykAllProjects, snykCommand);
    }

    public static CliOptions parse(String[] args, Path currentDirectory) {
        if (args.length == 0 || !"open".equals(args[0])) {
            throw new IllegalArgumentException("Usage: depviz open [options]");
        }
        Path projectDir = currentDirectory;
        BuildTool tool = null;
        String scope = "runtime";
        Boolean open = null;
        String output = null;
        String layout = null;
        String snyk = "auto";
        String snykJson = null;
        String snykOrg = null;
        boolean snykAllProjects = false;
        String snykCommand = null;
        List<String> list = new ArrayList<>(List.of(args));
        for (int index = 1; index < list.size(); index++) {
            String arg = list.get(index);
            switch (arg) {
                case "--project-dir" -> projectDir = resolveProjectDir(currentDirectory, requireValue(list, ++index, arg));
                case "--tool" -> tool = parseTool(requireValue(list, ++index, arg));
                case "--scope" -> scope = requireValue(list, ++index, arg);
                case "--open" -> open = true;
                case "--no-browser" -> open = false;
                case "--output" -> output = requireValue(list, ++index, arg);
                case "--layout" -> layout = requireValue(list, ++index, arg);
                case "--snyk" -> snyk = "true";
                case "--no-snyk" -> snyk = "false";
                case "--snyk-json" -> snykJson = requireValue(list, ++index, arg);
                case "--snyk-org" -> snykOrg = requireValue(list, ++index, arg);
                case "--snyk-all-projects" -> snykAllProjects = true;
                case "--snyk-command" -> snykCommand = requireValue(list, ++index, arg);
                default -> throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        return new CliOptions(projectDir, tool, scope, open, output, layout, snyk, snykJson, snykOrg, snykAllProjects, snykCommand);
    }

    private static String requireValue(List<String> args, int index, String option) {
        if (index >= args.size() || args.get(index).startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value.");
        }
        return args.get(index);
    }

    private static Path resolveProjectDir(Path currentDirectory, String value) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path;
        }
        return currentDirectory.resolve(path).normalize();
    }

    private static BuildTool parseTool(String value) {
        if ("maven".equalsIgnoreCase(value)) {
            return BuildTool.MAVEN;
        }
        if ("gradle".equalsIgnoreCase(value)) {
            return BuildTool.GRADLE;
        }
        throw new IllegalArgumentException("--tool must be maven or gradle.");
    }
}
