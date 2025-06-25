package com.stockdashboard;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import io.github.cdimascio.dotenv.Dotenv;

public class databaseInteractions {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        Dotenv dotenv = null;
        String dbUrlTemp = null;    // Use temporary variables
        String dbUserTemp = null;
        String dbPasswordTemp = null;

        try {
            dotenv = Dotenv.configure().directory(".").load();
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
            System.err.println("Attempting to fall back to system environment variables for DB credentials.");
        }

        if (dotenv != null) {
            dbUrlTemp = dotenv.get("DB_URL");
            dbUserTemp = dotenv.get("DB_USER");
            dbPasswordTemp = dotenv.get("DB_PASSWORD");
        }

        if (dbUrlTemp == null)
            dbUrlTemp = System.getenv("DB_URL");
        if (dbUserTemp == null)
            dbUserTemp = System.getenv("DB_USER");
        if (dbPasswordTemp == null)
            dbPasswordTemp = System.getenv("DB_PASSWORD");

        DB_URL = dbUrlTemp;
        DB_USER = dbUserTemp;
        DB_PASSWORD = dbPasswordTemp;

        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            System.err.println(
                    "CRITICAL ERROR: Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set. Exiting.");
            System.exit(1);
        }
        System.out.println("DB_URL: " + DB_URL);
        System.out.println("DB_USER: " + DB_USER);
        System.out.println("DB_PASSWORD: xxxx");
    }

    public void deleteOldSentiments() {
        String sql = "DELETE FROM Stocks WHERE SentimentTimestamp < ?";
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(11);
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        String[] sqls = new String[3];
        sqls[0] = "CREATE TABLE Stocks(" +
                "StockSymbol VARCHAR2(20) NOT NULL," +
                "CompanyName VARCHAR2(255) NOT NULL," +
                "Sentiment NUMBER(10, 4) NOT NULL," +
                "SentimentTimestamp TIMESTAMP NOT NULL," +
                "URLS CLOB CHECK (URLS IS JSON) NOT NULL," +
                "LLMAnalysis CLOB," +
                "PRIMARY KEY (StockSymbol, SentimentTimestamp)" +
                ")";
        sqls[1] = "CREATE TABLE users (" +
                "email VARCHAR2(255) PRIMARY KEY," +
                "name VARCHAR2(255)," +
                "picture VARCHAR2(1024)" +
                ")";
        sqls[2] = "CREATE TABLE user_stocks (" +
                "user_email VARCHAR2(255) REFERENCES users(email) ON DELETE CASCADE," +
                "stock_symbol VARCHAR2(20)," +
                "PRIMARY KEY (user_email, stock_symbol)" +
                ")";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                    String tableName = sql.split(" ")[2].split("\\(")[0];
                    System.out.println("Table '" + tableName + "' created successfully.");
                } catch (SQLException e) {
                    if (e.getErrorCode() == 955) {
                        String tableName = sql.split(" ")[2].split("\\(")[0];
                        System.out.println("Table '" + tableName + "' already exists. No action taken.");
                    } else {
                        throw e;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing schema:");
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found. Make sure ojdbcX.jar is in your classpath.");
            throw new SQLException("JDBC Driver not found", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            System.err.println("Database connection test FAILED");
            e.printStackTrace();
            return false;
        }
    }

    public void addSentiment(sentiment s) {
        String sql = "INSERT INTO Stocks (StockSymbol, CompanyName, Sentiment, SentimentTimestamp, URLS, LLMAnalysis) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.stockSymbol);
            ps.setString(2, s.companyName);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(s.sentimentValue));
            ps.setTimestamp(4, new Timestamp(s.sentimentTimestamp.getTime()));

            String urlsJson = String.format("[\"%s\", \"%s\", \"%s\"]",
                                    s.url1.replace("\"", "\\\""),
                                    s.url2.replace("\"", "\\\""),
                                    s.url3.replace("\"", "\\\""));

            ps.setString(5, urlsJson);
            ps.setString(6, s.llmAnalysis);
            long startTime = System.nanoTime();
            ps.executeUpdate();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
            System.out.println("Time taken to add sentiment: " + duration + "ms");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * --- NEW, OPTIMIZED METHOD ---
     * Fetches the latest sentiment records for a list of stock symbols in a single, efficient query.
     * This uses a window function for optimal performance on the database side.
     *
     * @param stockSymbols A list of stock symbols to fetch data for.
     * @param limit The number of recent records to fetch for each symbol.
     * @return A list of sentiment objects.
     * @throws SQLException
     */
    public ArrayList<sentiment> getLatestSentimentsForSymbols(List<String> stockSymbols, Integer limit) throws SQLException {
        ArrayList<sentiment> results = new ArrayList<>();
        if (stockSymbols == null || stockSymbols.isEmpty()) {
            return results; // Nothing to do, return empty list
        }
        if (limit > 20) {
            limit = 20;
        }

        // This SQL query uses a Window Function to get the 'top N' rows for each symbol group.
        // It is the most efficient way to solve this problem in a single query.
        // NOTE: Oracle 12c or later is required for the "FETCH FIRST" clause.
        String sql = "WITH RankedSentiments AS (" +
                     "  SELECT s.*, ROW_NUMBER() OVER(PARTITION BY StockSymbol ORDER BY SentimentTimestamp DESC) as rn " +
                     "  FROM Stocks s " +
                     "  WHERE StockSymbol IN (%s)" + // Dynamic placeholder for symbols
                     ") " +
                     "SELECT StockSymbol, CompanyName, Sentiment, SentimentTimestamp, URLS, LLMAnalysis " +
                     "FROM RankedSentiments " +
                     "WHERE rn <= ?";

        // Create the correct number of '?' placeholders for the IN clause
        String inClause = String.join(",", Collections.nCopies(stockSymbols.size(), "?"));
        String formattedSql = String.format(sql, inClause);

        long startTime = System.nanoTime();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(formattedSql)) {
            // Set the stock symbols for the IN clause
            int i = 1;
            for (String symbol : stockSymbols) {
                ps.setString(i++, symbol);
            }
            // Set the final parameter for the row number limit
            ps.setInt(i, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String symbol = rs.getString("StockSymbol");
                    String company = rs.getString("CompanyName");
                    double sentimentValue = rs.getBigDecimal("Sentiment").doubleValue();
                    Timestamp ts = rs.getTimestamp("SentimentTimestamp");
                    Date sentimentDate = new Date(ts.getTime());
                    String urlsJson = rs.getString("URLS");

                    String[] urlArray = {"", "", ""};
                    if (urlsJson != null && !urlsJson.trim().isEmpty()) {
                        try {
                            JsonArray jsonArray = JsonParser.parseString(urlsJson).getAsJsonArray();
                            for (int j = 0; j < jsonArray.size() && j < 3; j++) {
                                JsonElement element = jsonArray.get(j);
                                urlArray[j] = element.isJsonNull() ? "" : element.getAsString();
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing URLS JSON for symbol " + symbol + ": " + urlsJson + "; " + e.getMessage());
                        }
                    }

                    sentiment s = new sentiment(symbol, company, sentimentValue, sentimentDate, urlArray[0], urlArray[1], urlArray[2], rs.getString("LLMAnalysis"));
                    results.add(s);
                }
            }
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("Time taken to get latest sentiments for " + stockSymbols.size() + " symbols: " + duration + "ms");
        return results;
    }


    /**
     * --- REFACTORED OLD METHOD ---
     * This method is now a simple and efficient wrapper around the new bulk method.
     * This maintains the existing method signature while benefiting from the optimized query path.
     */
    public ArrayList<sentiment> getLatestSentimentsByStockSymbol(String stockSymbol, Integer limit) throws SQLException {
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Call the new, powerful method with a list containing just one symbol.
        return getLatestSentimentsForSymbols(Collections.singletonList(stockSymbol), limit);
    }

    public boolean saveUser(String email, String name, String picture) {
        String sql = "MERGE INTO users u " +
                "USING (SELECT ? AS email, ? AS name, ? AS picture FROM dual) s " +
                "ON (u.email = s.email) " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (email, name, picture) VALUES (s.email, s.name, s.picture) " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET u.name = s.name, u.picture = s.picture";
        long startTime = System.nanoTime();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            stmt.setString(3, picture);
            int rowsAffected = stmt.executeUpdate();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
            System.out.println("Time taken to save user: " + duration + "ms");
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQLException in saveUser:");
            e.printStackTrace();
            return false;
        }

    }

    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT email, name, picture FROM users WHERE email = ?";
        long startTime = System.nanoTime();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("email"), rs.getString("name"), rs.getString("picture"));
                }
            }
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
        System.out.println("Time taken to get user by email: " + duration + "ms");
        return null;
    }

    public boolean followStock(String email, String stockSymbol) throws SQLException {
        String sql = "INSERT INTO user_stocks (user_email, stock_symbol) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, stockSymbol);
            long startTime = System.nanoTime();
            int rows = stmt.executeUpdate();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
            System.out.println("Time taken to follow stock: " + duration + "ms");
            return rows > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // ORA-00001: unique constraint violated
                System.out.println("User already follows stock. No action taken.");
                return true;
            }
            // --- FIX: Re-throw other SQLExceptions to be handled by the API layer ---
            throw e;
        }
    }

    public boolean unfollowStock(String email, String stockSymbol) throws SQLException {
        String sql = "DELETE FROM user_stocks WHERE user_email = ? AND stock_symbol = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, stockSymbol);
            long startTime = System.nanoTime();

            int rows = stmt.executeUpdate();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
            System.out.println("Time taken to unfollow stock: " + duration + "ms");
            return rows > 0;
        }
    }

    // --- FIX: This method now throws SQLException on failure instead of returning an empty list ---
    public ArrayList<String> getFollowedStocks(String email) throws SQLException {
        ArrayList<String> followed = new ArrayList<>();
        String sql = "SELECT stock_symbol FROM user_stocks WHERE user_email = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            long startTime = System.nanoTime();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    followed.add(rs.getString("stock_symbol"));
                }
            }
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to milliseconds
            System.out.println("Time taken to get followed stocks: " + duration + "ms");    
        }
        // Let any SQLException bubble up to the apiHandler
        return followed;
    }

    public static void main(String[] args) {
        System.out.println("Running local database connection test...");
        databaseInteractions db = new databaseInteractions();
        boolean isConnected = db.testConnection();
        if (isConnected) {
            System.out.println("===================================================================");
            System.out.println("SUCCESS: Connection to Oracle Autonomous Database was successful!");
            System.out.println("===================================================================");
            db.initializeSchema();
        } else {
            System.out.println("*****************************************************************");
            System.out.println("FAILURE: Could not connect to the database. Check console errors.");
            System.out.println("*****************************************************************");
        }
    }
}