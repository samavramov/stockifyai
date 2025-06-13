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

    // --- Session Management (In-memory for demonstration) ---
    private static final Map<String, JsonObject> activeSessions = new ConcurrentHashMap<>();

    // Declare static final variables for client ID and secret
    private static final String GOOGLE_CLIENT_ID;
    private static final String GOOGLE_CLIENT_SECRET;

    // Static block to load environment variables when the class is loaded
    static {
        // Initialize Dotenv. It will look for a .env file in the current directory
        // or specified paths. Here, it will look in the backend directory where the .env file is.
        Dotenv dotenv = null;
        String clientId = null;
        String clientSecret = null;
        
        try {
            // Attempt to load .env from the backend directory
            dotenv = Dotenv.configure()
                           .directory("backend") // Specify the directory where your .env file is
                           .load();
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
            System.err.println("Please ensure backend/.env exists and is properly formatted.");
            // Fallback to system environment variables if .env not found/loaded
            // This is useful for deployment environments where variables are set directly
            clientId = System.getenv("GOOGLE_CLIENT_ID");
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            if (clientId == null || clientSecret == null) {
                System.err.println("CRITICAL ERROR: Google Client ID and/or Secret are not set in .env or as system environment variables. Exiting.");
                System.exit(1); // Exit if critical variables are missing
            }
        }

        if (dotenv != null) {
            // Retrieve variables from .env
            clientId = dotenv.get("GOOGLE_CLIENT_ID");
            clientSecret = dotenv.get("GOOGLE_CLIENT_SECRET");
        }
        
        // Assign the final fields only once
        GOOGLE_CLIENT_ID = clientId;
        GOOGLE_CLIENT_SECRET = clientSecret;

        // Final check to ensure variables are loaded, regardless of method
        if (GOOGLE_CLIENT_ID == null || GOOGLE_CLIENT_SECRET == null) {
            System.err.println("CRITICAL ERROR: Google Client ID and/or Secret environment variables are missing. Please set them in your .env file or system environment. Exiting.");
            System.exit(1); // Exit if critical variables are missing
        } else {
            System.out.println("Google Client ID loaded successfully.");
            // For security, do not print the secret itself
        }
    }


    public static void main(String[] args) throws IOException {
        int port = 8001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        server.setExecutor(threadPool);

        apiHandler apiHandler = new apiHandler();

        // Register API endpoints
        server.createContext("/api/sentiments", apiHandler);
        server.createContext("/api/saveUser", apiHandler);
        server.createContext("/api/getUser", apiHandler);
        server.createContext("/api/followStock", apiHandler);
        server.createContext("/api/getFollowedStocks", apiHandler);
        server.createContext("/api/unfollowStock", apiHandler);

        // Google OAuth redirect
        server.createContext("/auth/google", exchange -> {
            String state = UUID.randomUUID().toString();
            String redirectUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + GOOGLE_CLIENT_ID + // Use the loaded ID
                    "&redirect_uri=http://localhost:" + port + "/auth/google/callback" +
                    "&response_type=code" +
                    "&scope=profile%20email" +
                    "&access_type=offline" +
                    "&prompt=select_account" +
                    "&state=" + state;

            exchange.getResponseHeaders().add("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        // Google OAuth callback
        server.createContext("/auth/google/callback", exchange -> {
            try {
                // CORS Preflight handling
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    addCorsHeaders(exchange.getResponseHeaders(), "http://localhost:3000", "GET, POST, OPTIONS", "Content-Type, Authorization");
                    exchange.sendResponseHeaders(204, -1); // No Content for OPTIONS
                    exchange.close();
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                String code = null;

                if (query != null) { // Add null check for query
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
                        "&client_id=" + GOOGLE_CLIENT_ID + // Use the loaded ID
                        "&client_secret=" + GOOGLE_CLIENT_SECRET + // Use the loaded secret
                        "&redirect_uri=http://localhost:" + port + "/auth/google/callback" +
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
                    System.err.println("Error: Access token not received from Google. Response: " + tokenResponse.body());
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

                // Create a session for the user
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
                                .uri(URI.create("http://localhost:" + port + "/api/saveUser"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(saveUserJson.toString()))
                                .build();
                        client.send(saveUserRequest, HttpResponse.BodyHandlers.ofString());
                    } catch (Exception e) {
                        System.err.println("Error saving user to backend: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                exchange.getResponseHeaders().add("Set-Cookie", "sessionId=" + sessionToken + "; Path=/; HttpOnly; SameSite=Lax");

                exchange.getResponseHeaders().add("Location", "http://localhost:3000/home");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();

            } catch (Exception e) {
                System.err.println("Error in Google OAuth callback: " + e.getMessage());
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            }
        });

        // --- NEW: /logout endpoint ---
        server.createContext("/logout", exchange -> {
            // CORS Preflight handling
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange.getResponseHeaders(), "http://localhost:3000", "GET, POST, OPTIONS", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            addCorsHeaders(exchange.getResponseHeaders(), "http://localhost:3000", "GET", "Content-Type"); // GET method for logout
            String sessionId = getCookieValue(exchange.getRequestHeaders(), "sessionId");

            if (sessionId != null) {
                activeSessions.remove(sessionId); // Invalidate session
                // Clear the cookie on the client side by setting an expired/empty one
                exchange.getResponseHeaders().add("Set-Cookie", "sessionId=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax");
                System.out.println("Session " + sessionId + " invalidated.");
            } else {
                System.out.println("Logout request received, but no sessionId found.");
            }

            // Always respond with 200 OK, even if no session was found to invalidate,
            // to indicate the logout attempt was processed.
            exchange.sendResponseHeaders(200, -1); // No content
            exchange.close();
        });


        // --- /me endpoint for session validation ---
        server.createContext("/me", exchange -> {
            // CORS Preflight handling
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange.getResponseHeaders(), "http://localhost:3000", "GET, POST, OPTIONS", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1); // No Content for OPTIONS
                exchange.close();
                return;
            }

            // Always add CORS headers for actual requests as well
            addCorsHeaders(exchange.getResponseHeaders(), "http://localhost:3000", "GET", "Content-Type, Authorization");

            // Extract session ID from cookie
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
                // Not authenticated
                exchange.sendResponseHeaders(401, -1); // Send 401 Unauthorized
            }
            exchange.close();
        });

        // Serve index.html from /static
        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            try {
                // Adjust path to correctly reference 'static' relative to the project root
                // Assuming 'static' is a folder at the same level as 'backend' and 'website'
                // If it's inside website/frontend/dist or similar, adjust the path
                byte[] bytes = Files.readAllBytes(Paths.get("website/frontend/index.html")); // Corrected path based on previous screenshots
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                System.err.println("Error serving index.html: " + e.getMessage());
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });

        server.start();
        System.out.println("Server started: http://localhost:" + port);

        databaseInteractions db = new databaseInteractions();
        System.out.println("Database connection test: " + (db.testConnection() ? "SUCCESS" : "FAILURE"));
    }

    /**
     * Helper method to add CORS headers to a response.
     */
    private static void addCorsHeaders(Headers headers, String origin, String methods, String allowedHeaders) {
        headers.add("Access-Control-Allow-Origin", origin);
        headers.add("Access-Control-Allow-Methods", methods);
        headers.add("Access-Control-Allow-Headers", allowedHeaders);
        headers.add("Access-Control-Allow-Credentials", "true"); // Required for cookies
    }

    /**
     * Helper method to extract a cookie value from request headers.
     */
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
