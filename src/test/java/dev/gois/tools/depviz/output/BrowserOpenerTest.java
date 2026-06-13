package dev.gois.tools.depviz.output;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.URI;
import org.junit.jupiter.api.Test;

class BrowserOpenerTest {
    @Test
    void doesNotThrowWhenDesktopIsUnavailable() {
        BrowserOpener opener = new BrowserOpener(false);

        assertThatCode(() -> opener.open(URI.create("file:///tmp/dependency-graph.html"))).doesNotThrowAnyException();
    }
}
