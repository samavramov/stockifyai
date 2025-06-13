package com.stockdashboard;
import java.util.ArrayList;
import java.util.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class periodicUpdate {
    public static void updateDB() {
        databaseInteractions db = new databaseInteractions();
        db.initializeSchema();
        db.deleteOldSentiments();
        String[] companies = getCompanies();
        String[] symbols = getSymbols();
        for (int i = 0; i < Math.min(companies.length, symbols.length); i++) {
            String company = companies[i];
            String symbol = symbols[i];
            ArrayList<String> urls = aquireURL.getURLs(company);
            if (urls.size() == 0) {
                continue;
            }
            Double sentiments = 0.0;
            for (String url : urls) {
                try {
                    sentiments += aquireSentiment.getOllamaSentiment(url, company);
                    Thread.sleep(12_000);
                } catch (Exception e) {
                    System.out.println("Error getting sentiment for URL: " + url);
                    e.printStackTrace();
                }
            }
            BigDecimal avg = BigDecimal.valueOf(sentiments)
                    .divide(BigDecimal.valueOf(urls.size()), 2, RoundingMode.HALF_UP);
            Double sentimentAvg = avg.doubleValue();
            Date now = new Date();
            sentiment returnSentiment;
            if (urls.size() == 1) {
                returnSentiment = new sentiment(symbol, company, sentimentAvg, now, urls.get(0), null, null, null);
            } else if (urls.size() == 2) {
                returnSentiment = new sentiment(symbol, company, sentimentAvg, now, urls.get(0), urls.get(1), null,
                        null);
            } else {
                returnSentiment = new sentiment(symbol, company, sentimentAvg, now, urls.get(0), urls.get(1),
                        urls.get(2),
                        null);
            }
            db.addSentiment(returnSentiment);
            System.out.println("Added sentiment: " + returnSentiment);
        }
    }
    public static String[] getSymbols() {
        return new String[] {
                "AAPL", "AMD", "AMZN", "AVGO", "BA", "COIN", "DIS", "GME", "GOOGL", "INTC",
                "LCID", "META", "MSFT", "MU", "NFLX", "NVDA", "ORCL", "PLTR", "PYPL", "QCOM",
                "RBLX", "SHOP", "SNAP", "SOFI", "SPOT", "TSLA", "UBER", "WBD", "ZOOM"
        };
    }
    public static String[] getCompanies() {
        return new String[] {
                "Apple Inc.", "Advanced Micro Devices", "Amazon.com Inc.", "Broadcom Inc.", "Boeing Company",
                "Coinbase Global Inc.", "Walt Disney Co.", "GameStop Corp.", "Alphabet Inc.", "Intel Corporation",
                "Lucid Group Inc.", "Meta Platforms Inc.", "Microsoft Corporation", "Micron Technology Inc.",
                "Netflix Inc.",
                "NVIDIA Corporation", "Oracle Corporation", "Palantir Technologies", "PayPal Holdings Inc.",
                "Qualcomm Incorporated",
                "Roblox Corporation", "Shopify Inc.", "Snapchat Inc.", "SoFi Technologies Inc.",
                "Spotify Technology SA",
                "Tesla Inc.", "Uber Technologies Inc.", "Warner Bros. Discovery Inc.", "Zoom Video Communications"
        };
    }
}
