package scamchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void escapesQuotesBackslashesAndNewLines() {
        String escaped = Json.escape("He said \"hi\"\nC:\\temp");
        assertEquals("He said \\\"hi\\\"\\nC:\\\\temp", escaped);
    }

    @Test
    void escapesHiddenControlCharacters() {
        assertEquals("a\\u0001b", Json.escape("a\u0001b"));
    }

    @Test
    void resultTurnsIntoJson() {
        Finding finding = new Finding("URGENCY", 0, 6, "URGENT", "Rushing you.");
        ScanResult result = new ScanResult(List.of(finding), 2, "Low");
        String json = Json.toJson(result);
        assertTrue(json.startsWith("{\"score\":2,\"level\":\"Low\",\"findings\":[{"));
        assertTrue(json.contains("\"type\":\"URGENCY\""));
        assertTrue(json.contains("\"start\":0,\"end\":6"));
        assertTrue(json.endsWith("}]}"));
    }

    @Test
    void emptyResultHasAnEmptyList() {
        String json = Json.toJson(new ScanResult(List.of(), 0, "Low"));
        assertEquals("{\"score\":0,\"level\":\"Low\",\"findings\":[]}", json);
    }

    @Test
    void errorMessageIsEscaped() {
        assertEquals("{\"error\":\"Say \\\"no\\\"\"}", Json.error("Say \"no\""));
    }
}
