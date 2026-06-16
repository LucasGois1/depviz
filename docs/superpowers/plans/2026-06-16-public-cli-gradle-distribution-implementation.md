# Public CLI And Gradle Distribution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn Depviz into a publicly distributable Maven + Gradle + CLI tool under `io.github.lucasgois1.depviz`, without requiring analyzed applications to add Depviz dependencies or edit build files.

**Architecture:** Extract build-tool-independent graph/security/output logic into `depviz-core`, keep `depviz-maven-plugin` as a thin Maven adapter, add a Gradle plugin adapter, and add a CLI wrapper that delegates to Maven or Gradle. The CLI, core, plugins, schema, and viewer share one version and the generated viewer remains offline.

**Tech Stack:** Java 17, Maven 3.9+, Maven Plugin Plugin, Maven Invoker, Gradle Plugin API/TestKit, React/Vite/Sigma viewer, JUnit 5, AssertJ, Vitest.

---

## File Structure Map

The final implementation should converge on this structure:

```text
depviz/
  pom.xml
  depviz-core/
    pom.xml
    src/main/java/io/github/lucasgois1/depviz/{config,graph,output,security,util,version}/...
    src/main/resources/depviz/dependency-graph.html.tpl
    src/test/java/io/github/lucasgois1/depviz/...
    src/test/resources/snyk/...
  depviz-maven-plugin/
    pom.xml
    src/main/java/io/github/lucasgois1/depviz/maven/OpenMojo.java
    src/main/java/io/github/lucasgois1/depviz/maven/graph/...
    src/main/java/io/github/lucasgois1/depviz/maven/version/MavenVersionLookup.java
    src/it/...
    src/test/java/io/github/lucasgois1/depviz/maven/...
  depviz-gradle-plugin/
    build.gradle.kts
    src/main/java/io/github/lucasgois1/depviz/gradle/...
    src/main/resources/META-INF/gradle-plugins/io.github.lucasgois1.depviz.properties
    src/test/java/io/github/lucasgois1/depviz/gradle/...
  depviz-cli/
    pom.xml
    src/main/java/io/github/lucasgois1/depviz/cli/...
    src/test/java/io/github/lucasgois1/depviz/cli/...
  viewer/
    package.json
    src/...
  scripts/
    install.sh
```

Responsibility boundaries:

- `depviz-core` owns model, deduplication, graph document building, Snyk, output writing, viewer assets, and build-tool-neutral version update orchestration.
- `depviz-maven-plugin` owns Maven APIs, Maven reactor extraction, Maven version metadata lookup, and Mojo parameter wiring.
- `depviz-gradle-plugin` owns Gradle APIs, Gradle configuration extraction, Gradle multi-project aggregation, and Gradle task/extension wiring.
- `depviz-cli` owns command-line parsing, build tool detection, wrapper selection, flag translation, init script creation, process execution, and installable launcher behavior.
- `viewer` remains the frontend and should not import Java code.

## Invariants For Every Task

- Use `rtk` before shell commands.
- Do not revert unrelated user changes.
- Keep the two pre-existing untracked plan files untouched unless the user explicitly asks.
- Use TDD for behavior changes: write failing tests first, run them, implement, rerun.
- Commit each task separately with `git -c commit.gpgsign=false -c gpg.format= commit`.
- Run `rtk git status --short --branch` before committing.

---

### Task 1: Public Coordinates And Package Rename

