package scamchecker;

import java.io.IOException;

// Starts the web server.
// Hosting sites usually tell us which port to use through the PORT setting.
// ALLOWED_ORIGIN can be set to your website's address so only it can use the server.
public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        String portSetting = System.getenv("PORT");
        if (portSetting != null && !portSetting.isBlank()) {
            port = Integer.parseInt(portSetting.trim());
        }

        String origin = System.getenv("ALLOWED_ORIGIN");
        if (origin == null || origin.isBlank()) {
            origin = "*";
        }

        new ScanServer(origin).start(port);
        System.out.println("Scam Checker API is running on port " + port);
    }
}
