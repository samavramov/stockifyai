package com.stockdashboard;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;
public class aquireSentiment {
    private static final String DIFFBOT_TOKEN = initializeDiffbotToken();
    private static String initializeDiffbotToken() {
        Dotenv dotenv = null;
        String token = null;       
        try {
            dotenv = Dotenv.configure()
                           .directory("backend") 
                           .load();
            token = dotenv.get("DIFFBOT_TOKEN");
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Error loading .env file in aquireSentiment: " + e.getMessage());
            System.err.println("Please ensure backend/.env exists and is properly formatted.");
            token = System.getenv("DIFFBOT_TOKEN");
        }
        if (token == null) {
            System.err.println("CRITICAL ERROR: Diffbot Token environment variable is missing. Please set it in your .env file or system environment. Exiting.");
            System.exit(1); 
        }
        System.out.println("Diffbot Token loaded successfully.");
        return token;
    }
    private static final String DIFFBOT_ENDPOINT = "https://api.diffbot.com/v3/article";
    public static String buildDiffbotUrl(String articleUrl) throws Exception {
        String encodedUrl = URLEncoder.encode(articleUrl, StandardCharsets.UTF_8.toString());
        String fields = "title,text,sentiment";
        return DIFFBOT_ENDPOINT
                + "?token=" + DIFFBOT_TOKEN
                + "&url=" + encodedUrl
                + "&fields=" + fields;
    }
    public static String fetchHttpGet(String urlString) throws Exception {
        URL url = java.net.URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(30_000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
    private static double getValidSentimentScore(OllamaAPI ollamaAPI, String prompt, String model, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                OllamaResult result = ollamaAPI.generate(model, prompt, false, new OptionsBuilder().build());
                String raw = result.getResponse().trim();

                double score = Double.parseDouble(raw);
                return score;
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Attempt " + attempt + " failed: Response not a valid number.");
            } catch (Exception e) {
                System.err.println("❌ Ollama call failed on attempt " + attempt + ": " + e.getMessage());
                break;
            }
        }
        System.err.println("❌ Failed to get a valid sentiment score after " + maxAttempts + " attempts.");
        return 0.0;
    }
    public static double getOllamaSentiment(String url, String company) throws Exception {
        String diffbotUrl = buildDiffbotUrl(url);
        String jsonResponse = fetchHttpGet(diffbotUrl);
        JSONObject json = new JSONObject(jsonResponse);
        String articleText = json.getJSONArray("objects").getJSONObject(0).getString("text");
        OllamaAPI ollamaAPI = new OllamaAPI();
        ollamaAPI.setVerbose(false);
        PromptBuilder promptBuilder = new PromptBuilder()
                .addLine("You are sentiment analysis machine that analyzes articles in plain text.")
                .addLine("Given a question, answer ONLY with one singular decimal representing a sentiment.")
                .addLine("Produce exactly one decimal as your answer.")
                .addLine("DO NOT include ANY extra text apart from the output decimal.")
                .addLine("Do not add any extra information or summary.")
                .addLine("Given an article, return a single numeric sentiment score between -1.0 and 1.0 based only on sentiment toward "
                        + company)
                .addSeparator()
                .addLine("Example: " + company + " stock plunged 20% due to disappointing earnings")
                .addLine("Answer:")
                .addLine("-0.75")
                .addLine("Example: " + company + " beat expectations and gained investor confidence")
                .addLine("Answer:")
                .addLine("0.82")
                .addLine("Example: " + company + " maintained stable performance with no major events")
                .addLine("Answer:")
                .addLine("0.0")
                .addSeparator()
                .add("Analyze this article and return the sentiment score: " + articleText);

        return getValidSentimentScore(ollamaAPI, promptBuilder.build(), "mistral", 3);
    }
}
