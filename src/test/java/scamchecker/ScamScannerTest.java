package scamchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScamScannerTest {

    private final ScamScanner scanner = new ScamScanner();

    // helper: does the result contain a finding of this type?
    private boolean has(ScanResult result, String type) {
        for (Finding finding : result.findings()) {
            if (finding.type().equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void normalMessageIsLowRisk() {
        ScanResult result = scanner.scan("Hey, are we still meeting for lunch on Thursday?");
        assertEquals(0, result.score());
        assertEquals("Low", result.level());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void emptyAndNullTextAreSafe() {
        assertEquals(0, scanner.scan("").score());
        assertEquals(0, scanner.scan("   ").score());
        assertEquals(0, scanner.scan(null).score());
    }

    @Test
    void giftCardRequestIsFlagged() {
        ScanResult result = scanner.scan("Please buy a Google Play card and send me the numbers.");
        assertTrue(has(result, "GIFT_CARD_OR_CRYPTO"));
    }

    @Test
    void urgencyIsFlagged() {
        ScanResult result = scanner.scan("URGENT: reply within 24 hours");
        assertTrue(has(result, "URGENCY"));
    }

    @Test
    void asksForVerificationCodeIsFlagged() {
        ScanResult result = scanner.scan("Please read me the verification code we just texted you.");
        assertTrue(has(result, "SECRET_REQUEST"));
    }

    @Test
    void lookalikeDomainIsFlagged() {
        ScanResult result = scanner.scan("Log in at https://paypal-secure-login.example.xyz/verify now");
        assertTrue(has(result, "LOOKALIKE_DOMAIN"));
    }

    @Test
    void realDomainIsNotFlaggedAsLookalike() {
        ScanResult result = scanner.scan("See https://www.paypal.com/help and https://mail.paypal.com");
        assertFalse(has(result, "LOOKALIKE_DOMAIN"));
    }

    @Test
    void atSignTrickIsFlagged() {
        ScanResult result = scanner.scan("Go to http://paypal.com@evil.example/login");
        assertTrue(has(result, "ODD_LINK"));
        // the real site is evil.example, so there is no lookalike on a paypal host
        assertFalse(has(result, "LOOKALIKE_DOMAIN"));
    }

    @Test
    void shortenedLinkIsFlagged() {
        ScanResult result = scanner.scan("Claim here: https://bit.ly/3abcXYZ");
        assertTrue(has(result, "SHORT_LINK"));
    }

    @Test
    void numberAddressLinkIsFlagged() {
        ScanResult result = scanner.scan("Update your details at http://192.168.4.20/login");
        assertTrue(has(result, "IP_LINK"));
    }

    @Test
    void trailingPunctuationIsNotPartOfTheLink() {
        String text = "Visit https://bit.ly/abc.";
        ScanResult result = scanner.scan(text);
        Finding link = result.findings().get(0);
        assertEquals("https://bit.ly/abc", link.text());
        assertEquals(text.indexOf("https"), link.start());
    }

    @Test
    void positionsPointAtTheRightWords() {
        String text = "This is URGENT please";
        Finding finding = scanner.scan(text).findings().get(0);
        assertEquals("URGENT", text.substring(finding.start(), finding.end()));
    }

    @Test
    void repeatingTheSameWordDoesNotRaiseTheScore() {
        int once = scanner.scan("urgent").score();
        int many = scanner.scan("urgent urgent urgent urgent").score();
        assertEquals(once, many);
    }

    @Test
    void classicScamMessageIsHighRisk() {
        String text = "CRA: you owe unpaid taxes. An arrest warrant will be issued. "
            + "Pay immediately with gift cards: http://192.0.2.5/pay";
        ScanResult result = scanner.scan(text);
        assertEquals("High", result.level());
        assertTrue(result.score() <= 10);
    }

    @Test
    void findingsComeOutInTextOrder() {
        List<Finding> findings = scanner.scan("gift card ... urgent ... bitcoin").findings();
        for (int i = 1; i < findings.size(); i++) {
            assertTrue(findings.get(i - 1).start() <= findings.get(i).start());
        }
    }

    @Test
    void deliveryFeeScamIsFlagged() {
        ScanResult result = scanner.scan(
            "USPS: Your package could not be delivered due to an incomplete address. "
            + "Click here to pay a \u00a31.45 redelivery fee to update: www.upsscammm.com");
        assertTrue(has(result, "DELIVERY_SCAM"));
        assertTrue(has(result, "PAYMENT_LINK"));
        assertEquals("Medium", result.level());
    }

    @Test
    void realLookingDeliveryNoticeWithoutAskIsNotFlagged() {
        ScanResult result = scanner.scan("Your order has shipped and should arrive on Friday.");
        assertEquals(0, result.score());
    }

    @Test
    void courierLookalikeLinkIsFlagged() {
        ScanResult result = scanner.scan("Track it here: http://ups-delivery-update.com/track");
        assertTrue(has(result, "LOOKALIKE_DOMAIN"));
    }

    @Test
    void realCourierSiteIsNotALookalike() {
        ScanResult result = scanner.scan("Track it here: https://www.ups.com/track");
        assertFalse(has(result, "LOOKALIKE_DOMAIN"));
    }
}
