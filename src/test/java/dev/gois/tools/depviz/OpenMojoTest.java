package dev.gois.tools.depviz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.apache.maven.plugin.AbstractMojo;
import org.junit.jupiter.api.Test;

class OpenMojoTest {
    @Test
    void isMavenMojoNamedOpen() throws Exception {
        assertThat(AbstractMojo.class.isAssignableFrom(OpenMojo.class)).isTrue();
        String classBytes = readClassBytes(OpenMojo.class);
        assertThat(classBytes).contains("Lorg/apache/maven/plugins/annotations/Mojo;");
        assertThat(classBytes).contains("open");
        Method execute = OpenMojo.class.getMethod("execute");
        assertThat(execute.getReturnType()).isEqualTo(Void.TYPE);
    }

    private static String readClassBytes(Class<?> type) throws IOException {
        String resourceName = type.getSimpleName() + ".class";
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
