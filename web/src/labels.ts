// Friendly names for the kinds of warning signs, and how serious each one is.

export const LABELS: Record<string, string> = {
  URGENCY: "Pressure to act fast",
  GIFT_CARD_OR_CRYPTO: "Gift card or crypto payment",
  ACCOUNT_THREAT: "Account threat",
  PRIZE: "Prize claim",
  GOVERNMENT_THREAT: "Government threat",
  SECRET_REQUEST: "Asks for a code or password",
  DELIVERY_SCAM: "Fake delivery notice",
  PAYMENT_LINK: "Click-to-pay request",
  SHORT_LINK: "Shortened link",
  IP_LINK: "Number-address link",
  ODD_LINK: "Suspicious link",
  LOOKALIKE_DOMAIN: "Lookalike website",
};

// These are the most worrying signs
const STRONG = new Set([
  "GIFT_CARD_OR_CRYPTO",
  "GOVERNMENT_THREAT",
  "SECRET_REQUEST",
  "LOOKALIKE_DOMAIN",
]);

export function labelFor(type: string): string {
  return LABELS[type] ?? type;
}

export function isStrong(type: string): boolean {
  return STRONG.has(type);
}
