package com.stockdashboard;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv; 
public class aquireURL {
    private static final String API_TOKEN = initializeApiToken();
    private static String initializeApiToken() {
        Dotenv dotenv = null;
        String token = null;
        try {
            dotenv = Dotenv.configure()
                    .directory("backend")
                    .load();
            token = dotenv.get("THENEWSAPI_TOKEN");
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file in aquireURL: " + e.getMessage());
            System.err.println("Please ensure backend/.env exists and is properly formatted.");
            token = System.getenv("THENEWSAPI_TOKEN"); 
        }
        if (token == null) {
            System.err.println(
                    "CRITICAL ERROR: The News API Token environment variable is missing. Please set it in your .env file or system environment. Exiting.");
            System.exit(1);
        }
        System.out.println("The News API Token loaded successfully.");
        return token;
    }
    private static String buildEndpoint(String query, String date) {
        return String.format(
                "https://api.thenewsapi.com/v1/news/all?api_token=%s&search=%s&language=en&categories=business&published_on=%s&limit=3",
                API_TOKEN, 
                query.replace(" ", "%20"), date);
    }
    public static ArrayList<String> getURLs(String searchTerm) {
        String today = LocalDate.now().toString();
        ArrayList<String> urls = new ArrayList<>();
        try {
            String endpoint = buildEndpoint(searchTerm, today);
            URI uri = URI.create(endpoint);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();
            JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
            JSONArray articles = jsonResponse.getJSONArray("data");
            if (articles.length() == 0) {
                System.out.println("No articles found for: " + searchTerm);
            } else {
                for (int i = 0; i < articles.length(); i++) {
                    JSONObject article = articles.getJSONObject(i);
                    String articleUrl = article.getString("url");
                    urls.add(articleUrl);
                }
            }
        } catch (Exception e) {
            System.out.println("Error while fetching news: " + e.getMessage());
            e.printStackTrace();
        }
        return urls;
    }
}
