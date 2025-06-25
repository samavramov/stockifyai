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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class apiHandler implements HttpHandler {

    private final String frontendUrl;
    private final databaseInteractions db;
    private final ObjectMapper mapper = new ObjectMapper();

    // This list is used for the "All Stocks" view on the homepage
    private static final List<String> SYMBOLS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "CRM", "ORCL",
            "ADBE", "INTC", "AMD", "PYPL", "UBER", "SPOT", "ZOOM", "TWTR", "SNAP", "SQ",
            "SHOP", "ROKU", "PINS", "DOCU", "PLTR", "COIN", "HOOD", "RBLX", "U", "DDOG"
    );

    public apiHandler(String frontendUrl, databaseInteractions db) {
        this.frontendUrl = frontendUrl;
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            handleOptionsRequest(exchange);
            return;
        }

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
                    case "/api/sentiments/bulk": // <-- NEWLY ADDED ENDPOINT
                        handleGetBulkSentiments(exchange);
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
            System.err.println("An unexpected error occurred while handling request: " + path);
            e.printStackTrace();
            sendJsonError(exchange, 500, "Internal Server Error");
        }
    }

    /**
     * Handles the new POST /api/sentiments/bulk endpoint.
     * Expects a JSON body like: {"symbols": ["AAPL", "TSLA"], "limit": 10}
     */
    private void handleGetBulkSentiments(HttpExchange exchange) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(requestBody);

            List<String> symbols = new ArrayList<>();
            if (root.has("symbols") && root.get("symbols").isArray()) {
                for (final JsonNode symbolNode : root.get("symbols")) {
                    symbols.add(symbolNode.asText());
                }
            }

            if (symbols.isEmpty()) {
                sendJsonError(exchange, 400, "Request body must contain a 'symbols' array.");
                return;
            }
            
            int limit = root.has("limit") ? root.get("limit").asInt() : 10;

            // Make one efficient call to the database
            ArrayList<sentiment> rawSentiments = db.getLatestSentimentsForSymbols(symbols, limit);

            // Process the data (calculate averages, etc.) by grouping results
            ArrayList<sentiment> processedSentiments = processRawSentiments(rawSentiments);

            String json = mapper.writeValueAsString(processedSentiments);
            sendJsonResponse(exchange, 200, json);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetBulkSentiments:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Database error while fetching bulk sentiments.");
        } catch (Exception e) {
            System.err.println("Error processing bulk sentiments request:");
            e.printStackTrace();
            sendJsonError(exchange, 500, "Server error during bulk sentiment processing.");
        }
    }


    /**
     * Handles GET /api/sentiments. This is now optimized for the "all stocks" case.
     */
    private void handleGetSentiments(HttpExchange exchange) throws IOException {
        String symbolParam = getQueryParam(exchange.getRequestURI().getQuery(), "symbol");
        String limitParam = getQueryParam(exchange.getRequestURI().getQuery(), "limit");
        int limit = (limitParam != null) ? Integer.parseInt(limitParam) : 10;

        try {
            if (symbolParam != null && !symbolParam.isEmpty()) {
                // --- Case 1: Request for a single stock's history (for detail page chart) ---
                ArrayList<sentiment> sentiments = db.getLatestSentimentsByStockSymbol(symbolParam, limit);
                
                // The frontend needs the calculated values on the most recent entry
                if (!sentiments.isEmpty()) {
                    processSingleStockHistory(sentiments);
                }
                
                String json = mapper.writeValueAsString(sentiments);
                sendJsonResponse(exchange, 200, json);

            } else {
                // --- Case 2: Request for all stocks (for main dashboard) ---
                // This now uses ONE database call instead of a loop of 30 calls.
                ArrayList<sentiment> rawSentiments = db.getLatestSentimentsForSymbols(SYMBOLS, 10);
                
                // Process the raw data (group by symbol, calculate averages, etc.)
                ArrayList<sentiment> processedSentiments = processRawSentiments(rawSentiments);
                
                String json = mapper.writeValueAsString(processedSentiments);
                sendJsonResponse(exchange, 200, json);
            }
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

    /**
     * A helper method to process a flat list of sentiment data, group it by symbol,
     * and calculate aggregate values for the most recent entry of each symbol.
     * @param rawSentiments A flat list of sentiment objects from the database.
     * @return A list containing only the most recent sentiment object for each stock, augmented with calculated data.
     */
    private ArrayList<sentiment> processRawSentiments(List<sentiment> rawSentiments) {
        // Group the flat list of sentiments by their stock symbol
        Map<String, List<sentiment>> groupedBySymbol = rawSentiments.stream()
            .collect(Collectors.groupingBy(s -> s.stockSymbol));

        ArrayList<sentiment> processedSentiments = new ArrayList<>();
        for (Map.Entry<String, List<sentiment>> entry : groupedBySymbol.entrySet()) {
            List<sentiment> stockSentiments = entry.getValue(); // These are already sorted DESC by date from the DB
            
            // Augment the most recent entry with calculated data
            processSingleStockHistory(stockSentiments);
            
            // Add only the most recent (and now augmented) sentiment to the final list
            processedSentiments.add(stockSentiments.get(0));
        }
        return processedSentiments;
    }

    /**
     * A helper to calculate and set aggregate data on the most recent sentiment object in a list.
     * @param stockHistory A list of sentiment objects for a single stock, sorted newest to oldest.
     */
    private void processSingleStockHistory(List<sentiment> stockHistory) {
        if (stockHistory == null || stockHistory.isEmpty()) {
            return;
        }

        ArrayList<Double> lastTenValues = new ArrayList<>();
        double sum = 0.0;
        for (sentiment s : stockHistory) {
            lastTenValues.add(s.sentimentValue);
            sum += s.sentimentValue;
        }

        double percentChange = 0.0;
        if (stockHistory.size() > 1) {
            double recent = stockHistory.get(0).sentimentValue;
            double previous = stockHistory.get(1).sentimentValue;
            percentChange = ((recent+1) - (previous+1) / 2);
        }

        // Get the most recent record to set the calculated values on
        sentiment mostRecent = stockHistory.get(0);
        mostRecent.tenDayAverage = sum / stockHistory.size();
        mostRecent.percentChange = percentChange;
        mostRecent.lastTen = lastTenValues;
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

            boolean success = db.followStock(email, stock);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"status\":\"followed\"}");
            } else {
                sendJsonError(exchange, 500, "Could not follow stock.");
            }
        } catch (SQLException e) {
            System.err.println("Database error in handleFollowStock:");
            e.printStackTrace();
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
            
            boolean success = db.unfollowStock(email, stock);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"status\":\"unfollowed\"}");
            } else {
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