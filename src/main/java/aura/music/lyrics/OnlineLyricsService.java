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
                        if (synced != null && !synced.isEmpty()) {
                            return synced;
                        }
                    }
                    if (json.has("plainLyrics") && !json.get("plainLyrics").isJsonNull()) {
                        return json.get("plainLyrics").getAsString();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch lyrics: " + e.getMessage());
            }
            return null;
        });
    }
}
