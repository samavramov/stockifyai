package com.stockdashboard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import io.github.cdimascio.dotenv.Dotenv;
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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList; // Added for sample data list
import java.util.Date; // Added for timestamp
import java.util.LinkedHashMap;
import java.util.List; // Added for sample data list

public class Server {
    private static final Map<String, JsonObject> activeSessions = new ConcurrentHashMap<>();
    private static final String GOOGLE_CLIENT_ID;
    private static final String GOOGLE_CLIENT_SECRET;
    private static final String BACKEND_URL;
    private static final String FRONTEND_URL;

    static {
        Dotenv dotenv = null;
        String clientId = null;
        String clientSecret = null;
        String backendURL = null;
        String frontendURL = null;

        try {
            dotenv = Dotenv.configure().directory("backend").load();
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
            System.err.println("Falling back to system environment variables.");
            clientId = System.getenv("GOOGLE_CLIENT_ID");
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            backendURL = System.getenv("BACKEND_URL");
            frontendURL = System.getenv("FRONTEND_URL");
        }

        if (dotenv != null) {
            clientId = dotenv.get("GOOGLE_CLIENT_ID");
            clientSecret = dotenv.get("GOOGLE_CLIENT_SECRET");
            backendURL = dotenv.get("BACKEND_URL");
            frontendURL = dotenv.get("FRONTEND_URL");
            System.out.println("Loaded environment variables from .env file.");
        }

        GOOGLE_CLIENT_ID = clientId;
        GOOGLE_CLIENT_SECRET = clientSecret;
        BACKEND_URL = backendURL;
        FRONTEND_URL = frontendURL;

        if (GOOGLE_CLIENT_ID == null || GOOGLE_CLIENT_SECRET == null || BACKEND_URL == null || FRONTEND_URL == null) {
            System.err.println(
                    "CRITICAL ERROR: One or more environment variables are missing. Exiting.");
            System.exit(1);
        }

        System.out.println("Google Client ID loaded successfully.");
        System.out.println("Backend URL loaded successfully: " + BACKEND_URL);
        System.out.println("Frontend URL loaded successfully: " + FRONTEND_URL);
    }

    // In Server.java

