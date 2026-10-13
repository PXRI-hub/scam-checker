package scamchecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Looks through a message for common scam warning signs.
// It only follows rules. It does not use AI, and it cannot prove a message is a scam.
public class ScamScanner {

    // One kind of red flag: its name, how many points it is worth,
    // the explanation we show, and the patterns that look for it.
    private static class Rule {
        final String type;
        final int points;
        final String why;
        final List<Pattern> patterns = new ArrayList<>();

        Rule(String type, int points, String why, String... regexes) {
            this.type = type;
            this.points = points;
            this.why = why;
            for (String regex : regexes) {
                // CASE_INSENSITIVE so "URGENT" and "urgent" both match
                patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            }
        }
    }

    // The word rules. Each kind of red flag is only counted once in the score,
    // so a message can't get a huge score by repeating the same word.
    private static final List<Rule> WORD_RULES = List.of(
        new Rule("URGENCY", 2,
            "Scammers rush you so you don't stop to think or check.",
            "\\burgent(ly)?\\b", "\\bact (now|immediately)\\b", "\\bimmediately\\b",
            "\\bwithin (24|48) hours\\b", "\\bfinal notice\\b", "\\blast warning\\b"),

        new Rule("GIFT_CARD_OR_CRYPTO", 4,
            "Gift cards, crypto and wire transfers are hard to get back. Real companies don't ask for them as payment.",
            "\\bgift cards?\\b", "\\bitunes card\\b", "\\bgoogle play card\\b", "\\bsteam card\\b",
            "\\bbitcoin\\b", "\\bcrypto(currency)?\\b", "\\bwestern union\\b", "\\bwire (me |the )?money\\b"),

        new Rule("ACCOUNT_THREAT", 3,
            "A message that says your account is in trouble is a common way to get you to click a bad link.",
            "\\b(account|card) (has been|was|is) (suspended|locked|limited|compromised|closed|blocked)\\b",
            "\\bunusual (activity|sign.?in)\\b",
            "\\bverify your (account|identity|information)\\b",
            "\\bconfirm your (account|identity|details)\\b"),

        new Rule("PRIZE", 3,
            "You can't win a contest you never entered.",
            "\\byou('ve| have) (won|been selected)\\b", "\\bclaim your (prize|reward|refund)\\b",
            "\\blottery\\b", "\\bcongratulations,? you\\b"),

        new Rule("GOVERNMENT_THREAT", 4,
            "Real government offices don't threaten arrest or demand payment by message.",
            "\\barrest warrant\\b", "\\blegal action\\b", "\\bunpaid taxes\\b",
            "\\btax (debt|fraud)\\b", "\\bpolice will\\b"),

        new Rule("SECRET_REQUEST", 4,
            "Nobody honest needs your password, PIN or a code sent to your phone.",
            "\\b(send|give|share|tell|read) (me |us )?(the |your )?(code|password|pin)\\b",
            "\\bverification code\\b", "\\bone.?time (code|passcode)\\b",
            "\\bsocial insurance number\\b"),

        new Rule("DELIVERY_SCAM", 3,
            "Fake delivery notices ask for a small fee to get you to enter your card details. Check the courier's real website or app yourself.",
            "\\b(package|parcel|delivery)\\b[^.]{0,40}\\b(could not|couldn't|unable to|failed to|cannot|can't) be delivered\\b",
            "\\bredelivery\\b", "\\bdelivery fee\\b", "\\bincomplete address\\b",
            "\\breschedule (your )?delivery\\b"),

        new Rule("PAYMENT_LINK", 2,
            "Being told to click a link to pay or confirm something is how card and login details get stolen.",
            "\\bclick (here|the link|below) to (pay|update|verify|confirm|claim|track)\\b",
            "\\bpay (a |the )?[$\u00a3\u20ac]\\s?\\d+(\\.\\d\\d)?\\b")
    );

    // Websites that shorten links. They hide where the link really goes.
    private static final Set<String> SHORTENERS = Set.of(
        "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rb.gy", "shorturl.at");

    // Well-known names and the real websites they use.
    // If a link uses the name but a different website, that's a lookalike.
    private static final Map<String, List<String>> BRANDS = new HashMap<>();
    static {
        BRANDS.put("paypal", List.of("paypal.com"));
        BRANDS.put("amazon", List.of("amazon.com", "amazon.ca"));
        BRANDS.put("cibc", List.of("cibc.com"));
        BRANDS.put("rbc", List.of("rbc.com", "rbcroyalbank.com"));
        BRANDS.put("scotiabank", List.of("scotiabank.com"));
        BRANDS.put("interac", List.of("interac.ca"));
        BRANDS.put("canadapost", List.of("canadapost-postescanada.ca"));
        BRANDS.put("netflix", List.of("netflix.com"));
        BRANDS.put("microsoft", List.of("microsoft.com"));
        BRANDS.put("ups", List.of("ups.com"));
        BRANDS.put("usps", List.of("usps.com"));
        BRANDS.put("fedex", List.of("fedex.com"));
        BRANDS.put("dhl", List.of("dhl.com"));
    }

