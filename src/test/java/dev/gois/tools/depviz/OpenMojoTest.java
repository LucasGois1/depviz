package dev.gois.tools.depviz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
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
        assertThat(text(openMojo, "threadSafe")).isEqualTo("true");

        Method execute = OpenMojo.class.getMethod("execute");
        assertThat(execute.getReturnType()).isEqualTo(Void.TYPE);
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
}
