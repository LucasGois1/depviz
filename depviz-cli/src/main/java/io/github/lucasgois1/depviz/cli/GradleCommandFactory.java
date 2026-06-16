package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GradleCommandFactory {
    public List<String> command(String executable, Path initScript, boolean refreshDependencies) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        if (refreshDependencies) {
            command.add("--refresh-dependencies");
        }
        command.add("--init-script");
        command.add(initScript.toString());
        command.add(":depvizOpen");
        return List.copyOf(command);
    }
}
