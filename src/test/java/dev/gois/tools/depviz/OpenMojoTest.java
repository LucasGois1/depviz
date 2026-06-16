package dev.gois.tools.depviz;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gois.tools.depviz.config.DepvizConfig;
import dev.gois.tools.depviz.config.SnykMode;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

class OpenMojoTest {
    @Test
    void pluginDescriptorDefinesOpenMojo() throws Exception {
        assertThat(AbstractMojo.class.isAssignableFrom(OpenMojo.class)).isTrue();

        Document descriptor = readPluginDescriptor();
        assertThat(text(descriptor, "/plugin/requiredMavenVersion")).isEqualTo("3.9.0");
        Node openMojo = node(descriptor, "/plugin/mojos/mojo[goal='open']");
        assertThat(openMojo).isNotNull();
        assertThat(text(openMojo, "goal")).isEqualTo("open");
        assertThat(text(openMojo, "requiresProject")).isEqualTo("true");
        assertThat(text(openMojo, "aggregator")).isEqualTo("true");
        assertThat(text(openMojo, "threadSafe")).isEqualTo("true");
        Node outputDirectory = node(openMojo, "parameters/parameter[name='outputDirectory']");
        assertThat(outputDirectory).isNotNull();
        assertThat(text(openMojo, "configuration/outputDirectory/@default-value"))
            .isEqualTo("${project.build.directory}/depviz");
        assertThat(text(openMojo, "configuration/reactorProjects/@default-value")).isEqualTo("${reactorProjects}");
        assertThat(text(openMojo, "configuration/snyk")).isEqualTo("${depviz.snyk}");
        assertThat(text(openMojo, "configuration/snykJson")).isEqualTo("${depviz.snykJson}");
        assertThat(text(openMojo, "configuration/snykCommand")).isEqualTo("${depviz.snykCommand}");
        assertThat(text(openMojo, "configuration/snykOrg")).isEqualTo("${depviz.snykOrg}");
        assertThat(text(openMojo, "configuration/snykAllProjects")).isEqualTo("${depviz.snykAllProjects}");

        Method execute = OpenMojo.class.getMethod("execute");
        assertThat(execute.getReturnType()).isEqualTo(Void.TYPE);
    }

    @Test
    void parseConfigForwardsSnykParameters() throws Exception {
        OpenMojo mojo = new OpenMojo();
        setField(mojo, "snyk", "true");
        setField(mojo, "snykJson", "/tmp/snyk.json");
        setField(mojo, "snykCommand", "/opt/bin/snyk");
        setField(mojo, "snykOrg", "depviz-org");
        setField(mojo, "snykAllProjects", "true");

        DepvizConfig config = parseConfig(mojo);

        assertThat(config.snykMode()).isEqualTo(SnykMode.TRUE);
        assertThat(config.snykJson()).isEqualTo(Path.of("/tmp/snyk.json"));
        assertThat(config.snykCommand()).isEqualTo("/opt/bin/snyk");
        assertThat(config.snykOrg()).isEqualTo("depviz-org");
        assertThat(config.snykAllProjects()).isTrue();
    }

    private static Document readPluginDescriptor() throws Exception {
        try (InputStream inputStream = OpenMojoTest.class.getClassLoader()
                .getResourceAsStream("META-INF/maven/plugin.xml")) {
            assertThat(inputStream).isNotNull();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    private static Node node(Object item, String expression) throws Exception {
        return (Node) XPathFactory.newInstance().newXPath().evaluate(expression, item, XPathConstants.NODE);
    }

    private static String text(Object item, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, item);
    }

    private static DepvizConfig parseConfig(OpenMojo mojo) throws Exception {
        Method method = OpenMojo.class.getDeclaredMethod("parseConfig");
        method.setAccessible(true);
        return (DepvizConfig) method.invoke(mojo);
    }

    private static void setField(OpenMojo mojo, String fieldName, Object value) throws Exception {
        Field field = OpenMojo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
    }
}
