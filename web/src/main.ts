import "./style.css";
import { scanMessage } from "./api";
import { buildSegments } from "./highlight";
import { isStrong, labelFor } from "./labels";
import { EXAMPLES } from "./examples";
import type { ScanResult } from "./types";

const MAX_CHARS = 5000;
const METER_BLOCKS = 10;

// Grab a page element, and fail loudly if the HTML and code ever get out of sync.
function el<T extends HTMLElement>(id: string): T {
  const found = document.getElementById(id);
  if (!found) {
    throw new Error(`Missing element: ${id}`);
  }
  return found as T;
}

const message = el<HTMLTextAreaElement>("message");
const counter = el<HTMLSpanElement>("counter");
const checkButton = el<HTMLButtonElement>("check");
const clearButton = el<HTMLButtonElement>("clear");
const statusLine = el<HTMLParagraphElement>("status");
const resultBox = el<HTMLElement>("result");
const levelText = el<HTMLParagraphElement>("level");
const scoreText = el<HTMLParagraphElement>("score");
const meter = el<HTMLDivElement>("meter");
const marked = el<HTMLParagraphElement>("marked");
const findingsList = el<HTMLUListElement>("findings");

function updateCounter(): void {
  counter.textContent = `${message.value.length} / ${MAX_CHARS}`;
}

function setStatus(text: string): void {
  statusLine.textContent = text;
}

// The meter is 10 blocks. The first `score` blocks light up.
function drawMeter(result: ScanResult): void {
  meter.replaceChildren();
  meter.dataset.level = result.level.toLowerCase();
  meter.setAttribute("aria-label", `Score ${result.score} out of ${METER_BLOCKS}`);
  for (let i = 0; i < METER_BLOCKS; i++) {
    const block = document.createElement("span");
    block.className = i < result.score ? "block on" : "block";
    meter.appendChild(block);
  }
}

// The message with flagged words wrapped in <mark>.
// Built with text nodes only, so the user's text can never run as HTML.
function drawMarkedText(text: string, result: ScanResult): void {
  marked.replaceChildren();
  for (const segment of buildSegments(text, result.findings)) {
    if (segment.findings.length === 0) {
      marked.appendChild(document.createTextNode(segment.text));
      continue;
    }
    const mark = document.createElement("mark");
    mark.textContent = segment.text;
    if (segment.findings.some((f) => isStrong(f.type))) {
      mark.className = "strong";
    }
    mark.title = segment.findings.map((f) => labelFor(f.type)).join(", ");
    marked.appendChild(mark);
  }
}

function drawFindings(result: ScanResult): void {
  findingsList.replaceChildren();

  if (result.findings.length === 0) {
    const item = document.createElement("li");
    item.className = "none";
    item.textContent = "No warning signs found. That doesn't guarantee it's safe, so still be careful.";
    findingsList.appendChild(item);
    return;
  }

  for (const finding of result.findings) {
    const item = document.createElement("li");
    if (isStrong(finding.type)) {
      item.className = "strong";
    }

    const title = document.createElement("strong");
    title.textContent = labelFor(finding.type);

    const quote = document.createElement("code");
    quote.textContent = finding.text;

    const why = document.createElement("span");
    why.className = "why";
    why.textContent = finding.why;

    item.append(title, quote, why);
    findingsList.appendChild(item);
  }
}

function showResult(text: string, result: ScanResult): void {
  levelText.textContent = result.level;
  levelText.dataset.level = result.level.toLowerCase();
  scoreText.textContent = `Score ${result.score} out of ${METER_BLOCKS}`;
  drawMeter(result);
  drawMarkedText(text, result);
  drawFindings(result);
  resultBox.hidden = false;
}

async function check(): Promise<void> {
  const text = message.value;
  if (text.trim() === "") {
    setStatus("Paste a message first.");
    return;
  }

  checkButton.disabled = true;
  setStatus("Checking...");

  // Free hosting puts the server to sleep. Tell the user why it's slow instead of leaving them guessing.
  const slowTimer = window.setTimeout(() => {
    setStatus("Waking up the server, this can take up to a minute the first time...");
  }, 4000);

  try {
    const result = await scanMessage(text);
    setStatus("");
    showResult(text, result);
    resultBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch (error) {
    resultBox.hidden = true;
    // fetch throws a TypeError when the server can't be reached at all
    if (error instanceof TypeError) {
      setStatus("Couldn't reach the checker. Is the Java server running?");
    } else {
      setStatus(error instanceof Error ? error.message : "Something went wrong.");
    }
  } finally {
    window.clearTimeout(slowTimer);
    checkButton.disabled = false;
  }
}

function clearAll(): void {
  message.value = "";
  resultBox.hidden = true;
  setStatus("");
  updateCounter();
  message.focus();
}

message.addEventListener("input", updateCounter);
checkButton.addEventListener("click", () => void check());
clearButton.addEventListener("click", clearAll);
// Ctrl/Cmd + Enter checks, like many chat boxes
message.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
    void check();
  }
});

document.querySelectorAll<HTMLButtonElement>("[data-example]").forEach((button) => {
  button.addEventListener("click", () => {
    message.value = EXAMPLES[button.dataset.example ?? ""] ?? "";
    updateCounter();
    void check();
  });
});

updateCounter();
