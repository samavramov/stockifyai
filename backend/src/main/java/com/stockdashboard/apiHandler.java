package com.stockdashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class apiHandler implements HttpHandler {

    // FIX: Add member variables for dependencies
    private final String frontendUrl;
    private final databaseInteractions db;
    private final ObjectMapper mapper = new ObjectMapper(); // Create one mapper to reuse

    // List of stocks to process
    private static final String[] SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "CRM", "ORCL",
            "ADBE", "INTC", "AMD", "PYPL", "UBER", "SPOT", "ZOOM", "TWTR", "SNAP", "SQ",
            "SHOP", "ROKU", "PINS", "DOCU", "PLTR", "COIN", "HOOD", "RBLX", "U", "DDOG"
    };

    /**
     * FIX: Updated constructor to accept a databaseInteractions object.
     * This is more efficient than creating a new one for every request.
     * @param frontendUrl The allowed origin for CORS requests.
     * @param db The shared database interaction object.
     */
    public apiHandler(String frontendUrl, databaseInteractions db) {
        this.frontendUrl = frontendUrl;
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // OPTIONS request is handled first
        if ("OPTIONS".equalsIgnoreCase(method)) {
            handleOptionsRequest(exchange);
            return;
        }

        // Set CORS for all actual requests
        setCorsHeaders(exchange);

        try {
            if ("POST".equalsIgnoreCase(method)) {
                switch (path) {
                    case "/api/saveUser":
                        handleSaveUser(exchange);
                        break;
                    case "/api/followStock":
                        handleFollowStock(exchange);
                        break;
                    case "/api/unfollowStock":
                        handleUnfollowStock(exchange);
                        break;
                    default:
                        sendJsonError(exchange, 404, "Not Found");
                        break;
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                switch (path) {
                    case "/api/getUser":
                        handleGetUser(exchange);
                        break;
                    case "/api/getFollowedStocks":
                        handleGetFollowedStocks(exchange);
                        break;
                    case "/api/sentiments":
                        handleGetSentiments(exchange);
                        break;
                    default:
                        sendJsonError(exchange, 404, "Not Found");
                        break;
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        } catch (Exception e) {
            // Catch-all for any unexpected errors in handlers
            System.err.println("An unexpected error occurred while handling request: " + path);
            e.printStackTrace();
            sendJsonError(exchange, 500, "Internal Server Error");
        }
    }

    // --- Request Handling Methods ---

    private void handleGetSentiments(HttpExchange exchange) throws IOException {
        try {
            ArrayList<sentiment> sentiments = new ArrayList<>();
            for (String symbol : SYMBOLS) {
                // Now calling the updated method which can throw SQLException
                ArrayList<sentiment> lastTen = db.getLatestSentimentsByStockSymbol(symbol, 10);
                if (lastTen != null && !lastTen.isEmpty()) {
                    ArrayList<Double> lastTenValues = new ArrayList<>();
                    double sum = 0.0;
                    Double[] recent = new Double[2];
                    double percentChange = 0.0;

                    for (sentiment s : lastTen) {
                        lastTenValues.add(s.sentimentValue);
                        sum += s.sentimentValue;
                    }
                    for (int i = 0; i < lastTen.size() && i < 2; i++) {
                        recent[i] = lastTen.get(i).sentimentValue;
                    }

                    if (recent[0] != null && recent[1] != null && recent[1] != 0) {
                        percentChange = ((recent[0] - recent[1]) / Math.abs(recent[1])) * 100.0;
                    }

                    double averageSentiment = sum / lastTen.size();
                    sentiment mostRecent = lastTen.get(0);
                    mostRecent.tenDayAverage = averageSentiment;
                    mostRecent.percentChange = percentChange;
                    mostRecent.lastTen = lastTenValues;
                    sentiments.add(mostRecent);
                }
            }

            // FIX: Use ObjectMapper for robust JSON conversion
            String json = mapper.writeValueAsString(sentiments);
            sendJsonResponse(exchange, 200, json);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetSentiments:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Database error while fetching sentiments.");
        } catch (JsonProcessingException e) {
            System.err.println("JSON processing error in handleGetSentiments:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Server error while formatting data.");
        }
    }

    public void handleGetUser(HttpExchange exchange) throws IOException {
        String email = getQueryParam(exchange.getRequestURI().getQuery(), "email");
        if (email == null || email.isEmpty()) {
            sendJsonError(exchange, 400, "Missing email parameter");
            return;
        }

        try {
            User user = db.getUserByEmail(email);
            if (user == null) {
                sendJsonError(exchange, 404, "User not found");
                return;
            }
            String responseJson = mapper.writeValueAsString(user);
            sendJsonResponse(exchange, 200, responseJson);
        } catch (SQLException e) {
            System.err.println("Database error in handleGetUser:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Database error while fetching user.");
        }
    }

    public void handleSaveUser(HttpExchange exchange) throws IOException {
        // This method was already fairly robust.
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(requestBody);
            String email = root.path("email").asText();
            String name = root.path("name").asText();
            String picture = root.path("picture").asText();

            boolean success = db.saveUser(email, name, picture);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
            } else {
                sendJsonError(exchange, 500, "Failed to save user.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleSaveUser:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Server error during save user.");
        }
    }

    private void handleFollowStock(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(body);
            String email = root.path("email").asText();
            String stock = root.path("stockSymbol").asText();

            // FIX: Catch the SQLException that can now be thrown
            boolean success = db.followStock(email, stock);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"status\":\"followed\"}");
            } else {
                sendJsonError(exchange, 500, "Could not follow stock.");
            }
        } catch (SQLException e) {
            System.err.println("Database error in handleFollowStock:");
            e.printStackTrace();
            // Check for foreign key violation specifically
            if (e.getErrorCode() == 2291) {
                 sendJsonError(exchange, 400, "Cannot follow stock, user does not exist.");
            } else {
                 sendJsonError(exchange, 500, "Database error while following stock.");
            }
        }
    }

    private void handleUnfollowStock(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(body);
            String email = root.path("email").asText();
            String stock = root.path("stockSymbol").asText();
            
            // FIX: Catch the SQLException that can now be thrown
            boolean success = db.unfollowStock(email, stock);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"status\":\"unfollowed\"}");
            } else {
                // This case might happen if the stock was already unfollowed
                sendJsonResponse(exchange, 200, "{\"status\":\"already unfollowed or not found\"}");
            }
        } catch (SQLException e) {
            System.err.println("Database error in handleUnfollowStock:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Database error while unfollowing stock.");
        }
    }

    private void handleGetFollowedStocks(HttpExchange exchange) throws IOException {
        String email = getQueryParam(exchange.getRequestURI().getQuery(), "email");
        if (email == null || email.isEmpty()) {
            sendJsonError(exchange, 400, "Missing email parameter");
            return;
        }

        try {
            // FIX: Catch the SQLException that can now be thrown
            ArrayList<String> followedStocks = db.getFollowedStocks(email);
            String response = mapper.writeValueAsString(followedStocks);
            sendJsonResponse(exchange, 200, response);
        } catch (SQLException e) {
            System.err.println("Database error in handleGetFollowedStocks:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Database error while getting followed stocks.");
        }
    }

    // --- Utility Methods ---

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }
    
    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] keyVal = pair.split("=", 2);
            if (keyVal.length > 0 && keyVal[0].equals(param)) {
                return keyVal.length > 1 ? URLDecoder.decode(keyVal[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private void sendJsonError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String error = String.format("{\"error\":\"%s\"}", message.replace("\"", "\\\""));
        // No need to set CORS headers here, as the main handle() method does it now.
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, error.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(error.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", this.frontendUrl);
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
    }
}