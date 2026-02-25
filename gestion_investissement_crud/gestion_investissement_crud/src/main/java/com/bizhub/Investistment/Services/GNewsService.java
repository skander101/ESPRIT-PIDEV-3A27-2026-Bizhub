package com.bizhub.Investistment.Services;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GNewsService {

    public static final class Article {
        private final String title;
        private final String url;
        private final String publishedAt;
        private final String sourceName;

        public Article(String title, String url, String publishedAt, String sourceName) {
            this.title = title;
            this.url = url;
            this.publishedAt = publishedAt;
            this.sourceName = sourceName;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public String getSourceName() {
            return sourceName;
        }
    }

    public static final class NewsSnapshot {
        private final String query;
        private final List<Article> articles;
        private final Sentiment sentiment;
        private final Instant fetchedAt;

        public NewsSnapshot(String query, List<Article> articles, Sentiment sentiment, Instant fetchedAt) {
            this.query = query;
            this.articles = articles;
            this.sentiment = sentiment;
            this.fetchedAt = fetchedAt;
        }

        public String getQuery() {
            return query;
        }

        public List<Article> getArticles() {
            return articles;
        }

        public Sentiment getSentiment() {
            return sentiment;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }
    }

    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE,
        UNKNOWN
    }

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final String API_BASE = "https://gnews.io/api/v4/search";
    private static final String DEFAULT_LANG = "fr";
    private static final String DEFAULT_COUNTRY = "tn";

    private final HttpClient httpClient;

    private String cachedQuery;
    private NewsSnapshot cachedSnapshot;
    private final RssNewsService rssFallback;

    public GNewsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.rssFallback = new RssNewsService();
    }

    public NewsSnapshot searchProjectNews(String query, String lang, int max) throws IOException, InterruptedException {
        return searchProjectNews(query, lang, DEFAULT_COUNTRY, max);
    }

    public NewsSnapshot searchProjectNews(String query, String lang, String country, int max) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return new NewsSnapshot("", Collections.emptyList(), Sentiment.UNKNOWN, Instant.now());
        }

        if (cachedSnapshot != null && cachedQuery != null && cachedQuery.equalsIgnoreCase(query)) {
            Duration age = Duration.between(cachedSnapshot.getFetchedAt(), Instant.now());
            if (age.compareTo(CACHE_TTL) < 0) {
                return cachedSnapshot;
            }
        }

        try {
            // Try NewsAPI first
            NewsApiService newsApi = new NewsApiService();
            NewsApiService.NewsSnapshot apiSnap = newsApi.searchProjectNews(query, lang, max);
            List<Article> apiArticles = new ArrayList<>();
            for (NewsApiService.Article a : apiSnap.getArticles()) {
                apiArticles.add(new Article(a.getTitle(), a.getUrl(), a.getPublishedAt(), a.getSourceName()));
            }
            Sentiment apiSentiment = switch (apiSnap.getSentiment()) {
                case POSITIVE -> Sentiment.POSITIVE;
                case NEGATIVE -> Sentiment.NEGATIVE;
                case NEUTRAL -> Sentiment.NEUTRAL;
            };
            cachedQuery = query;
            cachedSnapshot = new NewsSnapshot(query, apiArticles, apiSentiment, apiSnap.getFetchedAt());
            return cachedSnapshot;
        } catch (Exception e) {
            // Fallback to RSS when NewsAPI fails
            try {
                RssNewsService.NewsSnapshot rssSnap = rssFallback.searchProjectNews(query, lang, max);
                List<Article> rssArticles = new ArrayList<>();
                for (RssNewsService.Article a : rssSnap.getArticles()) {
                    rssArticles.add(new Article(a.getTitle(), a.getUrl(), a.getPublishedAt(), a.getSourceName()));
                }
                Sentiment rssSentiment = rssSnap.getSentiment();
                cachedQuery = query;
                cachedSnapshot = new NewsSnapshot(query, rssArticles, rssSentiment, rssSnap.getFetchedAt());
                return cachedSnapshot;
            } catch (Exception fallbackErr) {
                // If RSS also fails, rethrow original NewsAPI error
                throw e;
            }
        }
    }

    private String safeErrorMessage(String body) {
        if (body == null || body.isBlank()) return "empty response";

        String message = extractJsonString(body, "\"message\":\"", 0);
        if (message != null && !message.isBlank()) {
            return unescapeJson(message);
        }
        String errors = extractJsonString(body, "\"errors\":\"", 0);
        if (errors != null && !errors.isBlank()) {
            return unescapeJson(errors);
        }

        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private List<Article> parseArticles(String json, int limit) {
        if (json == null || json.isBlank()) return Collections.emptyList();

        int idx = json.indexOf("\"articles\"");
        if (idx < 0) return Collections.emptyList();

        List<Article> list = new ArrayList<>();
        int cursor = idx;

        for (int i = 0; i < limit; i++) {
            String title = extractJsonString(json, "\"title\":\"", cursor);
            String url = extractJsonString(json, "\"url\":\"", cursor);
            String publishedAt = extractJsonString(json, "\"publishedAt\":\"", cursor);

            String sourceName = null;
            int sourceIdx = json.indexOf("\"source\"", cursor);
            if (sourceIdx >= 0) {
                sourceName = extractJsonString(json, "\"name\":\"", sourceIdx);
            }

            if (title == null || url == null) break;

            list.add(new Article(unescapeJson(title), unescapeJson(url),
                    publishedAt == null ? "" : unescapeJson(publishedAt),
                    sourceName == null ? "" : unescapeJson(sourceName)));

            int next = json.indexOf("\"title\"", cursor + 1);
            if (next < 0) break;
            cursor = next;
        }

        return list;
    }

    private String extractJsonString(String json, String marker, int fromIndex) {
        int k = json.indexOf(marker, fromIndex);
        if (k < 0) return null;
        int start = k + marker.length();
        int end = start;
        boolean escaped = false;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (escaped) {
                escaped = false;
                end++;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                end++;
                continue;
            }
            if (c == '"') break;
            end++;
        }
        if (end <= start || end >= json.length()) return null;
        return json.substring(start, end);
    }

    private String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\");
    }

    private Sentiment computeSentiment(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return Sentiment.UNKNOWN;

        int score = 0;
        for (Article a : articles) {
            String t = a.getTitle() == null ? "" : a.getTitle().toLowerCase();
            score += sentimentScore(t);
        }

        if (score >= 2) return Sentiment.POSITIVE;
        if (score <= -2) return Sentiment.NEGATIVE;
        return Sentiment.NEUTRAL;
    }

    private int sentimentScore(String text) {
        int s = 0;

        String[] positive = {"growth", "record", "profit", "success", "wins", "award", "funding", "raises", "partnership", "expands", "launch"};
        String[] negative = {"loss", "lawsuit", "fraud", "scandal", "decline", "crisis", "bankruptcy", "down", "fired", "shutdown", "hack"};

        for (String p : positive) if (text.contains(p)) s++;
        for (String n : negative) if (text.contains(n)) s--;

        return s;
    }
}
