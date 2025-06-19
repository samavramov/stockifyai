package com.stockdashboard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import io.github.cdimascio.dotenv.Dotenv; // Import the Dotenv library
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Map<String, JsonObject> activeSessions = new ConcurrentHashMap<>();
    private static final String GOOGLE_CLIENT_ID;
    private static final String GOOGLE_CLIENT_SECRET;
    private static final String BACKEND_URL;
    private static final String FRONTEND_URL; // Declare FRONTEND_URL

    static {
        Dotenv dotenv = null;
        String clientId = null;
        String clientSecret = null;
        String backendURL = null;
        String frontendURL = null; // Initialize here

        try {
            dotenv = Dotenv.configure()
                    .directory("backend")
                    .load();
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
            System.err.println("Please ensure backend/.env exists and is properly formatted.");
            // Fallback to system environment variables if .env is not found
            clientId = System.getenv("GOOGLE_CLIENT_ID");
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            backendURL = System.getenv("BACKEND_URL");
            frontendURL = System.getenv("FRONTEND_URL");
        }

        if (dotenv != null) {
            clientId = dotenv.get("GOOGLE_CLIENT_ID");
            clientSecret = dotenv.get("GOOGLE_CLIENT_SECRET");
            backendURL = dotenv.get("BACKEND_URL");
            frontendURL = dotenv.get("FRONTEND_URL"); // Load from .env
        }

        GOOGLE_CLIENT_ID = clientId;
        GOOGLE_CLIENT_SECRET = clientSecret;
        BACKEND_URL = backendURL;
        FRONTEND_URL = frontendURL; // Assign the loaded value

        // Critical check for all required environment variables
        if (GOOGLE_CLIENT_ID == null || GOOGLE_CLIENT_SECRET == null || BACKEND_URL == null || FRONTEND_URL == null) {
            System.err.println(
                    "CRITICAL ERROR: One or more environment variables are missing (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, BACKEND_URL, FRONTEND_URL). Please set them in your .env file or system environment. Exiting.");
            System.exit(1);
        }

        System.out.println("Google Client ID loaded successfully.");
        System.out.println("Backend URL loaded successfully: " + BACKEND_URL);
        System.out.println("Frontend URL loaded successfully: " + FRONTEND_URL); // Confirm load
    }

    public static void main(String[] args) throws IOException {
        int port = 8001; // This port is for the *backend server*
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        server.setExecutor(threadPool);
        apiHandler apiHandler = new apiHandler(FRONTEND_URL);
        server.createContext("/api/sentiments", apiHandler);
        server.createContext("/api/saveUser", apiHandler);
        server.createContext("/api/getUser", apiHandler);
        server.createContext("/api/followStock", apiHandler);
        server.createContext("/api/getFollowedStocks", apiHandler);
        server.createContext("/api/unfollowStock", apiHandler);

        server.createContext("/auth/google", exchange -> {
            String state = UUID.randomUUID().toString();
            String redirectUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + GOOGLE_CLIENT_ID +
                    "&redirect_uri=" + BACKEND_URL + "/auth/google/callback" +
                    "&response_type=code" +
                    "&scope=profile%20email" +
                    "&access_type=offline" +
                    "&prompt=select_account" +
                    "&state=" + state;

            exchange.getResponseHeaders().add("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/auth/google/callback", exchange -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    // *** Use FRONTEND_URL for CORS headers ***
                    addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET, POST, OPTIONS",
                            "Content-Type, Authorization");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            code = param.split("=")[1];
                            break;
                        }
                    }
                }
                if (code == null) {
                    System.err.println("Error: Authorization code missing from callback.");
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                    return;
                }
                String tokenRequestBody = "code=" + code +
                        "&client_id=" + GOOGLE_CLIENT_ID +
                        "&client_secret=" + GOOGLE_CLIENT_SECRET +
                        "&redirect_uri=" + BACKEND_URL + "/auth/google/callback" +
                        "&grant_type=authorization_code";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody))
                        .build();

                HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
                JsonObject tokenJson = JsonParser.parseString(tokenResponse.body()).getAsJsonObject();

                if (!tokenJson.has("access_token")) {
                    System.err
                            .println("Error: Access token not received from Google. Response: " + tokenResponse.body());
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }

                String accessToken = tokenJson.get("access_token").getAsString();

                HttpRequest userInfoRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                        .header("Authorization", "Bearer " + accessToken)
                        .build();

                HttpResponse<String> userInfoResponse = client.send(userInfoRequest,
                        HttpResponse.BodyHandlers.ofString());
                JsonObject userJson = JsonParser.parseString(userInfoResponse.body()).getAsJsonObject();

                String email = userJson.has("email") ? userJson.get("email").getAsString() : "unknown@example.com";
                String name = userJson.has("name") ? userJson.get("name").getAsString() : "Unknown User";
                String picture = userJson.has("picture") ? userJson.get("picture").getAsString() : "";

                String sessionToken = UUID.randomUUID().toString();
                JsonObject sessionUser = new JsonObject();
                sessionUser.addProperty("email", email);
                sessionUser.addProperty("name", name);
                sessionUser.addProperty("picture", picture);
                activeSessions.put(sessionToken, sessionUser);

                threadPool.submit(() -> {
                    try {
                        JsonObject saveUserJson = new JsonObject();
                        saveUserJson.addProperty("email", email);
                        saveUserJson.addProperty("name", name);
                        saveUserJson.addProperty("picture", picture);

                        HttpRequest saveUserRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BACKEND_URL + "/api/saveUser"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(saveUserJson.toString()))
                                .build();
                        client.send(saveUserRequest, HttpResponse.BodyHandlers.ofString());
                    } catch (Exception e) {
                        System.err.println("Error saving user to backend: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                exchange.getResponseHeaders().add("Set-Cookie",
                        "sessionId=" + sessionToken + "; Path=/; HttpOnly; SameSite=Lax");
                // *** Use FRONTEND_URL for the final redirect ***
                exchange.getResponseHeaders().add("Location", FRONTEND_URL + "/home");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            } catch (Exception e) {
                System.err.println("Error in Google OAuth callback: " + e.getMessage());
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            }
        });

        server.createContext("/logout", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                // *** Use FRONTEND_URL for CORS headers ***
                addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET, POST, OPTIONS",
                        "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            // *** Use FRONTEND_URL for CORS headers ***
            addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET", "Content-Type");
            String sessionId = getCookieValue(exchange.getRequestHeaders(), "sessionId");
            if (sessionId != null) {
                activeSessions.remove(sessionId);
                exchange.getResponseHeaders().add("Set-Cookie",
                        "sessionId=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax");
                System.out.println("Session " + sessionId + " invalidated.");
            } else {
                System.out.println("Logout request received, but no sessionId found.");
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.createContext("/me", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                // *** Use FRONTEND_URL for CORS headers ***
                addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET, POST, OPTIONS",
                        "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            // *** Use FRONTEND_URL for CORS headers ***
            addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET", "Content-Type, Authorization");
            String sessionId = getCookieValue(exchange.getRequestHeaders(), "sessionId");
            if (sessionId != null && activeSessions.containsKey(sessionId)) {
                JsonObject user = activeSessions.get(sessionId);
                String response = "{\"user\":" + user.toString() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(401, -1);
            }
            exchange.close();
        });

        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                exchange.close();
                return;
            }

            try {
                // Define the root directory for your built frontend files
                final String root = "website/frontend/dist";
                URI uri = exchange.getRequestURI();
                String path = uri.getPath().equals("/") ? "/index.html" : uri.getPath();

                // Construct the full path to the requested file
                String filePath = root + path;

                // Basic security check to prevent path traversal attacks
                if (filePath.contains("..")) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    exchange.close();
                    return;
                }

                byte[] bytes = Files.readAllBytes(Paths.get(filePath));

                // Determine the correct Content-Type based on file extension
                Headers headers = exchange.getResponseHeaders();
                if (filePath.endsWith(".html")) {
                    headers.set("Content-Type", "text/html; charset=UTF-8");
                } else if (filePath.endsWith(".js")) {
                    headers.set("Content-Type", "application/javascript");
                } else if (filePath.endsWith(".css")) {
                    headers.set("Content-Type", "text/css");
                } else if (filePath.endsWith(".png")) {
                    headers.set("Content-Type", "image/png");
                } // Add more types as needed (e.g., .jpg, .svg, .ico)

                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                // This will catch file not found errors
                System.err.println("Error serving static file: " + e.getMessage());
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Server started on port " + port + ". Backend is configured at: " + BACKEND_URL);
        databaseInteractions db = new databaseInteractions();
        System.out.println("Database connection test: " + (db.testConnection() ? "SUCCESS" : "FAILURE"));
    }

    private static void addCorsHeaders(Headers headers, String origin, String methods, String allowedHeaders) {
        headers.add("Access-Control-Allow-Origin", origin);
        headers.add("Access-Control-Allow-Methods", methods);
        headers.add("Access-Control-Allow-Headers", allowedHeaders);
        headers.add("Access-Control-Allow-Credentials", "true");
    }

    private static String getCookieValue(Headers headers, String cookieName) {
        String cookieHeader = headers.getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split("; ");
            for (String cookie : cookies) {
                if (cookie.startsWith(cookieName + "=")) {
                    return cookie.substring(cookieName.length() + 1);
                }
            }
        }
        return null;
    }
}