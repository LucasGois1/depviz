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

dependencies {
    implementation("io.github.lucasgois1.depviz:depviz-core:${project.version}")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.1")
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
