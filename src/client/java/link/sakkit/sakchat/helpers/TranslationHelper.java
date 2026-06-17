package link.sakkit.sakchat.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import link.sakkit.sakchat.config.ConfigManager;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TranslationHelper {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static CompletableFuture<String> translate(String text, String toLang) {

        ConfigManager.debugLog("[TRANSLATE] Requested: '" + text + "' → " + toLang);

        if (text == null || text.isBlank()) {
            ConfigManager.debugLog("[TRANSLATE] Empty text, skipping.");
            return CompletableFuture.completedFuture("");
        }

        var config = ConfigManager.configs.config.translations;
        String mode = config.apiProvider != null ? config.apiProvider.toUpperCase() : "AUTO";

        ConfigManager.debugLog("[TRANSLATE] Provider selected: " + mode);

        return switch (mode) {
            case "GOOGLE" -> translateGoogleFree(text, toLang);
            case "CUSTOM" -> translateCustom(text, toLang, config.custom, config.customType, config.customKey);
            default -> {
                ConfigManager.debugLog("[TRANSLATE] AUTO mode: trying Custom → Google");
                yield translateCustom(text, toLang, config.custom, config.customType, config.customKey)
                        .exceptionallyCompose(err -> {
                            ConfigManager.debugLog("[TRANSLATE] Custom failed: " + err);
                            return translateGoogleFree(text, toLang);
                        })

                        .exceptionally(err -> {
                            ConfigManager.debugLog("[TRANSLATE] All providers failed: " + err);
                            return "§c[Translation Error]";
                        });
            }
        };
    }

    // ------------------------------------------------------------
    // GOOGLE FREE
    // ------------------------------------------------------------
    public static CompletableFuture<String> translateGoogleFree(String text, String toLang) {

        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                + toLang + "&dt=t&q=" + encode(text);

        ConfigManager.debugLog("[GOOGLE] URL: " + url);

        return sendRequest(url).thenApply(body -> {
            ConfigManager.debugLog("[GOOGLE] Response: " + body);

            try {
                JsonArray root = JsonParser.parseString(body).getAsJsonArray();
                JsonArray arr = root.get(0).getAsJsonArray();

                StringBuilder out = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    out.append(arr.get(i).getAsJsonArray().get(0).getAsString());
                }

                String result = out.toString().trim();
                ConfigManager.debugLog("[GOOGLE] Parsed: " + result);
                return result;

            } catch (Exception e) {
                ConfigManager.debugLog("[GOOGLE] PARSE ERROR: " + e);
                throw new RuntimeException("Failed to parse Google Free response", e);
            }
        });
    }

    // ------------------------------------------------------------
    // CUSTOM
    // ------------------------------------------------------------
    public static CompletableFuture<String> translateCustom(
            String text, String toLang, String rawUrl, String format, String jsonKey) {

        ConfigManager.debugLog("[CUSTOM] Raw URL: " + rawUrl);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (rawUrl == null || !rawUrl.contains("{message}")) {
                    ConfigManager.debugLog("[CUSTOM] Invalid URL template");
                    throw new IllegalArgumentException("Invalid Custom URL configuration");
                }

                String finalUrl = rawUrl
                        .replace("{message}", encode(text))
                        .replace("{to_lang}", encode(toLang));

                ConfigManager.debugLog("[CUSTOM] Final URL: " + finalUrl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                ConfigManager.debugLog("[CUSTOM] HTTP " + response.statusCode());
                ConfigManager.debugLog("[CUSTOM] Response: " + response.body());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Server responded with HTTP code: " + response.statusCode());
                }

                String body = response.body();

                if ("TXT".equalsIgnoreCase(format)) {
                    ConfigManager.debugLog("[CUSTOM] TXT Parsed: " + body.trim());
                    return body.trim();

                } else if ("JSON".equalsIgnoreCase(format)) {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    if (!json.has(jsonKey)) {
                        ConfigManager.debugLog("[CUSTOM] Missing JSON key: " + jsonKey);
                        throw new RuntimeException("JSON key '" + jsonKey + "' not found!");
                    }

                    String result = json.get(jsonKey).getAsString().trim();
                    ConfigManager.debugLog("[CUSTOM] JSON Parsed: " + result);
                    return result;
                }

                throw new RuntimeException("Unsupported format type: " + format);

            } catch (Exception e) {
                ConfigManager.debugLog("[CUSTOM] ERROR: " + e);
                throw new RuntimeException("Error during custom translation processing", e);
            }
        });
    }

    // ------------------------------------------------------------
    // HTTP WRAPPER
    // ------------------------------------------------------------
    private static CompletableFuture<String> sendRequest(String url) {

        ConfigManager.debugLog("[HTTP] GET " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {

                    ConfigManager.debugLog("[HTTP] Status: " + response.statusCode());
                    ConfigManager.debugLog("[HTTP] Body: " + response.body());

                    if (response.statusCode() == 200) {
                        return response.body();
                    }

                    throw new RuntimeException("HTTP Request Error: " + response.statusCode());
                });
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
