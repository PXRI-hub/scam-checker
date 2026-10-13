import type { ScanResult } from "./types";

// Where the Java server lives. Set VITE_API_URL in a .env file to change it.
const API_URL: string =
  (import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8080";

// Sends the message to the Java server and returns what it found.
export async function scanMessage(text: string): Promise<ScanResult> {
  const response = await fetch(`${API_URL}/scan`, {
    method: "POST",
    headers: { "Content-Type": "text/plain; charset=utf-8" },
    body: text,
  });

  if (!response.ok) {
    // The server sends {"error": "..."} when something is wrong. Show that if we can.
    let message = "Something went wrong. Please try again.";
    try {
      const data = (await response.json()) as { error?: string };
      if (data.error) {
        message = data.error;
      }
    } catch {
      // the reply wasn't JSON, so we keep the general message
    }
    throw new Error(message);
  }

  return (await response.json()) as ScanResult;
}
