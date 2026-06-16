package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliMavenSmokeTest {
    @TempDir
    Path projectDir;

    @Test
    void runsMavenProjectWithoutMutatingPom() throws Exception {
        Path pomXml = projectDir.resolve("pom.xml");
        Files.writeString(
            pomXml,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>

              <groupId>com.example</groupId>
              <artifactId>sample-maven-project</artifactId>
              <version>1.0.0</version>

              <dependencies>
                <dependency>
                  <groupId>org.slf4j</groupId>
                  <artifactId>slf4j-api</artifactId>
                  <version>2.0.13</version>
                </dependency>
              </dependencies>
            </project>
            """,
            StandardCharsets.UTF_8
        );

        int exitCode = Main.run(
            new String[] {"open", "--tool", "maven", "--no-browser", "--no-snyk", "--scope", "runtime"},
            projectDir,
            false
        );

        assertThat(exitCode).isZero();
        assertThat(projectDir.resolve("target/depviz/dependency-graph.json")).exists();
        assertThat(Files.readString(pomXml, StandardCharsets.UTF_8)).doesNotContain("depviz");
    }
}