    // Finds things that look like web links: http://..., https://... or www....
    private static final Pattern URL_PATTERN =
        Pattern.compile("\\b((https?://|www\\.)[^\\s<>\"']+)", Pattern.CASE_INSENSITIVE);

    // Points for the link rules
    private static final int SHORT_LINK_POINTS = 2;
    private static final int IP_LINK_POINTS = 3;
    private static final int ODD_LINK_POINTS = 3;
    private static final int LOOKALIKE_POINTS = 5;

    public ScanResult scan(String text) {
        List<Finding> findings = new ArrayList<>();
        Map<String, Integer> pointsByType = new HashMap<>();

        if (text == null || text.isBlank()) {
            return new ScanResult(findings, 0, "Low");
        }

        // 1. Word and phrase rules
        for (Rule rule : WORD_RULES) {
            for (Pattern pattern : rule.patterns) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    findings.add(new Finding(rule.type, matcher.start(), matcher.end(),
                        matcher.group(), rule.why));
                    pointsByType.put(rule.type, rule.points);
                }
            }
        }

        // 2. Link rules
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            String url = cleanEnd(urlMatcher.group(1));
            int start = urlMatcher.start(1);
            int end = start + url.length();
            checkLink(url, start, end, findings, pointsByType);
        }

        // Sort so the findings come out in the order they appear in the text
        findings.sort(Comparator.comparingInt(Finding::start));

        int score = 0;
        for (int points : pointsByType.values()) {
            score = score + points;
        }
        score = Math.min(score, 10);

        return new ScanResult(findings, score, levelFor(score));
    }

    // Turns a score into a simple word
    static String levelFor(int score) {
        if (score >= 6) {
            return "High";
        }
        if (score >= 3) {
            return "Medium";
        }
        return "Low";
    }

    // Takes off punctuation stuck to the end of a link, like the "." in "see www.site.com."
    private static String cleanEnd(String url) {
        while (!url.isEmpty() && ".,;:!?)]}".indexOf(url.charAt(url.length() - 1)) >= 0) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    // Checks one link for warning signs
    private void checkLink(String url, int start, int end,
                           List<Finding> findings, Map<String, Integer> pointsByType) {
        // Take off "https://" so we are left with the website part and the path
        String rest = url.replaceFirst("(?i)^https?://", "");

        // The website part ends at the first / ? or #
        int cut = rest.length();
        for (char c : new char[] {'/', '?', '#'}) {
            int position = rest.indexOf(c);
            if (position >= 0 && position < cut) {
                cut = position;
            }
        }
        String authority = rest.substring(0, cut).toLowerCase();

        // A "@" in the website part is a trick: the real site is AFTER the @
        // Example: http://paypal.com@evil.example/login goes to evil.example
        if (authority.contains("@")) {
            add(findings, pointsByType, "ODD_LINK", ODD_LINK_POINTS, url, start, end,
                "This link has an @ sign. The real website is the part after it, not the part before.");
            authority = authority.substring(authority.lastIndexOf('@') + 1);
        }

        // Take off a port number like :8080
        String host = authority.replaceFirst(":\\d+$", "");

        // "xn--" means the site name uses look-alike letters from other alphabets
        if (host.contains("xn--")) {
            add(findings, pointsByType, "ODD_LINK", ODD_LINK_POINTS, url, start, end,
                "This website name uses look-alike characters from another alphabet.");
        }

        if (SHORTENERS.contains(host)) {
            add(findings, pointsByType, "SHORT_LINK", SHORT_LINK_POINTS, url, start, end,
                "This is a shortened link, so you can't see where it really goes.");
        }

        if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            add(findings, pointsByType, "IP_LINK", IP_LINK_POINTS, url, start, end,
                "This link uses a number address instead of a normal website name. Real companies rarely do that.");
        }

        // Lookalike check: does the website name contain a famous name,
        // while the website is not that company's real one?
        // We split the host on dots and dashes: "paypal-secure-login.xyz" -> paypal, secure, login, xyz
        String[] pieces = host.split("[.\\-]");
        Set<String> pieceSet = new HashSet<>(List.of(pieces));
        for (Map.Entry<String, List<String>> brand : BRANDS.entrySet()) {
            if (pieceSet.contains(brand.getKey()) && !isRealSite(host, brand.getValue())) {
                add(findings, pointsByType, "LOOKALIKE_DOMAIN", LOOKALIKE_POINTS, url, start, end,
                    "This link uses the name \"" + brand.getKey()
                    + "\" but goes to a different website than the real company.");
            }
        }
    }

    // True if the host IS the real site or a subdomain of it (mail.paypal.com is fine)
    private static boolean isRealSite(String host, List<String> realSites) {
        for (String real : realSites) {
            if (host.equals(real) || host.endsWith("." + real)) {
                return true;
            }
        }
        return false;
    }

    private static void add(List<Finding> findings, Map<String, Integer> pointsByType,
                            String type, int points, String text, int start, int end, String why) {
        findings.add(new Finding(type, start, end, text, why));
        pointsByType.put(type, points);
    }
}
