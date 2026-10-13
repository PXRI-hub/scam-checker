package scamchecker;

import java.util.List;

// Builds the JSON text we send back to the web page.
// I wrote this by hand so the project doesn't need any extra libraries.
final class Json {

    private Json() {
    }

    // Makes a piece of text safe to put inside "quotes" in JSON.
    // Quotes, backslashes and new lines would break the JSON if we left them as they are.
    static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // other invisible control characters get written as a backslash, "u" and 4 digits
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    // Turns a scan result into JSON like:
    // {"score":7,"level":"High","findings":[{"type":"URGENCY","start":0,"end":6,"text":"URGENT","why":"..."}]}
    static String toJson(ScanResult result) {
        StringBuilder out = new StringBuilder();
        out.append("{\"score\":").append(result.score());
        out.append(",\"level\":\"").append(escape(result.level())).append("\"");
        out.append(",\"findings\":[");

        List<Finding> findings = result.findings();
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            if (i > 0) {
                out.append(",");
            }
            out.append("{\"type\":\"").append(escape(finding.type())).append("\"");
            out.append(",\"start\":").append(finding.start());
            out.append(",\"end\":").append(finding.end());
            out.append(",\"text\":\"").append(escape(finding.text())).append("\"");
            out.append(",\"why\":\"").append(escape(finding.why())).append("\"}");
        }

        out.append("]}");
        return out.toString();
    }

    // A small error message, like {"error":"Please paste a message."}
    static String error(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }
}
