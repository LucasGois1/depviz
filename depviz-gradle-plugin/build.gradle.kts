plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.lucasgois1.depviz"
version = "0.1.0-SNAPSHOT"
description = "Offline interactive Java dependency graph viewer for Gradle projects."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation("io.github.lucasgois1.depviz:depviz-core:${project.version}")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.1")
}

gradlePlugin {
    website.set("https://github.com/lucasgois1/depviz")
    vcsUrl.set("https://github.com/lucasgois1/depviz")
    plugins {
        create("depviz") {
            id = "io.github.lucasgois1.depviz"
            implementationClass = "io.github.lucasgois1.depviz.gradle.DepvizGradlePlugin"
            displayName = "Depviz"
            description = "Generate an offline interactive dependency graph viewer for Java projects."
            tags.set(listOf("dependency-graph", "java", "maven", "snyk", "visualization"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
