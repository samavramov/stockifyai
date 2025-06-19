package com.stockdashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class apiHandler implements HttpHandler {

    private final String frontendUrl; // To store the configured frontend URL

    // List of stocks to process
    private static final String[] SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "META", "NVDA", "NFLX", "CRM", "ORCL",
            "ADBE", "INTC", "AMD", "PYPL", "UBER",
            "SPOT", "ZOOM", "TWTR", "SNAP", "SQ",
            "SHOP", "ROKU", "PINS", "DOCU", "PLTR",
            "COIN", "HOOD", "RBLX", "U", "DDOG"
    };

    /**
     * Constructor to accept dependencies like the frontend URL.
     * @param frontendUrl The allowed origin for CORS requests.
     */
    public apiHandler(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            handleOptionsRequest(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            switch (path) {
                case "/api/saveUser":
                    handleSaveUser(exchange);
                    return;
                case "/api/followStock":
                    handleFollowStock(exchange);
                    return;
                case "/api/unfollowStock":
                    handleUnfollowStock(exchange);
                    return;
                default:
                    sendMethodNotAllowed(exchange);
                    return;
            }
        }

        if ("GET".equalsIgnoreCase(method)) {
            switch (path) {
                case "/api/getUser":
                    handleGetUser(exchange);
                    return;
                case "/api/getFollowedStocks":
                    handleGetFollowedStocks(exchange);
                    return;
                default:
                    handleGetSentiments(exchange);
                    return;
            }
        }

        sendMethodNotAllowed(exchange);
    }
    
    // --- Request Handling Methods ---

    private void handleGetSentiments(HttpExchange exchange) throws IOException {
        databaseInteractions db = new databaseInteractions();
        ArrayList<sentiment> sentiments = new ArrayList<>();

        for (String symbol : SYMBOLS) {
            ArrayList<sentiment> lastTen = db.getLatestSentimentsByStockSymbol(symbol, 10);
            if (!lastTen.isEmpty()) {
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

                if (recent[0] != null && recent[1] != null) {
                    percentChange = (((recent[0]) - (recent[1])) / 2.0) * 100.0;
                }

                double averageSentiment = sum / lastTen.size();
                sentiment mostRecent = lastTen.get(0);
                mostRecent.tenDayAverage = averageSentiment;
                mostRecent.percentChange = percentChange;
                mostRecent.lastTen = lastTenValues;
                sentiments.add(mostRecent);
            }
        }

        String json = toJsonArray(sentiments);
        setCorsHeaders(exchange); // This now uses the environment variable
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    public void handleGetUser(HttpExchange exchange) throws IOException {
        String email = getQueryParam(exchange.getRequestURI().getQuery(), "email");
        if (email == null || email.isEmpty()) {
            sendJsonError(exchange, 400, "Missing email parameter");
            return;
        }
        databaseInteractions db = new databaseInteractions();
        User user = db.getUserByEmail(email);
        if (user == null) {
            sendJsonError(exchange, 404, "User not found");
            return;
        }
        String responseJson = String.format("{\"email\":\"%s\",\"name\":\"%s\",\"picture\":\"%s\"}",
            escapeJson(user.email), escapeJson(user.name), escapeJson(user.picture));
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void handleSaveUser(HttpExchange exchange) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(requestBody);
            String email = root.path("email").asText();
            String name = root.path("name").asText();
            String picture = root.path("picture").asText();

            databaseInteractions db = new databaseInteractions();
            boolean success = db.saveUser(email, name, picture);
            String response = success ? "{\"status\":\"success\"}" : "{\"status\":\"error\"}";
            setCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(success ? 200 : 500, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonError(exchange, 500, "server error");
        }
    }

    private void handleFollowStock(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        String email = root.path("email").asText();
        String stock = root.path("stockSymbol").asText();
        databaseInteractions db = new databaseInteractions();
        boolean success = db.followStock(email, stock);
        String response = success ? "{\"status\":\"followed\"}" : "{\"status\":\"error\"}";
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(success ? 200 : 500, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleUnfollowStock(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        String email = root.path("email").asText();
        String stock = root.path("stockSymbol").asText();
        databaseInteractions db = new databaseInteractions();
        boolean success = db.unfollowStock(email, stock);
        String response = success ? "{\"status\":\"unfollowed\"}" : "{\"status\":\"error\"}";
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(success ? 200 : 500, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleGetFollowedStocks(HttpExchange exchange) throws IOException {
        String email = getQueryParam(exchange.getRequestURI().getQuery(), "email");
        if (email == null || email.isEmpty()) {
            sendJsonError(exchange, 400, "Missing email parameter");
            return;
        }
        databaseInteractions db = new databaseInteractions();
        ArrayList<String> followedStocks = db.getFollowedStocks(email);
        ObjectMapper mapper = new ObjectMapper();
        String response = mapper.writeValueAsString(followedStocks);
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // --- Utility Methods ---

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
        String error = String.format("{\"error\":\"%s\"}", message);
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, error.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(error.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Sets the CORS headers for API responses.
     * Uses the frontendUrl passed in during construction.
     */
    private void setCorsHeaders(HttpExchange exchange) {
        // *** THIS IS THE KEY CHANGE ***
        // It now uses the variable loaded from your .env file
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", this.frontendUrl);
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
    }

    private String toJsonArray(ArrayList<sentiment> list) {
        StringBuilder sb = new StringBuilder("[");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        for (int i = 0; i < list.size(); i++) {
            sentiment s = list.get(i);
            sb.append("{");
            sb.append("\"stockSymbol\":\"").append(escapeJson(s.stockSymbol)).append("\",");
            sb.append("\"companyName\":\"").append(escapeJson(s.companyName)).append("\",");
            sb.append("\"sentimentValue\":").append(s.sentimentValue).append(",");
            sb.append("\"tenDayAverage\":").append(s.tenDayAverage).append(",");
            sb.append("\"percentChange\":").append(s.percentChange).append(",");
            String isoDate = isoFormat.format(s.sentimentTimestamp);
            sb.append("\"sentimentTimestamp\":\"").append(escapeJson(isoDate)).append("\",");
            sb.append("\"lastTen\":[");
            for (int j = 0; j < s.lastTen.size(); j++) {
                sb.append(s.lastTen.get(j));
                if (j < s.lastTen.size() - 1) sb.append(",");
            }
            sb.append("],");
            sb.append("\"url1\":\"").append(escapeJson(s.url1)).append("\",");
            sb.append("\"url2\":\"").append(escapeJson(s.url2)).append("\",");
            sb.append("\"url3\":\"").append(escapeJson(s.url3)).append("\",");
            sb.append("\"llmAnalysis\":\"").append(escapeJson(s.llmAnalysis)).append("\"");
            sb.append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}