**Files:**
- Modify: `pom.xml`
- Modify: all Java files under `src/main/java/dev/gois/tools/depviz`
- Modify: all Java tests under `src/test/java/dev/gois/tools/depviz`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/*.md`
- Modify: `src/it/*/invoker.properties`
- Modify: `src/it/*/verify.groovy` only if package/coordinate assertions exist

- [ ] **Step 1: Write coordinate regression assertions**

Add descriptor assertions to `src/test/java/dev/gois/tools/depviz/OpenMojoTest.java` before renaming packages:

```java
@Test
void descriptorUsesPublicPluginCoordinate() throws Exception {
    Document descriptor = descriptor();
    Element plugin = (Element) descriptor.getElementsByTagName("plugin").item(0);

    assertThat(text(plugin, "groupId")).isEqualTo("io.github.lucasgois1.depviz");
    assertThat(text(plugin, "artifactId")).isEqualTo("depviz-maven-plugin");
}
```

If `OpenMojoTest` already loads only the `mojos/mojo` node, add this helper overload:

```java
private static String text(Element element, String tagName) {
    return element.getElementsByTagName(tagName).item(0).getTextContent();
}
```

- [ ] **Step 2: Run descriptor test to verify failure**

Run:

```bash
rtk mvn -Dtest=OpenMojoTest test
```

Expected: FAIL because the current descriptor groupId is `dev.gois.tools`.

- [ ] **Step 3: Rename packages mechanically**

Run:

```bash
rtk mkdir -p src/main/java/io/github/lucasgois1/depviz
rtk mkdir -p src/test/java/io/github/lucasgois1/depviz
rtk mv src/main/java/dev/gois/tools/depviz/* src/main/java/io/github/lucasgois1/depviz/
rtk mv src/test/java/dev/gois/tools/depviz/* src/test/java/io/github/lucasgois1/depviz/
rtk perl -pi -e 's/package dev\\.gois\\.tools\\.depviz/package io.github.lucasgois1.depviz/g; s/import dev\\.gois\\.tools\\.depviz/import io.github.lucasgois1.depviz/g' $(rtk find src/main/java src/test/java -type f -name '*.java')
rtk rmdir src/main/java/dev/gois/tools/depviz src/main/java/dev/gois/tools src/main/java/dev/gois src/main/java/dev 2>/dev/null || true
rtk rmdir src/test/java/dev/gois/tools/depviz src/test/java/dev/gois/tools src/test/java/dev/gois src/test/java/dev 2>/dev/null || true
```

- [ ] **Step 4: Update Maven coordinates and docs**

In `pom.xml`, change:

```xml
<groupId>io.github.lucasgois1.depviz</groupId>
<url>https://github.com/lucasgois1/depviz</url>
```

In `pom.xml` invoker goals and all `src/it/*/invoker.properties`, replace:

```text
dev.gois.tools:depviz-maven-plugin
```

with:

```text
io.github.lucasgois1.depviz:depviz-maven-plugin
```

In README and specs, replace user-facing coordinates:

```text
dev.gois.tools:depviz-maven-plugin:0.1.0-SNAPSHOT:open
```

with:

```text
io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0-SNAPSHOT:open
```

- [ ] **Step 5: Run focused tests**

Run:

```bash
rtk mvn -Dtest=OpenMojoTest test
```

Expected: PASS.

- [ ] **Step 6: Run full verification**

Run:

```bash
rtk mvn verify
```

Expected: Java tests, viewer tests, and invoker tests pass.

- [ ] **Step 7: Commit**

Run:

```bash
rtk git add pom.xml README.md docs/superpowers/specs src/main/java src/test/java src/it
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "refactor: rename public coordinates"
```

---

### Task 2: Introduce Maven Parent And `depviz-core`

**Files:**
- Modify: `pom.xml`
- Create: `depviz-core/pom.xml`
- Create: `depviz-maven-plugin/pom.xml`
- Move: core Java packages from `src/main/java/io/github/lucasgois1/depviz` to `depviz-core/src/main/java/io/github/lucasgois1/depviz`
- Move: Maven adapter Java files to `depviz-maven-plugin/src/main/java/io/github/lucasgois1/depviz/maven`
- Move: tests into matching modules
- Move: resources into matching modules
- Move: `src/it` into `depviz-maven-plugin/src/it`

- [ ] **Step 1: Create parent POM**

Replace root `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.github.lucasgois1.depviz</groupId>
  <artifactId>depviz-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <name>Depviz</name>
  <description>Offline interactive Java dependency graph viewer.</description>
  <url>https://github.com/lucasgois1/depviz</url>

  <modules>
    <module>depviz-core</module>
    <module>depviz-maven-plugin</module>
    <module>depviz-cli</module>
  </modules>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>17</maven.compiler.release>
    <maven.version>3.9.0</maven.version>
    <maven.plugin.tools.version>3.15.1</maven.plugin.tools.version>
    <maven.dependency.tree.version>3.3.0</maven.dependency.tree.version>
    <frontend.maven.plugin.version>1.15.4</frontend.maven.plugin.version>
    <frontend.node.version>v24.12.0</frontend.node.version>
    <frontend.npm.version>11.7.0</frontend.npm.version>
    <jackson.version>2.20.1</jackson.version>
    <junit.version>5.14.1</junit.version>
    <assertj.version>3.27.6</assertj.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
      </dependency>
      <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>${jackson.version}</version>
      </dependency>
      <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
      </dependency>
      <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>${assertj.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.14.1</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.5.4</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 2: Create `depviz-core/pom.xml`**

Create:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.github.lucasgois1.depviz</groupId>
    <artifactId>depviz-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>depviz-core</artifactId>
  <name>Depviz Core</name>

  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Step 3: Create `depviz-maven-plugin/pom.xml`**

Create:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.github.lucasgois1.depviz</groupId>
    <artifactId>depviz-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>depviz-maven-plugin</artifactId>
  <packaging>maven-plugin</packaging>
  <name>Depviz Maven Plugin</name>

  <dependencies>
    <dependency>
      <groupId>io.github.lucasgois1.depviz</groupId>
      <artifactId>depviz-core</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-plugin-api</artifactId>
      <version>${maven.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-core</artifactId>
      <version>${maven.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.maven.plugin-tools</groupId>
      <artifactId>maven-plugin-annotations</artifactId>
      <version>${maven.plugin.tools.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.maven.shared</groupId>
      <artifactId>maven-dependency-tree</artifactId>
      <version>${maven.dependency.tree.version}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-plugin-plugin</artifactId>
        <version>${maven.plugin.tools.version}</version>
        <executions>
          <execution>
            <id>descriptor</id>
            <goals><goal>descriptor</goal></goals>
          </execution>
          <execution>
            <id>help-goal</id>
            <goals><goal>helpmojo</goal></goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-invoker-plugin</artifactId>
        <version>3.10.1</version>
        <configuration>
          <projectsDirectory>src/it</projectsDirectory>
          <cloneProjectsTo>${project.build.directory}/it</cloneProjectsTo>
          <localRepositoryPath>${project.build.directory}/it-repo</localRepositoryPath>
          <settingsFile>src/it/settings.xml</settingsFile>
          <postBuildHookScript>verify</postBuildHookScript>
          <goals>
            <goal>io.github.lucasgois1.depviz:depviz-maven-plugin:${project.version}:open</goal>
          </goals>
          <properties>
            <depviz.open>false</depviz.open>
          </properties>
          <filterProperties>
            <depviz.plugin.version>${project.version}</depviz.plugin.version>
          </filterProperties>
        </configuration>
        <executions>
          <execution>
            <id>integration-test</id>
            <goals><goal>install</goal><goal>run</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 4: Move core files**

Move these package directories into `depviz-core/src/main/java/io/github/lucasgois1/depviz/`:

```text
config
graph
output
security
util
version
```

Then move Maven-only classes out of core:

```text
graph/MavenDependencyGraphExtractor.java
graph/ReactorDependencyGraphExtractor.java
version/MavenVersionLookup.java
```

into:

```text
depviz-maven-plugin/src/main/java/io/github/lucasgois1/depviz/maven/graph/
depviz-maven-plugin/src/main/java/io/github/lucasgois1/depviz/maven/version/
```

Move `OpenMojo.java` into:

```text
depviz-maven-plugin/src/main/java/io/github/lucasgois1/depviz/maven/OpenMojo.java
```

Update package declarations:

```java
package io.github.lucasgois1.depviz.maven;
package io.github.lucasgois1.depviz.maven.graph;
package io.github.lucasgois1.depviz.maven.version;
```

- [ ] **Step 5: Move tests and resources**

Move tests for core packages into `depviz-core/src/test/java/io/github/lucasgois1/depviz/...`.

Move tests for `OpenMojo`, Maven extraction, reactor extraction, and `MavenVersionLookup` into `depviz-maven-plugin/src/test/java/io/github/lucasgois1/depviz/maven/...`.

Move `src/test/resources/snyk` into:

```text
depviz-core/src/test/resources/snyk
```

Move `src/main/resources/depviz/dependency-graph.html.tpl` into:

```text
depviz-core/src/main/resources/depviz/dependency-graph.html.tpl
```

Move `src/it` into:

```text
depviz-maven-plugin/src/it
```

- [ ] **Step 6: Fix imports**

All Maven adapter classes should import core types from:

```java
import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.output.ViewerWriter;
```

`OpenMojo` should import Maven adapter classes from:

```java
import io.github.lucasgois1.depviz.maven.graph.MavenDependencyGraphExtractor;
import io.github.lucasgois1.depviz.maven.graph.ReactorDependencyGraphExtractor;
import io.github.lucasgois1.depviz.maven.version.MavenVersionLookup;
```

- [ ] **Step 7: Create minimal CLI module**

Create `depviz-cli/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.github.lucasgois1.depviz</groupId>
    <artifactId>depviz-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>depviz-cli</artifactId>
  <name>Depviz CLI</name>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Add `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/Main.java`:

```java
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
```

- [ ] **Step 8: Run full verification**

Run:

```bash
rtk mvn verify
```

Expected: all modules compile, core tests pass, Maven plugin tests pass, viewer tests run once through Maven plugin build if still configured there, invoker tests pass.

- [ ] **Step 9: Commit**

Run:

```bash
rtk git add pom.xml depviz-core depviz-maven-plugin depviz-cli src viewer README.md docs/superpowers/specs
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "refactor: split core and maven plugin modules"
```

---

### Task 3: Move Viewer Build Into Core Packaging

**Files:**
- Modify: `depviz-core/pom.xml`
- Modify: `viewer/vite.config.ts`
- Modify: `viewer/package.json`
- Modify: `depviz-core/src/main/java/io/github/lucasgois1/depviz/output/ViewerWriter.java`

- [ ] **Step 1: Add core asset packaging assertion**

Create `depviz-core/src/test/java/io/github/lucasgois1/depviz/output/ViewerAssetTest.java`:

```java
package io.github.lucasgois1.depviz.output;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ViewerAssetTest {
    @Test
    void coreJarResourcesExposeViewerTemplateAndAssets() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        assertThat(loader.getResource("depviz/dependency-graph.html.tpl")).isNotNull();
        assertThat(loader.getResource("depviz/assets/app.js")).isNotNull();
        assertThat(loader.getResource("depviz/assets/style.css")).isNotNull();
        assertThat(loader.getResource("depviz/assets/LICENSES.txt")).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -Dtest=ViewerAssetTest test
```

Expected: FAIL because assets are not yet copied into core resources.

- [ ] **Step 3: Configure viewer build output for core**

In `viewer/vite.config.ts`, set build output to:

```ts
build: {
  outDir: "../depviz-core/target/generated-resources/depviz/assets",
  emptyOutDir: true,
  rollupOptions: {
    output: {
      entryFileNames: "app.js",
      assetFileNames: "style.css"
    }
  }
}
```

If the existing config already sets `outDir`, update only the path to `../depviz-core/target/generated-resources/depviz/assets`.

- [ ] **Step 4: Move frontend Maven plugin to core**

In `depviz-core/pom.xml`, add:

```xml
<build>
  <resources>
    <resource>
      <directory>src/main/resources</directory>
    </resource>
    <resource>
      <directory>target/generated-resources</directory>
    </resource>
  </resources>
  <plugins>
    <plugin>
      <groupId>com.github.eirslett</groupId>
      <artifactId>frontend-maven-plugin</artifactId>
      <version>${frontend.maven.plugin.version}</version>
      <configuration>
        <workingDirectory>${project.basedir}/../viewer</workingDirectory>
        <installDirectory>${project.build.directory}/frontend</installDirectory>
      </configuration>
      <executions>
        <execution>
          <id>install-node-and-npm</id>
          <phase>generate-resources</phase>
          <goals><goal>install-node-and-npm</goal></goals>
          <configuration>
            <nodeVersion>${frontend.node.version}</nodeVersion>
            <npmVersion>${frontend.npm.version}</npmVersion>
          </configuration>
        </execution>
        <execution>
          <id>npm-ci</id>
          <phase>generate-resources</phase>
          <goals><goal>npm</goal></goals>
          <configuration><arguments>ci</arguments></configuration>
        </execution>
        <execution>
          <id>npm-build</id>
          <phase>generate-resources</phase>
          <goals><goal>npm</goal></goals>
          <configuration><arguments>run build</arguments></configuration>
        </execution>
        <execution>
          <id>npm-test</id>
          <phase>test</phase>
          <goals><goal>npm</goal></goals>
          <configuration><arguments>test</arguments></configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Remove frontend plugin executions from `depviz-maven-plugin/pom.xml`.

- [ ] **Step 5: Run core test**

Run:

```bash
rtk mvn -pl depviz-core -Dtest=ViewerAssetTest test
```

Expected: PASS.

- [ ] **Step 6: Run Maven plugin invoker**

Run:

```bash
rtk mvn -pl depviz-maven-plugin -am verify
```

Expected: Maven plugin can read assets from core dependency and invoker tests pass.

- [ ] **Step 7: Commit**

Run:

```bash
rtk git add depviz-core depviz-maven-plugin viewer
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "refactor: package viewer assets in core"
```

---

### Task 4: Add Build-Tool-Neutral Extraction Contract

**Files:**
- Create: `depviz-core/src/main/java/io/github/lucasgois1/depviz/graph/DependencyGraphInput.java`
- Create: `depviz-core/src/main/java/io/github/lucasgois1/depviz/graph/DependencyNodeInput.java`
- Modify: `depviz-core/src/main/java/io/github/lucasgois1/depviz/graph/GraphDocumentBuilder.java`
- Modify: Maven adapter extraction tests

- [ ] **Step 1: Write neutral builder test**

Create `depviz-core/src/test/java/io/github/lucasgois1/depviz/graph/DependencyGraphInputBuilderTest.java`:

```java
package io.github.lucasgois1.depviz.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyGraphInputBuilderTest {
    @Test
    void buildsDocumentFromBuildToolNeutralInput() {
        DependencyNodeInput root = new DependencyNodeInput(
            new ArtifactCoordinate("com.acme", "app", "jar", "", "1.0.0"),
            "root",
            false,
            false,
            List.of(new DependencyNodeInput(
                new ArtifactCoordinate("org.slf4j", "slf4j-api", "jar", "", "2.0.13"),
                "runtime",
                false,
                false,
                List.of(),
                List.of()
            )),
            List.of()
        );
        DependencyGraphInput input = new DependencyGraphInput(
            root,
            new ProjectInfo("com.acme", "app", "1.0.0", "jar", "app", "/tmp/app", false, List.of())
        );

        GraphDocument document = new GraphDocumentBuilder().build(
            input.root(),
            input.project(),
            DepvizConfig.fromRaw(null, "false", null, null, null, null, null, null, Path.of("target/depviz"), "false", null, null, null, null),
            VersionCheckResult.disabled()
        );

        assertThat(document.nodes()).extracting(GraphNode::artifactId).contains("app", "slf4j-api");
        assertThat(document.edges()).singleElement().satisfies(edge -> {
            assertThat(edge.source()).isEqualTo("com.acme:app:jar::1.0.0");
            assertThat(edge.target()).isEqualTo("org.slf4j:slf4j-api:jar::2.0.13");
        });
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -Dtest=DependencyGraphInputBuilderTest test
```

Expected: FAIL because `DependencyNodeInput` and `DependencyGraphInput` do not exist.

- [ ] **Step 3: Add neutral records**

Create `DependencyNodeInput.java`:

```java
package io.github.lucasgois1.depviz.graph;

import java.util.List;
import java.util.Objects;

public record DependencyNodeInput(
    ArtifactCoordinate coordinate,
    String scope,
    boolean optional,
    boolean moduleRoot,
    List<DependencyNodeInput> children,
    List<DiagnosticEntry> diagnostics
) {
    public DependencyNodeInput {
        coordinate = Objects.requireNonNull(coordinate, "coordinate is required.");
        scope = scope == null || scope.isBlank() ? "runtime" : scope.trim();
        children = children == null ? List.of() : List.copyOf(children);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
```

Create `DependencyGraphInput.java`:

```java
package io.github.lucasgois1.depviz.graph;

import java.util.Objects;

public record DependencyGraphInput(
    DependencyNodeInput root,
    ProjectInfo project
) {
    public DependencyGraphInput {
        root = Objects.requireNonNull(root, "root is required.");
        project = Objects.requireNonNull(project, "project is required.");
    }
}
```

- [ ] **Step 4: Keep existing `ExtractedDependencyNode` as compatibility alias**

If `ExtractedDependencyNode` already exists, change it to:

```java
package io.github.lucasgois1.depviz.graph;

import java.util.List;

public record ExtractedDependencyNode(
    ArtifactCoordinate coordinate,
    String scope,
    boolean optional,
    List<ExtractedDependencyNode> children,
    List<DiagnosticEntry> diagnostics
) {
    public DependencyNodeInput toInput(boolean moduleRoot) {
        return new DependencyNodeInput(
            coordinate,
            scope,
            optional,
            moduleRoot,
            children.stream().map(child -> child.toInput(false)).toList(),
            diagnostics
        );
    }
}
```

Then update Maven adapter code to call `root.toInput(false)` before invoking the core builder, or update `GraphDocumentBuilder` to overload `build(ExtractedDependencyNode, ...)` and delegate.

- [ ] **Step 5: Run focused tests**

Run:

```bash
rtk mvn -pl depviz-core -Dtest=DependencyGraphInputBuilderTest,GraphDocumentBuilderTest test
```

Expected: PASS.

- [ ] **Step 6: Run Maven plugin tests**

Run:

```bash
rtk mvn -pl depviz-maven-plugin -am test
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
rtk git add depviz-core depviz-maven-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(core): add neutral dependency graph input"
```

---

### Task 5: Add Gradle Plugin Module Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `depviz-gradle-plugin/build.gradle.kts`
- Create: `depviz-gradle-plugin/src/main/java/io/github/lucasgois1/depviz/gradle/DepvizGradlePlugin.java`
- Create: `depviz-gradle-plugin/src/main/java/io/github/lucasgois1/depviz/gradle/DepvizExtension.java`
- Create: `depviz-gradle-plugin/src/main/java/io/github/lucasgois1/depviz/gradle/DepvizOpenTask.java`
- Create: `depviz-gradle-plugin/src/test/java/io/github/lucasgois1/depviz/gradle/DepvizGradlePluginTest.java`

- [ ] **Step 1: Add Gradle settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "depviz"
include("depviz-gradle-plugin")
```

- [ ] **Step 2: Add Gradle plugin build file**

Create `depviz-gradle-plugin/build.gradle.kts`:

```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.lucasgois1.depviz"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.lucasgois1.depviz:depviz-core:${project.version}")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.assertj:assertj-core:3.27.6")
}

gradlePlugin {
    plugins {
        create("depviz") {
            id = "io.github.lucasgois1.depviz"
            implementationClass = "io.github.lucasgois1.depviz.gradle.DepvizGradlePlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Write plugin registration test**

Create `DepvizGradlePluginTest.java`:

```java
package io.github.lucasgois1.depviz.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DepvizGradlePluginTest {
    @TempDir
    Path projectDir;

    @Test
    void registersDepvizOpenTask() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"sample\"\\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), "plugins { id(\"io.github.lucasgois1.depviz\") }\\n");

        var result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .build();

        assertThat(result.output()).contains("depvizOpen");
        assertThat(result.task(":tasks").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
```

- [ ] **Step 4: Run Gradle plugin test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: core installs to `mavenLocal`, then Gradle test fails because plugin classes do not exist.

- [ ] **Step 5: Add plugin classes**

Create `DepvizExtension.java`:

```java
package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.RegularFileProperty;

public abstract class DepvizExtension {
    public DepvizExtension(ObjectFactory objects) {
        getScope().convention("runtime");
        getOpen().convention(true);
        getSnyk().convention("auto");
        getLayout().convention("breadthfirst");
    }

    public abstract Property<String> getScope();
    public abstract Property<Boolean> getOpen();
    public abstract Property<String> getSnyk();
    public abstract Property<String> getLayout();
    public abstract RegularFileProperty getSnykJson();
}
```

Create `DepvizOpenTask.java`:

```java
package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class DepvizOpenTask extends DefaultTask {
    @TaskAction
    public void open() {
        getLogger().lifecycle("Depviz Gradle plugin is registered.");
    }
}
```

Create `DepvizGradlePlugin.java`:

```java
package io.github.lucasgois1.depviz.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class DepvizGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("depviz", DepvizExtension.class, project.getObjects());
        project.getTasks().register("depvizOpen", DepvizOpenTask.class, task -> {
            task.setGroup("Depviz");
            task.setDescription("Generates and opens the Depviz dependency graph viewer.");
        });
    }
}
```

- [ ] **Step 6: Run Gradle plugin tests**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: PASS.

- [ ] **Step 7: Generate Gradle wrapper if missing**

If no `gradlew` exists at the repo root, generate one with:

```bash
rtk gradle wrapper --gradle-version 8.14.3
```

- [ ] **Step 8: Run module verification**

Run:

```bash
rtk mvn verify
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: Maven modules pass, then Gradle TestKit tests pass using `depviz-core` from `mavenLocal`.

- [ ] **Step 9: Commit**

Run:

```bash
rtk git add settings.gradle.kts gradlew gradlew.bat gradle depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(gradle): add plugin skeleton"
```

---

### Task 6: Implement Gradle Single-Project Dependency Graph

**Files:**
- Create: `depviz-gradle-plugin/src/main/java/io/github/lucasgois1/depviz/gradle/GradleDependencyGraphExtractor.java`
- Modify: `depviz-gradle-plugin/src/main/java/io/github/lucasgois1/depviz/gradle/DepvizOpenTask.java`
- Add TestKit fixture tests in `DepvizGradlePluginTest.java`

- [ ] **Step 1: Write TestKit generation test**

Add to `DepvizGradlePluginTest`:

```java
@Test
void generatesSingleProjectRuntimeGraph() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"gradle-sample\"\\n");
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins {
            java
            id("io.github.lucasgois1.depviz")
        }

        repositories { mavenCentral() }

        dependencies {
            runtimeOnly("org.slf4j:slf4j-api:2.0.13")
        }
        """);

    var result = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments("depvizOpen", "--stacktrace")
        .build();

    Path json = projectDir.resolve("build/depviz/dependency-graph.json");
    assertThat(result.task(":depvizOpen").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(json).exists();
    assertThat(Files.readString(json)).contains("\"artifactId\" : \"gradle-sample\"");
    assertThat(Files.readString(json)).contains("\"artifactId\" : \"slf4j-api\"");
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*generatesSingleProjectRuntimeGraph'
```

Expected: core installs to `mavenLocal`, then the focused Gradle test fails because `depvizOpen` does not generate output.

- [ ] **Step 3: Implement Gradle dependency extractor**

Create `GradleDependencyGraphExtractor.java`:

```java
package io.github.lucasgois1.depviz.gradle;

import io.github.lucasgois1.depviz.graph.ArtifactCoordinate;
import io.github.lucasgois1.depviz.graph.DependencyNodeInput;
import io.github.lucasgois1.depviz.graph.DiagnosticEntry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;

final class GradleDependencyGraphExtractor {
    DependencyNodeInput extract(Project project, String scope) {
        Configuration configuration = configuration(project, scope);
        Set<DependencyNodeInput> children = new LinkedHashSet<>();
        for (ResolvedDependency dependency : configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
            children.add(toNode(dependency, scope));
        }
        return new DependencyNodeInput(
            new ArtifactCoordinate(
                stringOrDefault(project.getGroup(), "unknown"),
                project.getName(),
                "gradle-project",
                "",
                stringOrDefault(project.getVersion(), "unspecified")
            ),
            "root",
            false,
            false,
            List.copyOf(children),
            List.of()
        );
    }

    private static DependencyNodeInput toNode(ResolvedDependency dependency, String scope) {
        return new DependencyNodeInput(
            new ArtifactCoordinate(
                dependency.getModuleGroup(),
                dependency.getModuleName(),
                "jar",
                "",
                dependency.getModuleVersion()
            ),
            scope,
            false,
            false,
            dependency.getChildren().stream().map(child -> toNode(child, scope)).toList(),
            List.of()
        );
    }

    private static Configuration configuration(Project project, String scope) {
        String name = switch (scope) {
            case "compile" -> "compileClasspath";
            case "test" -> "testRuntimeClasspath";
            case "runtime", "all" -> "runtimeClasspath";
            default -> throw new IllegalArgumentException("Unsupported Gradle depviz scope: " + scope);
        };
        return project.getConfigurations().getByName(name);
    }

    private static String stringOrDefault(Object value, String fallback) {
        String string = value == null ? "" : value.toString();
        return string.isBlank() ? fallback : string.trim();
    }
}
```

- [ ] **Step 4: Implement task output**

In `DepvizOpenTask`, inject project and write output:

```java
@TaskAction
public void open() {
    Project project = getProject();
    DepvizExtension extension = project.getExtensions().getByType(DepvizExtension.class);
    DependencyNodeInput root = new GradleDependencyGraphExtractor().extract(project, extension.getScope().get());
    DepvizConfig config = DepvizConfig.fromRaw(
        extension.getScope().get(),
        Boolean.toString(extension.getOpen().get()),
        null,
        extension.getLayout().get(),
        null,
        null,
        null,
        "false",
        project.getLayout().getBuildDirectory().dir("depviz").get().getAsFile().toPath(),
        extension.getSnyk().get(),
        extension.getSnykJson().isPresent() ? extension.getSnykJson().get().getAsFile().toPath().toString() : null,
        null,
        null,
        null
    );
    ProjectInfo projectInfo = new ProjectInfo(
        root.coordinate().groupId(),
        root.coordinate().artifactId(),
        root.coordinate().version(),
        "gradle",
        project.getName(),
        project.getProjectDir().toPath().toAbsolutePath().normalize().toString(),
        false,
        List.of()
    );
    GraphDocument document = new GraphDocumentBuilder().build(root, projectInfo, config, VersionCheckResult.disabled());
    try {
        OutputFiles output = new ViewerWriter().write(document, config.outputDirectory());
        getLogger().lifecycle("Generated depviz viewer: {}", output.htmlFile().toAbsolutePath().normalize());
    } catch (IOException exception) {
        throw new GradleException("Failed to write depviz viewer: " + exception.getMessage(), exception);
    }
}
```

Add required imports:

```java
import io.github.lucasgois1.depviz.config.DepvizConfig;
import io.github.lucasgois1.depviz.graph.DependencyNodeInput;
import io.github.lucasgois1.depviz.graph.GraphDocument;
import io.github.lucasgois1.depviz.graph.GraphDocumentBuilder;
import io.github.lucasgois1.depviz.graph.ProjectInfo;
import io.github.lucasgois1.depviz.output.OutputFiles;
import io.github.lucasgois1.depviz.output.ViewerWriter;
import io.github.lucasgois1.depviz.version.VersionCheckResult;
import java.io.IOException;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
```

- [ ] **Step 5: Run focused Gradle test**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*generatesSingleProjectRuntimeGraph'
```

Expected: PASS.

- [ ] **Step 6: Run full Gradle plugin tests**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
rtk git add depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(gradle): generate single project graph"
```

---

### Task 7: Implement Gradle Multi-Project Aggregation

**Files:**
- Modify: `GradleDependencyGraphExtractor.java`
- Modify: `DepvizOpenTask.java`
- Modify: `DepvizGradlePluginTest.java`

- [ ] **Step 1: Write multi-project TestKit test**

Add:

```java
@Test
void aggregatesMultiProjectGraphWithSharedDependency() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), """
        rootProject.name = "gradle-platform"
        include("api", "worker")
        """);
    Files.createDirectories(projectDir.resolve("api"));
    Files.createDirectories(projectDir.resolve("worker"));
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins { id("io.github.lucasgois1.depviz") }
        allprojects {
            group = "com.acme"
            version = "1.0.0"
            repositories { mavenCentral() }
        }
        subprojects {
            apply(plugin = "java")
            dependencies {
                "runtimeOnly"("org.slf4j:slf4j-api:2.0.13")
            }
        }
        """);
    Files.writeString(projectDir.resolve("api/build.gradle.kts"), "");
    Files.writeString(projectDir.resolve("worker/build.gradle.kts"), "");

    var result = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments("depvizOpen", "--stacktrace")
        .build();

    Path json = projectDir.resolve("build/depviz/dependency-graph.json");
    String text = Files.readString(json);

    assertThat(result.task(":depvizOpen").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(text).contains("\"type\" : \"reactor\"");
    assertThat(text).contains("\"artifactId\" : \"api\"");
    assertThat(text).contains("\"artifactId\" : \"worker\"");
    assertThat(countOccurrences(text, "\"artifactId\" : \"slf4j-api\"")).isEqualTo(1);
}

private static int countOccurrences(String text, String needle) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(needle, index)) >= 0) {
        count++;
        index += needle.length();
    }
    return count;
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*aggregatesMultiProjectGraphWithSharedDependency'
```

Expected: core installs to `mavenLocal`, then the focused Gradle test fails because current task extracts only the root project.

- [ ] **Step 3: Implement aggregate extraction**

Add method to `GradleDependencyGraphExtractor`:

```java
DependencyNodeInput extractAggregate(Project rootProject, String scope) {
    List<DependencyNodeInput> projectRoots = rootProject.getAllprojects().stream()
        .filter(project -> project != rootProject)
        .filter(project -> project.getPlugins().hasPlugin("java"))
        .map(project -> withModuleScope(extract(project, scope)))
        .toList();
    if (projectRoots.isEmpty()) {
        return extract(rootProject, scope);
    }
    return new DependencyNodeInput(
        new ArtifactCoordinate(
            stringOrDefault(rootProject.getGroup(), "unknown"),
            rootProject.getName() + "-reactor",
            "reactor",
            "",
            stringOrDefault(rootProject.getVersion(), "unspecified")
        ),
        "root",
        false,
        false,
        projectRoots,
        List.of()
    );
}

private static DependencyNodeInput withModuleScope(DependencyNodeInput node) {
    return new DependencyNodeInput(
        node.coordinate(),
        "module",
        false,
        true,
        node.children(),
        node.diagnostics()
    );
}
```

Update `DepvizOpenTask` to call:

```java
DependencyNodeInput root = new GradleDependencyGraphExtractor().extractAggregate(project.getRootProject(), extension.getScope().get());
```

Set `ProjectInfo.multiModule` to `rootProject.getSubprojects().isEmpty() == false` and modules to subproject paths.

- [ ] **Step 4: Run multi-project test**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*aggregatesMultiProjectGraphWithSharedDependency'
```

Expected: PASS.

- [ ] **Step 5: Run all Gradle tests**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(gradle): aggregate multi project graphs"
```

---

### Task 8: Add Gradle Scope And Output Configuration

**Files:**
- Modify: `DepvizExtension.java`
- Modify: `DepvizOpenTask.java`
- Modify: `GradleDependencyGraphExtractor.java`
- Modify: `DepvizGradlePluginTest.java`

- [ ] **Step 1: Write scope mapping tests**

Add tests:

```java
@Test
void compileScopeUsesCompileClasspath() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"scope-sample\"\\n");
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins {
            java
            id("io.github.lucasgois1.depviz")
        }
        repositories { mavenCentral() }
        dependencies {
            implementation("org.slf4j:slf4j-api:2.0.13")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-api:5.14.1")
        }
        depviz { scope.set("compile") }
        """);

    GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments("depvizOpen").build();

    String text = Files.readString(projectDir.resolve("build/depviz/dependency-graph.json"));
    assertThat(text).contains("\"artifactId\" : \"slf4j-api\"");
    assertThat(text).doesNotContain("\"artifactId\" : \"junit-jupiter-api\"");
}

@Test
void customOutputDirectoryIsRespected() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"output-sample\"\\n");
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins {
            java
            id("io.github.lucasgois1.depviz")
        }
        repositories { mavenCentral() }
        depviz { outputDirectory.set(layout.projectDirectory.dir("custom-depviz")) }
        """);

    GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments("depvizOpen").build();

    assertThat(projectDir.resolve("custom-depviz/dependency-graph.json")).exists();
}

