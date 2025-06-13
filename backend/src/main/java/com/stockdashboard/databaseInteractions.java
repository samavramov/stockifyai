package com.stockdashboard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

import org.postgresql.util.PGobject;

public class databaseInteractions {

    // Update these with your actual PostgreSQL connection details
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/user";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "pass";

    public void deleteOldSentiments() {
        String sql = "DELETE FROM Stocks WHERE SentimentTimestamp < ?";

        // Calculate the cutoff timestamp = current time minus 11 days
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(11);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Convert LocalDateTime to java.sql.Timestamp
            Timestamp cutoffTimestamp = Timestamp.valueOf(cutoffDateTime);

            pstmt.setTimestamp(1, cutoffTimestamp);
            int deletedRows = pstmt.executeUpdate();

            System.out.println("Deleted " + deletedRows + " old sentiment records.");

        } catch (SQLException e) {
            System.err.println("Error deleting old sentiments:");
            e.printStackTrace();
        }
    }

    public void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS Stocks(" +
                "StockSymbol TEXT NOT NULL," +
                "CompanyName TEXT NOT NULL," +
                "Sentiment DECIMAL NOT NULL," +
                "SentimentTimestamp TIMESTAMP NOT NULL," +
                "URLS JSONB NOT NULL," +
                "LLMAnalysis TEXT," +
                "PRIMARY KEY (StockSymbol, SentimentTimestamp)" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error initializing schema:");
            e.printStackTrace();
        }
    }

    // ⛓️ Connection utility
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2); // 2 second timeout
        } catch (SQLException e) {
            System.err.println("Database connection test FAILED");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts the given sentiment object into the Stocks table.
     */
    public void addSentiment(sentiment s) {
        String sql = "INSERT INTO Stocks (" +
                "  StockSymbol,         " +
                "  CompanyName,         " +
                "  Sentiment,           " +
                "  SentimentTimestamp,  " +
                "  URLS,                " +
                "  LLMAnalysis          " +
                ") VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.stockSymbol);
            ps.setString(2, s.companyName);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(s.sentimentValue));
            ps.setTimestamp(4, new Timestamp(s.sentimentTimestamp.getTime()));

            // Build JSON array string from url1, url2, url3
            String urlsJson = "[\""
                    + s.url1.replace("\"", "\\\"") + "\",\""
                    + s.url2.replace("\"", "\\\"") + "\",\""
                    + s.url3.replace("\"", "\\\"") + "\"]";

            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(urlsJson);

            ps.setObject(5, jsonObject);
            ps.setString(6, s.llmAnalysis);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a list of sentiment objects for the given stockSymbol,
     * ordered by SentimentTimestamp descending.
     */
    public ArrayList<sentiment> getLatestSentimentsByStockSymbol(String stockSymbol, Integer limit) {
        ArrayList<sentiment> results = new ArrayList<>();
        if (limit > 20)
            limit = 20;

        String sql = "SELECT StockSymbol, CompanyName, Sentiment, SentimentTimestamp, URLS, LLMAnalysis " +
                "FROM Stocks " +
                "WHERE StockSymbol = ? " +
                "ORDER BY SentimentTimestamp DESC " +
                "LIMIT " + limit;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockSymbol);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String symbol = rs.getString("StockSymbol");
                    String company = rs.getString("CompanyName");
                    double sentimentValue = rs.getBigDecimal("Sentiment").doubleValue();

                    Timestamp ts = rs.getTimestamp("SentimentTimestamp");
                    Date sentimentDate = new Date(ts.getTime());

                    // Parse JSONB URLs
                    String urlsJson = rs.getString("URLS");
                    String[] urlArray = urlsJson
                            .substring(1, urlsJson.length() - 1)
                            .replace("\"", "")
                            .split(",");

                    String u1 = urlArray.length > 0 ? urlArray[0] : "";
                    String u2 = urlArray.length > 1 ? urlArray[1] : "";
                    String u3 = urlArray.length > 2 ? urlArray[2] : "";

                    String analysis = rs.getString("LLMAnalysis");

                    sentiment s = new sentiment(symbol, company, sentimentValue, sentimentDate, u1, u2, u3, analysis);
                    results.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Saves a user to the `users` table (if not already present).
     */
    public boolean saveUser(String email, String name, String picture) {
        // Fixed SQL with conflict handling
        String sql = "INSERT INTO users (email, name, picture) VALUES (?, ?, ?) ON CONFLICT (email) DO NOTHING";

        try (
                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            stmt.setString(3, picture);
            int rowsAffected = stmt.executeUpdate();

            System.out.println("Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQLException in saveUser:");
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT email, name, picture FROM users WHERE email = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("picture"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean followStock(String email, String stockSymbol) {
        String sql = "INSERT INTO user_stocks (user_email, stock_symbol) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, stockSymbol);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean unfollowStock(String email, String stockSymbol) {
        String sql = "DELETE FROM user_stocks WHERE user_email = ? AND stock_symbol = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, stockSymbol);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<String> getFollowedStocks(String email) {
        ArrayList<String> followed = new ArrayList<>();
        String sql = "SELECT stock_symbol FROM user_stocks WHERE user_email = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                followed.add(rs.getString("stock_symbol"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followed;
    }

}
