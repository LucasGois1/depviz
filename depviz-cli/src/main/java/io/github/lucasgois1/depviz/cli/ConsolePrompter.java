package io.github.lucasgois1.depviz.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public final class ConsolePrompter {
    private final InputStream input;
    private final PrintStream output;

    public ConsolePrompter(InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public BuildTool chooseBuildTool() {
        output.println("Depviz found both Maven and Gradle in this directory.");
        output.println();
        output.println("1. Maven (pom.xml)");
        output.println("2. Gradle (build.gradle[.kts] / settings.gradle[.kts])");
        output.println("3. Cancel");
        output.println();
        output.print("Choose build tool: ");
        Scanner scanner = new Scanner(input);
        if (!scanner.hasNextLine()) {
            throw new IllegalArgumentException("No selection provided.");
        }
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> BuildTool.MAVEN;
            case "2" -> BuildTool.GRADLE;
            case "3" -> throw new IllegalArgumentException("Cancelled.");
            default -> throw new IllegalArgumentException("Invalid selection: " + choice);
        };
    }
}
