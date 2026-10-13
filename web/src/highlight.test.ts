import { describe, expect, it } from "vitest";
import { buildSegments } from "./highlight";
import type { Finding } from "./types";

function finding(type: string, start: number, end: number): Finding {
  return { type, start, end, text: "", why: "" };
}

describe("buildSegments", () => {
  it("returns one plain piece when nothing is flagged", () => {
    const segments = buildSegments("hello there", []);
    expect(segments).toEqual([{ text: "hello there", findings: [] }]);
  });

  it("returns nothing for empty text", () => {
    expect(buildSegments("", [])).toEqual([]);
  });

  it("splits plain, flagged, plain", () => {
    const text = "this is URGENT now";
    const segments = buildSegments(text, [finding("URGENCY", 8, 14)]);
    expect(segments.map((s) => s.text)).toEqual(["this is ", "URGENT", " now"]);
    expect(segments[1].findings).toHaveLength(1);
    expect(segments[0].findings).toHaveLength(0);
  });

  it("handles a finding at the very start and very end", () => {
    const text = "abcdef";
    const segments = buildSegments(text, [finding("A", 0, 2), finding("B", 4, 6)]);
    expect(segments.map((s) => s.text)).toEqual(["ab", "cd", "ef"]);
  });

  it("merges overlapping findings into one flagged piece", () => {
    const text = "see http://bit.ly/x now";
    const segments = buildSegments(text, [finding("SHORT_LINK", 4, 19), finding("ODD_LINK", 4, 15)]);
    expect(segments.map((s) => s.text)).toEqual(["see ", "http://bit.ly/x", " now"]);
    expect(segments[1].findings).toHaveLength(2);
  });

  it("keeps separate findings that only touch", () => {
    const segments = buildSegments("abcd", [finding("A", 0, 2), finding("B", 2, 4)]);
    expect(segments.map((s) => s.text)).toEqual(["ab", "cd"]);
    expect(segments.every((s) => s.findings.length === 1)).toBe(true);
  });

  it("ignores findings that don't fit inside the text", () => {
    const segments = buildSegments("abc", [finding("A", 2, 99), finding("B", -1, 2), finding("C", 2, 2)]);
    expect(segments).toEqual([{ text: "abc", findings: [] }]);
  });

  it("always rebuilds the original text", () => {
    const text = "URGENT: buy gift cards at http://bit.ly/zz. Thanks!";
    const segments = buildSegments(text, [
      finding("URGENCY", 0, 6),
      finding("GIFT", 12, 22),
      finding("SHORT", 26, 44),
    ]);
    expect(segments.map((s) => s.text).join("")).toBe(text);
  });
});
