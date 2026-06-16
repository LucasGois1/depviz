package io.github.lucasgois1.depviz.cli;

final class HelpText {
    private HelpText() {}

    static String root() {
        return """
            Depviz - offline interactive Java dependency graph viewer.

            Usage:
              depviz open [options]
              depviz help [command]
              depviz version
              depviz --version

            Commands:
              open       Generate the dependency graph viewer for a Maven or Gradle project.
              help       Show general help or help for a command.
              version    Print the installed Depviz CLI version.

            Common examples:
              depviz open
              depviz open --no-browser
              depviz open --tool maven --scope compile --no-updates
              depviz open --tool gradle --no-snyk
              depviz open --project-dir ../service-api --output target/depviz

            Run `depviz open --help` for all generation options.
            """;
    }

    static String open() {
        return """
            Usage: depviz open [options]

            Generates an offline dependency graph viewer for the selected project.
            Run this from a directory containing pom.xml, build.gradle, build.gradle.kts,
            settings.gradle, or settings.gradle.kts.

            Project selection:
              --project-dir <path>       Project directory to analyze. Defaults to the current directory.
              --tool <maven|gradle>      Build tool to use when both Maven and Gradle files are present.

            Graph generation:
              --scope <scope>            Dependency scope: compile, runtime, test, provided, system, import, all.
                                         Defaults to runtime.
              --output <path>            Output directory. Maven defaults to target/depviz.
              --layout <layout>          Initial viewer layout: breadthfirst, force, circle, concentric.
              --updates                  Enable dependency version update checks. Maven only; default behavior.
              --no-updates               Disable dependency version update checks. Useful for large projects,
                                         offline work, or slow corporate Maven mirrors.
              --refresh-dependencies     Force dependency metadata refresh. Adds -U for Maven and
                                         --refresh-dependencies for Gradle.

            Browser:
              --open                     Open the generated HTML after generation.
              --no-browser               Generate files without opening the browser.

            Snyk:
              --snyk                     Run Snyk enrichment explicitly.
              --no-snyk                  Disable Snyk enrichment.
              --snyk-json <path>         Read an existing Snyk JSON report.
              --snyk-org <org>           Pass --org to the Snyk CLI command.
              --snyk-all-projects        Pass --all-projects to the Snyk CLI command.
              --snyk-command <command>   Snyk executable or wrapper command. Defaults to snyk.

            Corporate Maven mirrors:
              Depviz CLI delegates Maven projects to the Depviz Maven plugin with the same
              version as the installed CLI. If Maven says depviz-maven-plugin is missing,
              the artifact is not visible through the Maven settings/mirror used by that
              project. Try --refresh-dependencies first. If the mirror still cannot see it,
              publish/sync the artifact in the mirror or use an install package that places
              Depviz artifacts in ~/.m2/repository.

            Examples:
              depviz open --no-browser --no-snyk --no-updates
              depviz open --tool maven --scope compile --refresh-dependencies
              depviz open --project-dir ~/work/service --output target/depviz
            """;
    }
}