@Test
void allScopeIncludesCompileRuntimeAndTestConfigurations() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"all-scope-sample\"\\n");
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins {
            java
            id("io.github.lucasgois1.depviz")
        }
        repositories { mavenCentral() }
        dependencies {
            implementation("org.slf4j:slf4j-api:2.0.13")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-api:5.14.1")
        }
        depviz { scope.set("all") }
        """);

    GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments("depvizOpen").build();

    String text = Files.readString(projectDir.resolve("build/depviz/dependency-graph.json"));
    assertThat(text).contains("\"artifactId\" : \"slf4j-api\"");
    assertThat(text).contains("\"artifactId\" : \"junit-jupiter-api\"");
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*compileScopeUsesCompileClasspath' --tests '*customOutputDirectoryIsRespected' --tests '*allScopeIncludesCompileRuntimeAndTestConfigurations'
```

Expected: custom output and `all` scope tests fail until extension and extraction support them.

- [ ] **Step 3: Add output directory extension property and all-scope extraction**

In `DepvizExtension`:

```java
public abstract DirectoryProperty getOutputDirectory();
```

Constructor:

```java
getOutputDirectory().convention(objects.directoryProperty().fileValue(new File("build/depviz")));
```

Use Gradle layout in plugin apply instead:

```java
DepvizExtension extension = project.getExtensions().create("depviz", DepvizExtension.class, project.getObjects());
extension.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("depviz"));
```

In `GradleDependencyGraphExtractor`, replace the single-configuration extraction loop with:

```java
Set<DependencyNodeInput> children = new LinkedHashSet<>();
for (Configuration configuration : configurations(project, scope)) {
    String nodeScope = scopeFor(scope, configuration);
    for (ResolvedDependency dependency : configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
        children.add(toNode(dependency, nodeScope));
    }
}
```

Replace the previous `configuration(Project project, String scope)` method with:

```java
private static List<Configuration> configurations(Project project, String scope) {
    return switch (scope) {
        case "runtime" -> List.of(configurationByName(project, "runtimeClasspath"));
        case "compile" -> List.of(configurationByName(project, "compileClasspath"));
        case "test" -> List.of(configurationByName(project, "testRuntimeClasspath"));
        case "all" -> List.of(
            configurationByName(project, "compileClasspath"),
            configurationByName(project, "runtimeClasspath"),
            configurationByName(project, "testRuntimeClasspath")
        );
        default -> throw new IllegalArgumentException("Unsupported Gradle depviz scope: " + scope);
    };
}

private static Configuration configurationByName(Project project, String name) {
    return project.getConfigurations().getByName(name);
}

private static String scopeFor(String requestedScope, Configuration configuration) {
    return "all".equals(requestedScope) ? configuration.getName() : requestedScope;
}
```

- [ ] **Step 4: Use configured output in task**

In `DepvizOpenTask`, replace build dir output with:

```java
extension.getOutputDirectory().get().getAsFile().toPath()
```

- [ ] **Step 5: Run scope and output tests**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*compileScopeUsesCompileClasspath' --tests '*customOutputDirectoryIsRespected' --tests '*allScopeIncludesCompileRuntimeAndTestConfigurations'
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(gradle): support scope and output options"
```

---

### Task 9: Add Gradle Snyk JSON And Diagnostics Wiring

**Files:**
- Modify: `DepvizExtension.java`
- Modify: `DepvizOpenTask.java`
- Add tests in `DepvizGradlePluginTest.java`

- [ ] **Step 1: Write Snyk JSON TestKit test**

Add:

```java
@Test
void enrichesGradleGraphFromSnykJson() throws Exception {
    Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"snyk-gradle\"\\n");
    Files.writeString(projectDir.resolve("snyk-report.json"), """
        {
          "vulnerabilities": [
            {
              "id": "SNYK-JAVA-ORGSLF4J-TEST-1",
              "severity": "high",
              "packageName": "org.slf4j:slf4j-api",
              "version": "2.0.13",
              "fixedIn": ["2.0.17"]
            }
          ]
        }
        """);
    Files.writeString(projectDir.resolve("build.gradle.kts"), """
        plugins {
            java
            id("io.github.lucasgois1.depviz")
        }
        repositories { mavenCentral() }
        dependencies { runtimeOnly("org.slf4j:slf4j-api:2.0.13") }
        depviz {
            snyk.set("auto")
            snykJson.set(layout.projectDirectory.file("snyk-report.json"))
        }
        """);

    GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments("depvizOpen").build();

    String text = Files.readString(projectDir.resolve("build/depviz/dependency-graph.json"));
    assertThat(text).contains("\"securitySummary\"");
    assertThat(text).contains("\"high\" : 1");
    assertThat(text).contains("SNYK-JAVA-ORGSLF4J-TEST-1");
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*enrichesGradleGraphFromSnykJson'
```

Expected: core installs to `mavenLocal`, then the focused Gradle test fails because Gradle task does not run security enrichment.

- [ ] **Step 3: Wire Snyk runner and enricher**

In `DepvizOpenTask`, after building initial document:

```java
SecurityCheckResult securityCheck = new SnykRunner().run(config, project.getProjectDir().toPath());
GraphDocument document = new SecurityGraphEnricher().enrich(initialDocument, securityCheck);
```

Add imports:

```java
import io.github.lucasgois1.depviz.security.SecurityCheckResult;
import io.github.lucasgois1.depviz.security.SecurityGraphEnricher;
import io.github.lucasgois1.depviz.security.SnykRunner;
```

- [ ] **Step 4: Run Snyk JSON test**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test --tests '*enrichesGradleGraphFromSnykJson'
```

Expected: PASS.

- [ ] **Step 5: Run full Gradle tests**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(gradle): support snyk json enrichment"
```

---

### Task 10: Implement CLI Argument Model And Build Tool Detection

**Files:**
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/CliOptions.java`
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/BuildTool.java`
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/ProjectDetector.java`
- Create: `depviz-cli/src/test/java/io/github/lucasgois1/depviz/cli/ProjectDetectorTest.java`
- Create: `depviz-cli/src/test/java/io/github/lucasgois1/depviz/cli/CliOptionsTest.java`
- Modify: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/Main.java`

- [ ] **Step 1: Write detector and option tests**

Create `ProjectDetectorTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectDetectorTest {
    @TempDir
    Path dir;

    @Test
    void detectsMavenOnlyInCurrentDirectory() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");

        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.MAVEN);
    }

    @Test
    void detectsGradleFromSettingsOrBuildFile() throws Exception {
        Files.writeString(dir.resolve("settings.gradle.kts"), "rootProject.name = \"demo\"");

        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.GRADLE);
    }

    @Test
    void reportsBothToolsWhenBothArePresent() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        Files.writeString(dir.resolve("build.gradle.kts"), "plugins { java }");

        assertThat(ProjectDetector.detect(dir).tools()).containsExactly(BuildTool.MAVEN, BuildTool.GRADLE);
    }

    @Test
    void doesNotWalkParentDirectories() throws Exception {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        Path child = Files.createDirectory(dir.resolve("child"));

        assertThat(ProjectDetector.detect(child).tools()).isEmpty();
    }
}
```

Create `CliOptionsTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CliOptionsTest {
    @Test
    void defaultsBrowserOpenFromInteractiveMode() {
        CliOptions parsed = CliOptions.parse(new String[] {"open"}, Path.of("/tmp/project"));

        assertThat(parsed.open()).isNull();
        assertThat(parsed.withDefaultOpen(true).open()).isTrue();
        assertThat(parsed.withDefaultOpen(false).open()).isFalse();
    }

    @Test
    void explicitBrowserFlagOverridesInteractiveDefault() {
        CliOptions parsed = CliOptions.parse(new String[] {"open", "--no-browser"}, Path.of("/tmp/project"));

        assertThat(parsed.withDefaultOpen(true).open()).isFalse();
    }

    @Test
    void parsesProjectDirAndSnykFlags() {
        CliOptions parsed = CliOptions.parse(
            new String[] {
                "open",
                "--project-dir", "/repo/app",
                "--tool", "gradle",
                "--scope", "all",
                "--snyk",
                "--snyk-json", "snyk.json",
                "--snyk-org", "acme",
                "--snyk-all-projects",
                "--snyk-command", "/opt/bin/snyk"
            },
            Path.of("/tmp/project")
        );

        assertThat(parsed.projectDir()).isEqualTo(Path.of("/repo/app"));
        assertThat(parsed.tool()).isEqualTo(BuildTool.GRADLE);
        assertThat(parsed.scope()).isEqualTo("all");
        assertThat(parsed.snyk()).isEqualTo("true");
        assertThat(parsed.snykJson()).isEqualTo("snyk.json");
        assertThat(parsed.snykOrg()).isEqualTo("acme");
        assertThat(parsed.snykAllProjects()).isTrue();
        assertThat(parsed.snykCommand()).isEqualTo("/opt/bin/snyk");
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
rtk mvn -pl depviz-cli -Dtest=ProjectDetectorTest,CliOptionsTest test
```

Expected: FAIL because detector and option classes do not exist.

- [ ] **Step 3: Add detection classes**

Create `BuildTool.java`:

```java
package io.github.lucasgois1.depviz.cli;

public enum BuildTool {
    MAVEN,
    GRADLE
}
```

Create `ProjectDetector.java`:

```java
package io.github.lucasgois1.depviz.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectDetector {
    private ProjectDetector() {}

    public static DetectionResult detect(Path directory) {
        List<BuildTool> tools = new ArrayList<>();
        if (Files.isRegularFile(directory.resolve("pom.xml"))) {
            tools.add(BuildTool.MAVEN);
        }
        if (hasGradleBuild(directory)) {
            tools.add(BuildTool.GRADLE);
        }
        return new DetectionResult(List.copyOf(tools));
    }

    private static boolean hasGradleBuild(Path directory) {
        return Files.isRegularFile(directory.resolve("settings.gradle"))
            || Files.isRegularFile(directory.resolve("settings.gradle.kts"))
            || Files.isRegularFile(directory.resolve("build.gradle"))
            || Files.isRegularFile(directory.resolve("build.gradle.kts"));
    }

    public record DetectionResult(List<BuildTool> tools) {}
}
```

- [ ] **Step 4: Add minimal CLI option parser**

Create `CliOptions.java`:

```java
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
                case "--project-dir" -> projectDir = Path.of(requireValue(list, ++index, arg));
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
        if (index >= args.size()) {
            throw new IllegalArgumentException(option + " requires a value.");
        }
        return args.get(index);
    }

    private static BuildTool parseTool(String value) {
        if ("maven".equalsIgnoreCase(value)) return BuildTool.MAVEN;
        if ("gradle".equalsIgnoreCase(value)) return BuildTool.GRADLE;
        throw new IllegalArgumentException("--tool must be maven or gradle.");
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
rtk mvn -pl depviz-cli test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): detect project build tools"
```

---

### Task 11: Implement CLI Maven Execution

**Files:**
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/CommandRunner.java`
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/MavenCommandFactory.java`
- Modify: `Main.java`
- Add tests

- [ ] **Step 1: Write Maven command test**

Create `MavenCommandFactoryTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MavenCommandFactoryTest {
    @Test
    void buildsFullCoordinateMavenCommandWithDepvizProperties() {
        CliOptions options = new CliOptions(
            Path.of("/repo"),
            BuildTool.MAVEN,
            "compile",
            false,
            "target/custom",
            "force",
            "true",
            "snyk.json",
            "my-org",
            true,
            "/opt/bin/snyk"
        );

        assertThat(new MavenCommandFactory("0.1.0-SNAPSHOT").command(options, "mvn")).containsExactly(
            "mvn",
            "io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0-SNAPSHOT:open",
            "-Ddepviz.scope=compile",
            "-Ddepviz.open=false",
            "-Ddepviz.outputDirectory=target/custom",
            "-Ddepviz.layout=force",
            "-Ddepviz.snyk=true",
            "-Ddepviz.snykJson=snyk.json",
            "-Ddepviz.snykOrg=my-org",
            "-Ddepviz.snykAllProjects=true",
            "-Ddepviz.snykCommand=/opt/bin/snyk"
        );
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-cli -Dtest=MavenCommandFactoryTest test
```

Expected: FAIL because factory does not exist.

- [ ] **Step 3: Implement Maven command factory**

Create:

```java
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
        if (options.open() != null) add(command, "depviz.open", options.open().toString());
        add(command, "depviz.outputDirectory", options.output());
        add(command, "depviz.layout", options.layout());
        add(command, "depviz.snyk", options.snyk());
        add(command, "depviz.snykJson", options.snykJson());
        add(command, "depviz.snykOrg", options.snykOrg());
        if (options.snykAllProjects()) add(command, "depviz.snykAllProjects", "true");
        add(command, "depviz.snykCommand", options.snykCommand());
        return command;
    }

    private static void add(List<String> command, String key, String value) {
        if (value != null && !value.isBlank()) {
            command.add("-D" + key + "=" + value);
        }
    }
}
```

- [ ] **Step 4: Implement command runner**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class CommandRunner {
    public int run(Path directory, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
rtk mvn -pl depviz-cli test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): build maven plugin command"
```

---

### Task 12: Implement CLI Gradle Execution Via Init Script

**Files:**
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/GradleInitScriptWriter.java`
- Create: `depviz-cli/src/main/java/io/github/lucasgois1/depviz/cli/GradleCommandFactory.java`
- Add tests

- [ ] **Step 1: Write init script test**

Create `GradleInitScriptWriterTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleInitScriptWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesInitScriptApplyingMatchingPluginVersionAndFlags() throws Exception {
        CliOptions options = new CliOptions(tempDir, BuildTool.GRADLE, "runtime", false, "custom", "force", "true", "snyk.json", "org", true, "/opt/bin/snyk");

        Path script = new GradleInitScriptWriter("0.1.0-SNAPSHOT").write(tempDir, options);

        String text = Files.readString(script);
        assertThat(text).contains("classpath 'io.github.lucasgois1.depviz:depviz-gradle-plugin:0.1.0-SNAPSHOT'");
        assertThat(text).contains("project.apply plugin: 'io.github.lucasgois1.depviz'");
        assertThat(text).contains("scope.set('runtime')");
        assertThat(text).contains("open.set(false)");
        assertThat(text).contains("outputDirectory.set(project.layout.projectDirectory.dir('custom'))");
        assertThat(text).contains("snyk.set('true')");
        assertThat(text).contains("snykJson.set(project.layout.projectDirectory.file('snyk.json'))");
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-cli -Dtest=GradleInitScriptWriterTest test
```

Expected: FAIL because writer does not exist.

- [ ] **Step 3: Implement init script writer**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GradleInitScriptWriter {
    private final String version;

    public GradleInitScriptWriter(String version) {
        this.version = version;
    }

    public Path write(Path directory, CliOptions options) throws IOException {
        Path script = Files.createTempFile("depviz-", ".gradle");
        Files.writeString(script, scriptText(options));
        return script;
    }

    private String scriptText(CliOptions options) {
        StringBuilder builder = new StringBuilder();
        builder.append("initscript {\\n");
        builder.append("  repositories { mavenLocal(); gradlePluginPortal(); mavenCentral() }\\n");
        builder.append("  dependencies { classpath 'io.github.lucasgois1.depviz:depviz-gradle-plugin:").append(escape(version)).append("' }\\n");
        builder.append("}\\n");
        builder.append("allprojects { project ->\\n");
        builder.append("  if (project == project.rootProject) {\\n");
        builder.append("    project.apply plugin: 'io.github.lucasgois1.depviz'\\n");
        builder.append("    project.extensions.configure('depviz') { depviz ->\\n");
        builder.append("    depviz.scope.set('").append(escape(options.scope())).append("')\\n");
        if (options.open() != null) builder.append("    depviz.open.set(").append(options.open()).append(")\\n");
        if (options.output() != null) builder.append("    depviz.outputDirectory.set(project.layout.projectDirectory.dir('").append(escape(options.output())).append("'))\\n");
        if (options.layout() != null) builder.append("    depviz.layout.set('").append(escape(options.layout())).append("')\\n");
        if (options.snyk() != null) builder.append("    depviz.snyk.set('").append(escape(options.snyk())).append("')\\n");
        if (options.snykJson() != null) builder.append("    depviz.snykJson.set(project.layout.projectDirectory.file('").append(escape(options.snykJson())).append("'))\\n");
        builder.append("    }\\n");
        builder.append("  }\\n");
        builder.append("}\\n");
        return builder.toString();
    }

    private static String escape(String value) {
        return value.replace("\\\\", "\\\\\\\\").replace("'", "\\\\'");
    }
}
```

- [ ] **Step 4: Implement Gradle command factory**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.List;

public final class GradleCommandFactory {
    public List<String> command(String executable, Path initScript) {
        return List.of(executable, "--init-script", initScript.toString(), "depvizOpen");
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
rtk mvn -pl depviz-cli test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): build gradle init script command"
```

---

### Task 13: Wire CLI Main And Wrapper Selection

**Files:**
- Modify: `Main.java`
- Create: `ExecutableSelector.java`
- Add tests

- [ ] **Step 1: Write wrapper selection tests**

Create `ExecutableSelectorTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutableSelectorTest {
    @TempDir
    Path dir;

    @Test
    void prefersMavenWrapperWhenExecutable() throws Exception {
        Path wrapper = dir.resolve("mvnw");
        Files.writeString(wrapper, "#!/bin/sh\\n");
        wrapper.toFile().setExecutable(true);

        assertThat(ExecutableSelector.maven(dir)).isEqualTo("./mvnw");
    }

    @Test
    void fallsBackToMavenWhenWrapperMissing() {
        assertThat(ExecutableSelector.maven(dir)).isEqualTo("mvn");
    }

    @Test
    void prefersGradleWrapperWhenExecutable() throws Exception {
        Path wrapper = dir.resolve("gradlew");
        Files.writeString(wrapper, "#!/bin/sh\\n");
        wrapper.toFile().setExecutable(true);

        assertThat(ExecutableSelector.gradle(dir)).isEqualTo("./gradlew");
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
rtk mvn -pl depviz-cli -Dtest=ExecutableSelectorTest test
```

Expected: FAIL because selector does not exist.

- [ ] **Step 3: Add executable selector**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExecutableSelector {
    private ExecutableSelector() {}

    public static String maven(Path directory) {
        return executable(directory.resolve("mvnw")) ? "./mvnw" : "mvn";
    }

    public static String gradle(Path directory) {
        return executable(directory.resolve("gradlew")) ? "./gradlew" : "gradle";
    }

    private static boolean executable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }
}
```

- [ ] **Step 4: Wire `Main`**

Implement:

```java
package io.github.lucasgois1.depviz.cli;

import java.nio.file.Path;
import java.util.List;

public final class Main {
    static final String VERSION = "0.1.0-SNAPSHOT";

    private Main() {}

    public static void main(String[] args) throws Exception {
        int exitCode = run(args, Path.of("").toAbsolutePath().normalize(), System.console() != null);
        System.exit(exitCode);
    }

    static int run(String[] args, Path currentDirectory, boolean interactive) throws Exception {
        CliOptions options = CliOptions.parse(args, currentDirectory).withDefaultOpen(interactive);
        ProjectDetector.DetectionResult detection = ProjectDetector.detect(options.projectDir());
        BuildTool tool = selectTool(options, detection, interactive);
        CommandRunner runner = new CommandRunner();
        if (tool == BuildTool.MAVEN) {
            List<String> command = new MavenCommandFactory(VERSION).command(options, ExecutableSelector.maven(options.projectDir()));
            return runner.run(options.projectDir(), command);
        }
        Path initScript = new GradleInitScriptWriter(VERSION).write(options.projectDir(), options);
        return runner.run(options.projectDir(), new GradleCommandFactory().command(ExecutableSelector.gradle(options.projectDir()), initScript));
    }

    private static BuildTool selectTool(CliOptions options, ProjectDetector.DetectionResult detection, boolean interactive) {
        if (options.tool() != null) {
            if (!detection.tools().contains(options.tool())) {
                throw new IllegalArgumentException("Requested build tool was not detected in " + options.projectDir());
            }
            return options.tool();
        }
        if (detection.tools().isEmpty()) {
            throw new IllegalArgumentException("No Maven or Gradle project found in " + options.projectDir() + ". Run depviz from a directory containing pom.xml, build.gradle, or settings.gradle.");
        }
        if (detection.tools().size() == 1) {
            return detection.tools().get(0);
        }
        if (!interactive) {
            throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
        }
        throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
    }
}
```

This first wiring treats ambiguous projects the same way in interactive and non-interactive mode. The next task replaces the interactive branch with a prompt.

- [ ] **Step 5: Run CLI tests**

Run:

```bash
rtk mvn -pl depviz-cli test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): wire open command execution"
```

---

### Task 14: Add CLI Interactive Ambiguity Prompt

**Files:**
- Modify: `Main.java`
- Create: `ConsolePrompter.java`
- Add tests

- [ ] **Step 1: Write prompt tests**

Create `ConsolePrompterTest.java`:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class ConsolePrompterTest {
    @Test
    void selectsMavenFromChoiceOne() {
        ByteArrayInputStream input = new ByteArrayInputStream("1\\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        BuildTool tool = new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool();

        assertThat(tool).isEqualTo(BuildTool.MAVEN);
        assertThat(output.toString()).contains("Depviz found both Maven and Gradle");
    }

    @Test
    void selectsGradleFromChoiceTwo() {
        ByteArrayInputStream input = new ByteArrayInputStream("2\\n".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThat(new ConsolePrompter(input, new PrintStream(output)).chooseBuildTool()).isEqualTo(BuildTool.GRADLE);
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
rtk mvn -pl depviz-cli -Dtest=ConsolePrompterTest test
```

Expected: FAIL because prompter does not exist.

- [ ] **Step 3: Implement prompter**

Create:

```java
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
        output.println("2. Gradle (build.gradle / settings.gradle)");
        output.println("3. Cancel");
        output.println();
        output.print("Choose build tool: ");
        String choice = new Scanner(input).nextLine().trim();
        return switch (choice) {
            case "1" -> BuildTool.MAVEN;
            case "2" -> BuildTool.GRADLE;
            case "3" -> throw new IllegalArgumentException("Cancelled.");
            default -> throw new IllegalArgumentException("Invalid selection: " + choice);
        };
    }
}
```

- [ ] **Step 4: Wire prompt into `Main`**

Change `selectTool` to accept a prompter:

```java
private static BuildTool selectTool(
    CliOptions options,
    ProjectDetector.DetectionResult detection,
    boolean interactive,
    ConsolePrompter prompter
) {
    if (options.tool() != null) {
        if (!detection.tools().contains(options.tool())) {
            throw new IllegalArgumentException("Requested build tool was not detected in " + options.projectDir());
        }
        return options.tool();
    }
    if (detection.tools().isEmpty()) {
        throw new IllegalArgumentException("No Maven or Gradle project found in " + options.projectDir() + ". Run depviz from a directory containing pom.xml, build.gradle, or settings.gradle.");
    }
    if (detection.tools().size() == 1) {
        return detection.tools().get(0);
    }
    if (!interactive) {
        throw new IllegalArgumentException("Both Maven and Gradle were detected. Re-run with --tool maven or --tool gradle.");
    }
    return prompter.chooseBuildTool();
}
```

Call with:

```java
BuildTool tool = selectTool(options, detection, interactive, new ConsolePrompter(System.in, System.out));
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
rtk mvn -pl depviz-cli test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): prompt for ambiguous build tools"
```

---

### Task 15: Add CLI Fat Jar And Install Script

**Files:**
- Modify: `depviz-cli/pom.xml`
- Create: `scripts/install.sh`
- Create: `depviz-cli/src/main/assembly/bin.xml` if using assembly
- Update: `README.md`

- [ ] **Step 1: Add assembly plugin**

In `depviz-cli/pom.xml`, add:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.6.1</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals><goal>shade</goal></goals>
          <configuration>
            <createDependencyReducedPom>false</createDependencyReducedPom>
            <transformers>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>io.github.lucasgois1.depviz.cli.Main</mainClass>
              </transformer>
            </transformers>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

- [ ] **Step 2: Write jar execution test command**

Run:

```bash
rtk mvn -pl depviz-cli package
rtk java -jar depviz-cli/target/depviz-cli-0.1.0-SNAPSHOT.jar
```

Expected: second command exits non-zero with `Usage: depviz open [options]`.

- [ ] **Step 3: Create install script**

Create `scripts/install.sh`:

```sh
#!/bin/sh
set -eu

VERSION="${DEPVIZ_VERSION:-0.1.0-SNAPSHOT}"
BASE_URL="${DEPVIZ_BASE_URL:-https://github.com/lucasgois1/depviz/releases/download/v${VERSION}}"
INSTALL_DIR="${DEPVIZ_HOME:-$HOME/.depviz}"
BIN_DIR="$INSTALL_DIR/bin"
LIB_DIR="$INSTALL_DIR/lib"

mkdir -p "$BIN_DIR" "$LIB_DIR"

if [ -n "${DEPVIZ_LOCAL_JAR:-}" ]; then
  cp "$DEPVIZ_LOCAL_JAR" "$LIB_DIR/depviz-cli.jar"
else
  curl -fsSL "$BASE_URL/depviz-cli.jar" -o "$LIB_DIR/depviz-cli.jar"
fi

cat > "$BIN_DIR/depviz" <<LAUNCHER
#!/bin/sh
exec java -jar "$LIB_DIR/depviz-cli.jar" "\$@"
LAUNCHER

chmod +x "$BIN_DIR/depviz"

echo "Depviz installed at $BIN_DIR/depviz"
case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) echo "Add this to your PATH: export PATH=\"$BIN_DIR:\$PATH\"" ;;
esac
```

- [ ] **Step 4: Test install script with local jar**

Run:

```bash
rtk rm -rf target/install-smoke
rtk mkdir -p target/install-smoke
rtk env DEPVIZ_HOME="$PWD/target/install-smoke/.depviz" DEPVIZ_LOCAL_JAR="$PWD/depviz-cli/target/depviz-cli-0.1.0-SNAPSHOT.jar" sh scripts/install.sh
rtk target/install-smoke/.depviz/bin/depviz
```

Expected: install succeeds; CLI prints usage and exits non-zero.

- [ ] **Step 5: Update README install section**

Add:

```markdown
## Install CLI

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | sh
```

Then run from a Maven or Gradle project:

```bash
depviz open
```

The CLI does not modify `pom.xml`, `build.gradle`, or `settings.gradle`. It delegates to Maven or Gradle with the matching Depviz plugin version.
```
```

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add depviz-cli scripts README.md
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "feat(cli): package installable launcher"
```

---

### Task 16: Add CLI Maven Smoke Fixture

**Files:**
- Create: `depviz-cli/src/test/java/io/github/lucasgois1/depviz/cli/CliMavenSmokeTest.java`

- [ ] **Step 1: Write CLI Maven smoke test**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliMavenSmokeTest {
    @TempDir
    Path projectDir;

    @Test
    void cliRunsMavenPluginWithoutEditingPom() throws Exception {
        Files.writeString(projectDir.resolve("pom.xml"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.acme</groupId>
              <artifactId>cli-maven-sample</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>org.slf4j</groupId>
                  <artifactId>slf4j-api</artifactId>
                  <version>2.0.13</version>
                </dependency>
              </dependencies>
            </project>
            """);

        int exitCode = Main.run(
            new String[] {"open", "--tool", "maven", "--no-browser", "--no-snyk", "--scope", "runtime"},
            projectDir,
            false
        );

        assertThat(exitCode).isZero();
        assertThat(projectDir.resolve("target/depviz/dependency-graph.json")).exists();
        assertThat(Files.readString(projectDir.resolve("pom.xml"))).doesNotContain("depviz");
    }
}
```

- [ ] **Step 2: Run smoke test to verify failure or pass**

Run:

```bash
rtk mvn install -DskipTests
rtk mvn -pl depviz-cli -Dtest=CliMavenSmokeTest test
```

Expected: PASS after previous CLI wiring; if it fails because the plugin cannot resolve, ensure `mvn install -DskipTests` installed `depviz-maven-plugin`.

- [ ] **Step 3: Commit**

Run:

```bash
rtk git add depviz-cli/src/test/java/io/github/lucasgois1/depviz/cli/CliMavenSmokeTest.java
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "test(cli): smoke maven project execution"
```

---

### Task 17: Add CLI Gradle Smoke Fixture

**Files:**
- Create: `depviz-cli/src/test/java/io/github/lucasgois1/depviz/cli/CliGradleSmokeTest.java`

- [ ] **Step 1: Write CLI Gradle smoke test**

Create:

```java
package io.github.lucasgois1.depviz.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliGradleSmokeTest {
    @TempDir
    Path projectDir;

    @Test
    void cliRunsGradlePluginWithoutEditingBuildFile() throws Exception {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"cli-gradle-sample\"\\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins { java }
            repositories { mavenCentral() }
            dependencies { runtimeOnly("org.slf4j:slf4j-api:2.0.13") }
            """);

        int exitCode = Main.run(
            new String[] {"open", "--tool", "gradle", "--no-browser", "--no-snyk", "--scope", "runtime"},
            projectDir,
            false
        );

        assertThat(exitCode).isZero();
        assertThat(projectDir.resolve("build/depviz/dependency-graph.json")).exists();
        assertThat(Files.readString(projectDir.resolve("build.gradle.kts"))).doesNotContain("depviz");
    }
}
```

- [ ] **Step 2: Run smoke test**

Run:

```bash
rtk mvn install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:publishToMavenLocal
rtk mvn -pl depviz-cli -Dtest=CliGradleSmokeTest test
```

Expected: PASS. The Gradle plugin is published to Maven local by the `maven-publish` plugin configured in Task 5.

- [ ] **Step 3: Commit**

Run:

```bash
rtk git add depviz-cli depviz-gradle-plugin
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "test(cli): smoke gradle project execution"
```

---

### Task 18: Update Documentation For Maven, Gradle, CLI, And Publishing

**Files:**
- Modify: `README.md`
- Create: `docs/publishing.md`

- [ ] **Step 1: Update README commands**

Ensure README includes:

```markdown
## Quick Start

Install the CLI:

```bash
curl -fsSL https://raw.githubusercontent.com/lucasgois1/depviz/main/scripts/install.sh | sh
```

Run from a Maven or Gradle project root:

```bash
depviz open
```

Depviz does not modify the analyzed project's `pom.xml`, `build.gradle`, or `settings.gradle`.
```
```

- [ ] **Step 2: Document direct Maven usage**

Add:

```markdown
### Direct Maven Usage

```bash
mvn io.github.lucasgois1.depviz:depviz-maven-plugin:0.1.0-SNAPSHOT:open
```

Use the full coordinate so no Maven `pluginGroups` setup is required.
```
```

- [ ] **Step 3: Document direct Gradle usage**

Add:

```markdown
### Direct Gradle Usage

When applied to a Gradle project:

```kotlin
plugins {
  id("io.github.lucasgois1.depviz") version "0.1.0-SNAPSHOT"
}
```

Run:

```bash
./gradlew depvizOpen
```

The CLI path applies the plugin through a temporary init script, so direct Gradle setup is optional.
```
```

- [ ] **Step 4: Document publication prep**

Create `docs/publishing.md`:

```markdown
# Publishing Depviz

Depviz uses one version across core, Maven plugin, Gradle plugin, CLI, schema, and viewer.

## Local Verification

```bash
mvn verify
mvn install -DskipTests
./gradlew :depviz-gradle-plugin:test
./gradlew :depviz-gradle-plugin:publishToMavenLocal
```

## Public Distribution Targets

- Maven Central: `depviz-core`, `depviz-maven-plugin`, `depviz-cli`
- Gradle Plugin Portal: `io.github.lucasgois1.depviz`
- GitHub Releases: `depviz-cli.jar`, `depviz`, `install.sh`, `checksums.txt`

Remote publication requires signing credentials, Maven Central namespace setup, Gradle Plugin Portal credentials, and a GitHub release token.
```

- [ ] **Step 5: Run docs grep**

Run:

```bash
rtk rg -n "dev\\.gois\\.tools|lucasgois/depviz" README.md docs pom.xml depviz-* src viewer
```

Expected: no stale public coordinates or old GitHub URL remain, except in historical docs if intentionally preserved. If historical docs mention old coordinates, add a note that they describe the pre-public prototype.

- [ ] **Step 6: Commit**

Run:

```bash
rtk git add README.md docs
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "docs: describe cli gradle and publishing"
```

---

### Task 19: Final Verification And Release Artifact Smoke

**Files:**
- No planned source edits unless verification exposes a bug.

- [ ] **Step 1: Run full Maven verification**

Run:

```bash
rtk mvn verify
```

Expected:

- core tests pass
- Maven plugin tests pass
- viewer tests pass
- Maven invoker tests pass
- CLI tests pass
- Gradle plugin tests remain covered by the separate Gradle verification step

- [ ] **Step 2: Run Gradle verification**

Run:

```bash
rtk mvn -pl depviz-core -am install -DskipTests
rtk ./gradlew :depviz-gradle-plugin:test
```

Expected: Gradle TestKit tests pass.

- [ ] **Step 3: Build installable CLI**

Run:

```bash
rtk mvn -pl depviz-cli package
rtk ls -lh depviz-cli/target/depviz-cli-0.1.0-SNAPSHOT.jar
```

Expected: CLI jar exists and has a main class.

- [ ] **Step 4: Install CLI locally from local jar**

Run:

```bash
rtk rm -rf target/install-smoke
rtk env DEPVIZ_HOME="$PWD/target/install-smoke/.depviz" DEPVIZ_LOCAL_JAR="$PWD/depviz-cli/target/depviz-cli-0.1.0-SNAPSHOT.jar" sh scripts/install.sh
rtk target/install-smoke/.depviz/bin/depviz
```

Expected: install succeeds; bare CLI prints usage and exits non-zero.

- [ ] **Step 5: Run Maven CLI smoke manually**

Run:

```bash
rtk mvn install -DskipTests
rtk target/install-smoke/.depviz/bin/depviz open --project-dir depviz-maven-plugin/src/it/simple-project --tool maven --no-browser --no-snyk --scope runtime
```

Expected: generated HTML exists under `depviz-maven-plugin/src/it/simple-project/target/depviz/dependency-graph.html`.

- [ ] **Step 6: Run Gradle CLI smoke manually**

Create `target/gradle-cli-smoke/settings.gradle.kts`:

```kotlin
rootProject.name = "gradle-cli-smoke"
```

Create `target/gradle-cli-smoke/build.gradle.kts`:

```kotlin
plugins { java }
repositories { mavenCentral() }
dependencies { runtimeOnly("org.slf4j:slf4j-api:2.0.13") }
```

Run:

```bash
rtk ./gradlew :depviz-gradle-plugin:publishToMavenLocal
rtk target/install-smoke/.depviz/bin/depviz open --project-dir target/gradle-cli-smoke --tool gradle --no-browser --no-snyk --scope runtime
```

Expected: generated HTML exists under `target/gradle-cli-smoke/build/depviz/dependency-graph.html`.

- [ ] **Step 7: Run Snyk JSON smoke**

Use the existing Snyk JSON fixture through Maven:

```bash
rtk target/install-smoke/.depviz/bin/depviz open --project-dir depviz-maven-plugin/src/it/snyk-json-project --tool maven --no-browser --snyk-json snyk-report.json --scope runtime
```

Expected: generated JSON has `securitySummary.checked=true` and one high finding.

- [ ] **Step 8: Run local Snyk CLI smoke when available**

Run:

```bash
rtk which snyk && rtk target/install-smoke/.depviz/bin/depviz open --project-dir . --tool maven --no-browser --snyk --scope runtime
```

Expected: if Snyk is installed/authenticated, generated JSON includes `securitySummary.checked=true`; if Snyk has an operational failure, generation succeeds with Snyk diagnostics.

- [ ] **Step 9: Inspect generated JSON**

Run:

```bash
rtk node -e 'const fs=require("fs"); const p="target/depviz/dependency-graph.json"; const j=JSON.parse(fs.readFileSync(p,"utf8")); console.log(JSON.stringify({project:j.project, securitySummary:j.securitySummary, diagnostics:j.diagnostics.filter(d=>String(d.type).startsWith("snyk"))}, null, 2));'
```

Expected: output clearly shows the current project info and Snyk status or diagnostics.

- [ ] **Step 10: Final code review**

Dispatch a read-only final reviewer over the full implementation. Ask it to focus on:

- dependency leakage from core into Maven/Gradle APIs
- CLI accidentally modifying analyzed projects
- Gradle init script correctness
- stale coordinates/packages
- graph deduplication regressions
- Maven/Gradle output defaults
- release/install script safety

- [ ] **Step 11: Commit final fixes if needed**

If verification or review exposes bugs, fix them with TDD and commit:

```bash
rtk git add <changed-files>
rtk git -c commit.gpgsign=false -c gpg.format= commit -m "fix: address public distribution verification"
```

If no fixes are needed, do not create an empty commit.
