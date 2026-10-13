// The shapes of the data the Java server sends back.

export interface Finding {
  type: string; // the kind of warning sign, like "URGENCY"
  start: number; // where it starts in the message
  end: number; // where it ends in the message
  text: string; // the exact words
  why: string; // a plain explanation
}

export interface ScanResult {
  score: number; // 0 to 10
  level: "Low" | "Medium" | "High";
  findings: Finding[];
}
