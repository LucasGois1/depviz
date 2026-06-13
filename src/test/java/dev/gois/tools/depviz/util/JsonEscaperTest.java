package dev.gois.tools.depviz.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonEscaperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void escapesLowercaseClosingScriptTag() {
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"</script>\"}")).doesNotContain("</script>");
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"</script>\"}")).contains("\\u003C/script>");
    }

    @Test
    void escapesMixedCaseClosingScriptTag() {
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"</ScRiPt>\"}")).doesNotContain("</ScRiPt>");
    }

    @Test
    void escapesHtmlCommentOpenSequence() {
        assertThat(JsonEscaper.forInlineScript("{\"value\":\"<!--\"}")).doesNotContain("<!--");
    }

    @Test
    void escapedJsonRemainsParseable() throws Exception {
        String json = "{\"value\":\"</ScRiPt><script>alert(1)</script><!--\"}";

        JsonNode original = objectMapper.readTree(json);
        JsonNode escaped = objectMapper.readTree(JsonEscaper.forInlineScript(json));

        assertThat(escaped).isEqualTo(original);
    }
}
