import type { Finding } from "./types";

// A piece of the message. If findings is empty it's plain text, otherwise it's flagged.
export interface Segment {
  text: string;
  findings: Finding[];
}

// Cuts the message into pieces: plain text and flagged text.
// If two warning signs overlap, they share one flagged piece
// (for example a link that is both shortened and a lookalike).
export function buildSegments(text: string, findings: Finding[]): Segment[] {
  // keep only findings that fit inside the text, and put them in order
  const sorted = findings
    .filter((f) => f.start >= 0 && f.end > f.start && f.end <= text.length)
    .sort((a, b) => a.start - b.start || a.end - b.end);

  const segments: Segment[] = [];
  let cursor = 0; // how much of the text we've already used
  let i = 0;

  while (i < sorted.length) {
    // start a group with this finding, then add every finding that overlaps it
    const group: Finding[] = [sorted[i]];
    const groupStart = sorted[i].start;
    let groupEnd = sorted[i].end;
    i++;

    while (i < sorted.length && sorted[i].start < groupEnd) {
      group.push(sorted[i]);
      groupEnd = Math.max(groupEnd, sorted[i].end);
      i++;
    }

    // plain text before the group, then the flagged piece itself
    if (groupStart > cursor) {
      segments.push({ text: text.slice(cursor, groupStart), findings: [] });
    }
    segments.push({ text: text.slice(groupStart, groupEnd), findings: group });
    cursor = groupEnd;
  }

  // whatever is left after the last flagged piece
  if (cursor < text.length) {
    segments.push({ text: text.slice(cursor), findings: [] });
  }

  return segments;
}
