package scamchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class ScanServerTest {

    // Starts a real server on a free port, runs the check, then shuts it down
    private interface Check {
        void run(HttpClient client, String base) throws Exception;
    }

    private void withServer(Check check) throws Exception {
        HttpServer server = new ScanServer("*").start(0);
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            check.run(HttpClient.newHttpClient(), base);
        } finally {
            server.stop(0);
        }
    }

    private HttpResponse<String> post(HttpClient client, String base, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/scan"))
            .header("Content-Type", "text/plain; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthCheckWorks() throws Exception {
        withServer((client, base) -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/health")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"ok\"}", response.body());
        });
    }

    @Test
    void scanReturnsFindingsAsJson() throws Exception {
        withServer((client, base) -> {
            HttpResponse<String> response = post(client, base,
                "URGENT: buy gift cards and read me the verification code");
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"level\":\"High\""));
            assertTrue(response.body().contains("GIFT_CARD_OR_CRYPTO"));
            assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        });
    }

    @Test
    void messageWithQuotesAndNewLinesStillGivesValidJson() throws Exception {
        withServer((client, base) -> {
            HttpResponse<String> response = post(client, base, "He said \"urgent\"\nplease");
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\\\"urgent\\\"") || response.body().contains("\"text\":\"urgent\""));
        });
    }

    @Test
    void emptyMessageIsRejected() throws Exception {
        withServer((client, base) -> assertEquals(400, post(client, base, "   ").statusCode()));
    }

    @Test
    void tooLongMessageIsRejected() throws Exception {
        withServer((client, base) -> {
            String big = "a".repeat(ScanServer.MAX_CHARS + 1);
            assertEquals(413, post(client, base, big).statusCode());
        });
    }

    @Test
    void messageAtTheLimitIsAccepted() throws Exception {
        withServer((client, base) -> {
            String limit = "a".repeat(ScanServer.MAX_CHARS);
            assertEquals(200, post(client, base, limit).statusCode());
        });
    }

    @Test
    void getIsNotAllowedOnScan() throws Exception {
        withServer((client, base) -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/scan")).GET().build();
            assertEquals(405, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        });
    }

    @Test
    void preflightRequestGetsCorsHeaders() throws Exception {
        withServer((client, base) -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/scan"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, response.statusCode());
            assertEquals("*", response.headers().firstValue("access-control-allow-origin").orElse(""));
        });
    }
}
