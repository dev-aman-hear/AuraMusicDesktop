package aura.music.lyrics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class OnlineLyricsService {
    
    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    
    public static CompletableFuture<String> fetchLyricsAsync(String trackName, String artistName, String albumName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Using lrclib.net open API
                String url = "https://lrclib.net/api/get?";
                if (trackName != null && !trackName.isEmpty()) url += "track_name=" + URLEncoder.encode(trackName, StandardCharsets.UTF_8);
                if (artistName != null && !artistName.isEmpty()) url += "&artist_name=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8);
                if (albumName != null && !albumName.isEmpty()) url += "&album_name=" + URLEncoder.encode(albumName, StandardCharsets.UTF_8);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "AuraMusicDesktop/1.0 (https://github.com/YOUR_USERNAME/AuraMusicDesktop)")
                        .GET()
                        .build();
                        
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()) {
                        String synced = json.get("syncedLyrics").getAsString();
                        if (synced != null && !synced.isBlank()) {
                            return synced;
                        }
                    }
                    if (json.has("plainLyrics") && !json.get("plainLyrics").isJsonNull()) {
                        String plain = json.get("plainLyrics").getAsString();
                        if (plain != null && !plain.isBlank()) {
                            return plain;
                        }
                    }
                }
                
                // Fallback attempt: query without album_name if initial query returned 404 or empty
                if (albumName != null && !albumName.isEmpty()) {
                    String fallbackUrl = "https://lrclib.net/api/get?";
                    if (trackName != null && !trackName.isEmpty()) fallbackUrl += "track_name=" + URLEncoder.encode(trackName, StandardCharsets.UTF_8);
                    if (artistName != null && !artistName.isEmpty()) fallbackUrl += "&artist_name=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8);
                    
                    HttpRequest fallbackReq = HttpRequest.newBuilder()
                            .uri(URI.create(fallbackUrl))
                            .header("User-Agent", "AuraMusicDesktop/1.0")
                            .GET()
                            .build();
                    HttpResponse<String> fbResponse = client.send(fallbackReq, HttpResponse.BodyHandlers.ofString());
                    if (fbResponse.statusCode() == 200) {
                        JsonObject fbJson = JsonParser.parseString(fbResponse.body()).getAsJsonObject();
                        if (fbJson.has("syncedLyrics") && !fbJson.get("syncedLyrics").isJsonNull()) {
                            String synced = fbJson.get("syncedLyrics").getAsString();
                            if (synced != null && !synced.isBlank()) return synced;
                        }
                        if (fbJson.has("plainLyrics") && !fbJson.get("plainLyrics").isJsonNull()) {
                            String plain = fbJson.get("plainLyrics").getAsString();
                            if (plain != null && !plain.isBlank()) return plain;
                        }
                    }
                }

                // Fallback 2: query search endpoint `https://lrclib.net/api/search?q=` if exact get endpoint missed
                String query = (trackName != null ? trackName : "") + " " + (artistName != null ? artistName : "");
                if (!query.trim().isEmpty()) {
                    String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
                    HttpRequest searchReq = HttpRequest.newBuilder()
                            .uri(URI.create(searchUrl))
                            .header("User-Agent", "AuraMusicDesktop/1.0")
                            .GET()
                            .build();
                    HttpResponse<String> searchResp = client.send(searchReq, HttpResponse.BodyHandlers.ofString());
                    if (searchResp.statusCode() == 200) {
                        com.google.gson.JsonArray arr = JsonParser.parseString(searchResp.body()).getAsJsonArray();
                        
                        // Pass 1: Look for exact or close track match with synced lyrics
                        for (com.google.gson.JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String resultTrack = obj.has("trackName") && !obj.get("trackName").isJsonNull() ? obj.get("trackName").getAsString() : "";
                                if (trackName == null || matchesTrack(trackName, resultTrack)) {
                                    if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
                                        String synced = obj.get("syncedLyrics").getAsString();
                                        if (synced != null && !synced.isBlank()) return synced;
                                    }
                                }
                            }
                        }

                        // Pass 2: Fallback to plain lyrics if track name matches
                        for (com.google.gson.JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String resultTrack = obj.has("trackName") && !obj.get("trackName").isJsonNull() ? obj.get("trackName").getAsString() : "";
                                if (trackName == null || matchesTrack(trackName, resultTrack)) {
                                    if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull()) {
                                        String plain = obj.get("plainLyrics").getAsString();
                                        if (plain != null && !plain.isBlank()) return plain;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch lyrics: " + e.getMessage());
            }
            return null;
        });
    }

    private static boolean matchesTrack(String target, String candidate) {
        if (target == null || candidate == null) return true;
        String t = target.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String c = candidate.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return t.contains(c) || c.contains(t);
    }
}
