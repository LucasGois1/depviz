package io.github.lucasgois1.depviz.cli;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: depviz open [options]");
            System.exit(2);
        }
        System.err.println("Unsupported command: " + args[0]);
        System.exit(2);
    }
}
