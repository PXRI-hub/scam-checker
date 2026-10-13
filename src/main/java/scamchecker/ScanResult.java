package scamchecker;

import java.util.List;

// What the scanner gives back: every red flag, a score from 0 to 10,
// and a simple level (Low, Medium or High).
public record ScanResult(List<Finding> findings, int score, String level) {
}
