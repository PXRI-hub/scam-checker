package scamchecker;

// One red flag found in a message.
// type  = what kind of red flag it is (like URGENCY or GIFT_CARD)
// start and end = where it sits in the text, so a web page can highlight it later
// text  = the exact words that triggered it
// why   = a plain explanation for the person reading
public record Finding(String type, int start, int end, String text, String why) {
}
