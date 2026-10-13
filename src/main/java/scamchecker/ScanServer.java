package scamchecker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

// A small web server around the scanner. It uses the HTTP server that comes with Java,
// so there is nothing extra to install.
//
//   POST /scan    body = the message as plain text   ->  JSON with the findings
//   GET  /health  ->  {"status":"ok"}  (handy for checking the server is awake)
//
// The server does not save or print the messages it receives.
public class ScanServer {

    // Longest message we accept, in characters
    static final int MAX_CHARS = 5000;
    // A character can take up to 4 bytes, so this is the most bytes we will read
    private static final int MAX_BYTES = MAX_CHARS * 4;

    private final ScamScanner scanner = new ScamScanner();
    private final String allowedOrigin;

    // allowedOrigin = which website is allowed to call this server ("*" means any site)
    public ScanServer(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    // Starts listening. Use port 0 to let the computer pick a free port (the tests do this).
    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/scan", this::handleScan);
        server.createContext("/health", this::handleHealth);
        // a few worker threads; daemon threads so they never stop the program from closing
        server.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        return server;
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        try {
            send(exchange, 200, "{\"status\":\"ok\"}");
        } finally {
            exchange.close();
        }
    }

    private void handleScan(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            // Browsers send an OPTIONS "can I call you?" request first when the page is on another site
            if (method.equals("OPTIONS")) {
                send(exchange, 204, null);
                return;
            }
            if (!method.equals("POST")) {
                send(exchange, 405, Json.error("Please use POST."));
                return;
            }

            // Read at most MAX_BYTES + 1 bytes. If we get the extra byte, the message is too big.
            byte[] body = exchange.getRequestBody().readNBytes(MAX_BYTES + 1);
            if (body.length > MAX_BYTES) {
                send(exchange, 413, Json.error("That message is too long."));
                return;
            }

            String text = new String(body, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                send(exchange, 400, Json.error("Please paste a message to check."));
                return;
            }
            if (text.length() > MAX_CHARS) {
                send(exchange, 413, Json.error("That message is too long. The limit is " + MAX_CHARS + " characters."));
                return;
            }

            send(exchange, 200, Json.toJson(scanner.scan(text)));
        } catch (RuntimeException problem) {
            send(exchange, 500, Json.error("Something went wrong on the server."));
        } finally {
            exchange.close();
        }
    }

    // Sends a reply. json == null means "no body" (used for status 204).
    private void send(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        if (json == null) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
