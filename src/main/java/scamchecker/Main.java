package scamchecker;

import java.io.IOException;

// A tiny command-line way to try the scanner.
// Usage: pass the message as text, or pipe it in.
public class Main {
    public static void main(String[] args) throws IOException {
        String text;
        if (args.length > 0) {
            text = String.join(" ", args);
        } else {
            text = new String(System.in.readAllBytes());
        }

        ScanResult result = new ScamScanner().scan(text);

        System.out.println("Risk: " + result.level() + " (score " + result.score() + " out of 10)");
        if (result.findings().isEmpty()) {
            System.out.println("No warning signs found. That does not prove the message is safe.");
            return;
        }
        for (Finding finding : result.findings()) {
            System.out.println("- [" + finding.type() + "] \"" + finding.text() + "\"");
            System.out.println("    " + finding.why());
        }
    }
}
