package com.bizhub.model.services.community.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

/**
 * AI service using Groq API — 100% free, no credit card, works in Tunisia.
 * Uses LLaMA 3.1 8B model — fast and capable.
 * Free tier: 14,400 requests/day, 30 requests/minute.
 */
public class GeminiService {

    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final String MODEL = "llama-3.1-8b-instant";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final String apiKey;

    public GeminiService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey = dotenv.get("GROQ_API_KEY", "");
    }

    /**
     * Send a prompt and get a text response from Groq.
     */
    public String ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠ GROQ_API_KEY not found in .env file.";
        }
        try {
            // System message
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content",
                    "You are an AI assistant embedded in BizHub, a business community platform. " +
                            "Users share posts about business, tech, investment, and events. " +
                            "Be concise, helpful, and professional. " +
                            "For fact-checking: clearly state what is accurate, uncertain, or false. " +
                            "For summaries: use 2-3 sentences max. " +
                            "For topic suggestions: give 3-5 relevant tags, no explanation. " +
                            "Always respond in the same language as the post content."
            );

            // User message
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(systemMsg);
            messages.add(userMsg);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);
            body.addProperty("max_tokens", 512);
            body.addProperty("temperature", 0.7);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return "⚠ API error " + response.code() + ": " + responseBody;
                }
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }
        } catch (Exception e) {
            return "⚠ Error: " + e.getMessage();
        }
    }

    // --- Convenience methods ---

    public String factCheck(String title, String content) {
        return ask("Fact-check this post. Identify claims that are accurate, misleading, or unverifiable. Be concise.\n\nTitle: " + title + "\n\nContent: " + content);
    }

    public String summarize(String title, String content) {
        return ask("Summarize this post in 2-3 sentences.\n\nTitle: " + title + "\n\nContent: " + content);
    }

    public String suggestTopics(String title, String content) {
        return ask("Suggest 4-5 relevant topics or hashtags for this post. Return as comma-separated list only, no explanation.\n\nTitle: " + title + "\n\nContent: " + content);
    }

    public String askAboutPost(String title, String content, String question) {
        return ask("Answer this question about the post.\n\nPost Title: " + title + "\nPost Content: " + content + "\n\nQuestion: " + question);
    }
}