package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class ConsolePrompterTest {
    @Test
    void selectsMavenFromChoiceOne() {
        ByteArrayInputStream input = new ByteArrayInputStream("1\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        BuildTool tool = new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool();

        assertThat(tool).isEqualTo(BuildTool.MAVEN);
        assertThat(output.toString()).contains("Depviz found both Maven and Gradle");
    }

    @Test
    void selectsGradleFromChoiceTwo() {
        ByteArrayInputStream input = new ByteArrayInputStream("2\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThat(new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool()).isEqualTo(BuildTool.GRADLE);
    }

    @Test
    void cancelsFromChoiceThree() {
        ByteArrayInputStream input = new ByteArrayInputStream("3\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cancelled.");
    }

    @Test
    void rejectsInvalidSelection() {
        ByteArrayInputStream input = new ByteArrayInputStream("x\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid selection: x");
    }
}
