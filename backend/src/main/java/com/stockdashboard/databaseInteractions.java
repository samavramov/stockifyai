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

// ORACLE CHANGE: No longer need the postgres-specific import
// import org.postgresql.util.PGobject; 
import io.github.cdimascio.dotenv.Dotenv;

public class databaseInteractions {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        
        System.setProperty("oracle.net.tns_admin", "/Users/samavramov/oracle_wallet");
        System.setProperty("oracle.net.ssl_server_dn_match", "true");
        System.setProperty("javax.net.ssl.trustStore", "/Users/samavramov/oracle_wallet/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "@HJR#E73fRH4<1K*r48iDx&+{'2");
        System.setProperty("javax.net.ssl.keyStore", "/Users/samavramov/oracle_wallet/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "@HJR#E73fRH4<1K*r48iDx&+{'2");
        Dotenv dotenv = null;
        String dbUrl = null;
        String dbUser = null;
        String dbPassword = null;

        try {
            dotenv = Dotenv.load();
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
            System.err.println("Attempting to fall back to system environment variables for DB credentials.");
        }

        if (dotenv != null) {
            dbUrl = dotenv.get("DB_URL");
            dbUser = dotenv.get("DB_USER");
            dbPassword = dotenv.get("DB_PASSWORD");
        }

        if (dbUrl == null)
            dbUrl = System.getenv("DB_URL");
        if (dbUser == null)
            dbUser = System.getenv("DB_USER");
        if (dbPassword == null)
            dbPassword = System.getenv("DB_PASSWORD");

        DB_URL = dbUrl;
        DB_USER = dbUser;
        DB_PASSWORD = dbPassword;

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

    // ORACLE CHANGE: Complete rewrite of schema initialization for Oracle SQL
    // dialect.
    public void initializeSchema() {
        String[] sqls = new String[3];
        sqls[0] = "CREATE TABLE Stocks(" +
                "StockSymbol VARCHAR2(20) NOT NULL," +
                "CompanyName VARCHAR2(255) NOT NULL," +
                "Sentiment NUMBER(10, 4) NOT NULL," +
                "SentimentTimestamp TIMESTAMP NOT NULL," +
                "URLS JSON NOT NULL," +
                "LLMAnalysis CLOB," + // Use CLOB for potentially very long text
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
                    // This is a simple way to get the table name for the print statement
                    String tableName = sql.split(" ")[2].split("\\(")[0];
                    System.out.println("Table '" + tableName + "' created successfully.");
                } catch (SQLException e) {
                    // ORA-00955 is the error code for "name is already used by an existing object"
                    if (e.getErrorCode() == 955) {
                        String tableName = sql.split(" ")[2].split("\\(")[0];
                        System.out.println("Table '" + tableName + "' already exists. No action taken.");
                    } else {
                        // Re-throw other errors
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
        // Ensure the Oracle JDBC driver is loaded when getConnection is called
        // This makes sure Class.forName is not solely reliant on static block execution order
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

    // ORACLE CHANGE: Simplified JSON handling.
    public void addSentiment(sentiment s) {
        String sql = "INSERT INTO Stocks (StockSymbol, CompanyName, Sentiment, SentimentTimestamp, URLS, LLMAnalysis) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.stockSymbol);
            ps.setString(2, s.companyName);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(s.sentimentValue));
            ps.setTimestamp(4, new Timestamp(s.sentimentTimestamp.getTime()));
            String urlsJson = "[\""
                    + s.url1.replace("\"", "\\\"") + "\",\""
                    + s.url2.replace("\"", "\\\"") + "\",\""
                    + s.url3.replace("\"", "\\\"") + "\"]";

            // The OJDBC driver can accept a string for a JSON column. No PGobject needed.
            ps.setString(5, urlsJson);
            ps.setString(6, s.llmAnalysis);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ORACLE CHANGE: Use modern Oracle syntax for limiting rows.
    public ArrayList<sentiment> getLatestSentimentsByStockSymbol(String stockSymbol, Integer limit) {
        ArrayList<sentiment> results = new ArrayList<>();
        if (limit > 20)
            limit = 20;

        String sql = "SELECT StockSymbol, CompanyName, Sentiment, SentimentTimestamp, URLS, LLMAnalysis " +
                "FROM Stocks " +
                "WHERE StockSymbol = ? " +
                "ORDER BY SentimentTimestamp DESC " +
                "FETCH FIRST ? ROWS ONLY"; // The standard SQL / Oracle 12c+ way to limit results

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockSymbol);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String symbol = rs.getString("StockSymbol");
                    String company = rs.getString("CompanyName");
                    double sentimentValue = rs.getBigDecimal("Sentiment").doubleValue();
                    Timestamp ts = rs.getTimestamp("SentimentTimestamp");
                    Date sentimentDate = new Date(ts.getTime());
                    String urlsJson = rs.getString("URLS");
                    String[] urlArray = urlsJson.substring(1, urlsJson.length() - 1).replace("\"", "").split(",");
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

    // ORACLE CHANGE: Use MERGE statement instead of ON CONFLICT.
    public boolean saveUser(String email, String name, String picture) {
        String sql = "MERGE INTO users u " +
                "USING (SELECT ? AS email, ? AS name, ? AS picture FROM dual) s " +
                "ON (u.email = s.email) " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (email, name, picture) VALUES (s.email, s.name, s.picture)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            stmt.setString(3, picture);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQLException in saveUser:");
            e.printStackTrace();
            return false;
        }
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT email, name, picture FROM users WHERE email = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("email"), rs.getString("name"), rs.getString("picture"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ORACLE CHANGE: Handle "insert or ignore" by catching the unique constraint
    // violation exception.
    public boolean followStock(String email, String stockSymbol) {
        String sql = "INSERT INTO user_stocks (user_email, stock_symbol) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, stockSymbol);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            // ORA-00001 is the Oracle error code for unique constraint violation
            if (e.getErrorCode() == 1) {
                System.out.println("User already follows stock. No action taken.");
                return false; // Not a new follow, so return false.
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean unfollowStock(String email, String stockSymbol) {
        String sql = "DELETE FROM user_stocks WHERE user_email = ? AND stock_symbol = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    followed.add(rs.getString("stock_symbol"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followed;
    }

    // This is a temporary main method for local testing
    public static void main(String[] args) {
        System.out.println("Running local database connection test...");

        // This will load the .env file from your 'backend' directory
        databaseInteractions db = new databaseInteractions();

        // Call the testConnection method
        boolean isConnected = db.testConnection();

        if (isConnected) {
            System.out.println("===================================================================");
            System.out.println("SUCCESS: Connection to Oracle Autonomous Database was successful!");
            System.out.println("===================================================================");
        } else {
            System.out.println("*****************************************************************");
            System.out.println("FAILURE: Could not connect to the database. Check console errors.");
            System.out.println("*****************************************************************");
            System.out.println("Common issues:");
            System.out.println("1. Is the TNS_ADMIN path in your .env file correct?");
            System.out.println("2. Is the DB_PASSWORD in your .env file correct?");
            System.out.println("3. Is your computer connected to the internet?");
        }
    }
}
