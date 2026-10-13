# Scam Checker

A small Java tool that reads a message and points out common scam warning signs: pressure to act fast, gift card or crypto payments, threats about your account, requests for codes, and links that look fake.

It follows plain rules. It does not use AI, and it can't prove a message is or isn't a scam. A low score only means it found nothing it knows to look for.
## About the live demo

The page at https://pxri-hub.github.io/scam-checker/ is a **visual demo only**. GitHub Pages can host the website, but it can't run the Java server that does the actual scanning. So on the live page, the **Check message** button won't work yet.

To see the real thing working, run the Java server and the website on your own computer. The steps are under "How to run it" below.

## How it fits together

- `src/main/java/scamchecker/` is the Java side: the scanner, a small web API (`POST /scan`, `GET /health`) and tests.
- `web/` is the TypeScript website (Vite). It sends the message to the API and marks up the warning signs.

## How to run it

You need JDK 21 or newer, Maven, and Node 18 or newer.

Terminal 1, the API:

```bash
mvn test
mvn -q compile exec:java -Dexec.mainClass=scamchecker.ServerMain
```

It listens on http://localhost:8080. Set `PORT` or `ALLOWED_ORIGIN` to change that.

Terminal 2, the website:

```bash
cd web
npm install
npm test
npm run dev
```

Open the address Vite prints (usually http://localhost:5173).

To try the scanner without the website:

```bash
mvn -q compile exec:java -Dexec.mainClass=scamchecker.Main -Dexec.args="URGENT: pay with gift cards"
```

## What it looks for

| Warning sign | Example |
|---|---|
| Urgency | "act now", "final notice" |
| Gift cards, crypto, wire transfers | "buy a Google Play card" |
| Account threats | "your account has been suspended" |
| Prize claims | "you have won" |
| Government threats | "arrest warrant", "unpaid taxes" |
| Asking for secrets | "read me the verification code" |
| Odd links | shortened links, number addresses, `@` tricks |
| Lookalike websites | `paypal-secure-login.example.xyz` |

## What I learned
This website contains separate backend and frontend. The Java server does the scanning and the TypeScript page just sends it a message and shows the result. I learned they talk over HTTP on a port (8080 for the server, 5173 for the page), and that you need both running at once.

I used Maven to download the libraries and run the tests with mvn test. I also found out that Maven can skip recompiling when files look up to date. I hit that when I put new files over an old folder, and mvn clean fixed it.

My first version scored an obvious delivery-fee scam as Low risk. I had only written rules for the scams I’d thought of, so I added new rules and tests for it. That showed me how a rules-based checker has limits. It can’t catch everything, like a brand name hidden inside a longer fake website name. If I keep going, I’d host it online and try a smarter way to catch lookalike websites.
