package dev.gois.tools.depviz.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonEscaperTest {
    @Test
    void escapesClosingScriptTag() {
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"</script>\"}")).doesNotContain("</script>");
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"</script>\"}")).contains("<\\/script>");
    }
}
