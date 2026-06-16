package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.List;

public final class GradleCommandFactory {
    public List<String> command(String executable, Path initScript) {
        return List.of(executable, "--init-script", initScript.toString(), ":depvizOpen");
    }
}
