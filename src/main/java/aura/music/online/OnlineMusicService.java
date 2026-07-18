package aura.music.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Catalog discovery and 30-second previews; does not scrape subscription services. */
public final class OnlineMusicService {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private OnlineMusicService() { }

    public static CompletableFuture<List<OnlineTrack>> search(String term) {
        String query = term == null || term.isBlank() ? "new music" : term.trim();
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "https://itunes.apple.com/search?term=" + encoded + "&entity=song&limit=24&country=US"))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(OnlineMusicService::parseTracks)
                .exceptionally(error -> List.of());
    }

    private static List<OnlineTrack> parseTracks(String response) {
        List<OnlineTrack> tracks = new ArrayList<>();
        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        if (results == null) return tracks;
        for (JsonElement element : results) {
            JsonObject item = element.getAsJsonObject();
            String preview = value(item, "previewUrl");
            if (preview.isBlank()) continue;
            tracks.add(new OnlineTrack(value(item, "trackName"), value(item, "artistName"),
                    value(item, "collectionName"), value(item, "artworkUrl100"), preview, value(item, "trackViewUrl"),
                    item.has("trackTimeMillis") ? item.get("trackTimeMillis").getAsInt() : 30000,
                    OnlineTrack.Source.ITUNES, ""));
        }
        return tracks;
    }

    private static String value(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : "";
    }
}