    public static void populateDataRand(databaseInteractions db, boolean pop) {
        if (pop) {
            System.out.println("Populating database with initial randomized stock data...");
            try {
                // --- The Robust Fix: Use a data structure that pairs the ticker and name ---
                // This Map ensures the ticker and name can never be out of sync.
                Map<String, String> companyData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion
                                                                         // order
                companyData.put("AAPL", "Apple Inc.");
                companyData.put("MSFT", "Microsoft Corporation");
                companyData.put("GOOGL", "Alphabet Inc.");
                companyData.put("AMZN", "Amazon.com Inc.");
                companyData.put("TSLA", "Tesla Inc.");
                companyData.put("META", "Meta Platforms Inc.");
                companyData.put("NVDA", "NVIDIA Corporation");
                companyData.put("NFLX", "Netflix Inc.");
                companyData.put("CRM", "Salesforce, Inc.");
                companyData.put("ORCL", "Oracle Corporation");
                companyData.put("ADBE", "Adobe Inc.");
                companyData.put("INTC", "Intel Corporation");
                companyData.put("AMD", "Advanced Micro Devices");
                companyData.put("PYPL", "PayPal Holdings Inc.");
                companyData.put("UBER", "Uber Technologies Inc.");
                companyData.put("SPOT", "Spotify Technology SA");
                companyData.put("ZOOM", "Zoom Video Communications");
                companyData.put("TWTR", "X Corp. (Twitter)");
                companyData.put("SNAP", "Snap Inc.");
                companyData.put("SQ", "Block, Inc.");
                companyData.put("SHOP", "Shopify Inc.");
                companyData.put("ROKU", "Roku, Inc.");
                companyData.put("PINS", "Pinterest, Inc.");
                companyData.put("DOCU", "DocuSign, Inc.");
                companyData.put("PLTR", "Palantir Technologies");
                companyData.put("COIN", "Coinbase Global Inc.");
                companyData.put("HOOD", "Robinhood Markets, Inc.");
                companyData.put("RBLX", "Roblox Corporation");
                companyData.put("U", "Unity Software Inc.");
                companyData.put("DDOG", "Datadog, Inc.");

                // Convert the Map keys to an array for easy random selection
                String[] tickers = companyData.keySet().toArray(new String[0]);

                String[] positiveSummaries = {
                        "Strong quarterly earnings report exceeded expectations, driving positive sentiment.",
                        "New product launch met with widespread consumer praise and critical acclaim.",
                        "Strategic partnership announced, expected to open up new revenue streams.",
                        "LLM analysis indicates a bullish outlook due to strong market positioning and innovation.",
                        "Upgraded by top analysts following a period of sustained growth."
                };
                String[] negativeSummaries = {
                        "Missed earnings targets, leading to a drop in investor confidence.",
                        "Facing increased regulatory scrutiny which is causing market uncertainty.",
                        "Supply chain disruptions are expected to impact production and sales.",
                        "LLM analysis reveals bearish sentiment amid growing competition and market headwinds.",
                        "A recent product recall has negatively impacted the company's public image."
                };

                Random random = new Random();
                List<sentiment> sampleStocks = new ArrayList<>();
                for (int c = 0; c < 11; c++) {
                    // Generate sentiment entries for all 30 stocks
                    for (int i = 0; i < tickers.length; i++) {
                        // No need for a random index anymore if we want to add all of them
                        String ticker = tickers[i];
                        String name = companyData.get(ticker); // Get the name from the Map using the ticker

                        // Generate a random sentiment score between -1.0 and 1.0
                        double sentimentScore = -1.0 + (2.0 * random.nextDouble());
                        String summary;

                        // Choose a summary based on the sentiment score
                        if (sentimentScore > 0.1) {
                            summary = positiveSummaries[random.nextInt(positiveSummaries.length)];
                        } else if (sentimentScore < -0.1) {
                            summary = negativeSummaries[random.nextInt(negativeSummaries.length)];
                        } else {
                            summary = "Market sentiment is neutral, with no significant catalysts observed.";
                        }

                        sampleStocks.add(new sentiment(
                                ticker,
                                name,
                                sentimentScore,
                                new Date(), // A new date for each entry
                                "https://www.example.com/" + ticker.toLowerCase() + "-news1",
                                "https://www.example.com/" + ticker.toLowerCase() + "-news2",
                                "https://www.example.com/" + ticker.toLowerCase() + "-news3",
                                summary));
                        Thread.sleep(10); // Sleep to ensure unique timestamps and avoid the ORA-00001 error
                    }

                    // Loop through the list and add each generated stock to the database
                    for (sentiment stock : sampleStocks) {
                        System.out.println("Adding sentiment for: " + stock.stockSymbol);
                        db.addSentiment(stock);
                    }
                }

                System.out.println("Database population complete.");

            } catch (Exception e) {
                System.err.println("An error occurred during database population: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Skipping database population.");
        }
    }

    public static void main(String[] args) throws IOException {
        // --- 1. SETUP DATABASE CONNECTION FIRST ---
        // Create a single, shared database interactions object.
        final databaseInteractions db = new databaseInteractions();

        // Test the connection. If it fails, exit the application.
        System.out.println("Connecting to the database and initializing schema...");
        if (!db.testConnection()) {
            System.err.println("CRITICAL: Database connection failed. The server will not start.");
            System.exit(1);
        }
        System.out.println("Database connection successful.");

        // Initialize the schema (creates tables if they don't exist).
        db.initializeSchema();
        System.out.println("Schema initialization complete.");

        // --- START: MODIFIED SECTION ---

        // --- 3. POPULATE DATABASE WITH SAMPLE DATA ---
        System.out.println("Populating database with initial stock data...");
        try {
            populateDataRand(db, false);
            System.out.println("Database population complete.");

        } catch (Exception e) {
            System.err.println("An error occurred during database population: " + e.getMessage());
            e.printStackTrace();
        }

        // --- END: MODIFIED SECTION ---

        // --- 2. CONFIGURE AND START THE HTTP SERVER ---
        int port = 8001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        server.setExecutor(threadPool);

        // --- FIX: Pass the shared 'db' object to the apiHandler's constructor ---
        // This makes the handler more efficient by sharing the single DB connection
        // pool.
        final apiHandler apiHandler = new apiHandler(FRONTEND_URL, db);

        server.createContext("/api/sentiments", apiHandler);
        server.createContext("/api/sentiments/bulk", apiHandler);
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
                        System.out.println("Attempting to save user to DB: " + email);
                        boolean success = db.saveUser(email, name, picture);
                        if (success) {
                            System.out.println("Successfully saved/merged user in DB: " + email);
                        } else {
                            System.err.println("Failed to save/merge user in DB: " + email);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception while saving user to backend: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                exchange.getResponseHeaders().add("Set-Cookie",
                        "sessionId=" + sessionToken + "; Path=/; HttpOnly; SameSite=Lax");
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
                addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET, POST, OPTIONS",
                        "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
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
                addCorsHeaders(exchange.getResponseHeaders(), FRONTEND_URL, "GET, POST, OPTIONS",
                        "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
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
                final String root = "website/frontend/dist";
                URI uri = exchange.getRequestURI();
                String path = uri.getPath().equals("/") ? "/index.html" : uri.getPath();

                String filePath = root + path;

                if (filePath.contains("..")) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    exchange.close();
                    return;
                }

                byte[] bytes = Files.readAllBytes(Paths.get(filePath));

                Headers headers = exchange.getResponseHeaders();
                if (filePath.endsWith(".html")) {
                    headers.set("Content-Type", "text/html; charset=UTF-8");
                } else if (filePath.endsWith(".js")) {
                    headers.set("Content-Type", "application/javascript");
                } else if (filePath.endsWith(".css")) {
                    headers.set("Content-Type", "text/css");
                } else if (filePath.endsWith(".png")) {
                    headers.set("Content-Type", "image/png");
                }

                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
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
        System.out.println("Server started on port " + port);
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