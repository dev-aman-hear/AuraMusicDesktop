package aura.music.online;

import com.google.gson.JsonArray;
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

/** Official YouTube Data API search. Playback is handled by the official embedded player. */
public final class YoutubeMusicService {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private YoutubeMusicService() { }

    public static CompletableFuture<List<OnlineTrack>> search(String query, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return CompletableFuture.completedFuture(List.of());
        String encoded = URLEncoder.encode(query == null || query.isBlank() ? "music" : query, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&maxResults=15&q="
                + encoded + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body).thenApply(YoutubeMusicService::parse).exceptionally(error -> List.of());
    }

    private static List<OnlineTrack> parse(String body) {
        List<OnlineTrack> tracks = new ArrayList<>();
        JsonArray items = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("items");
        if (items == null) return tracks;
        for (var itemElement : items) {
            JsonObject item = itemElement.getAsJsonObject();
            JsonObject snippet = item.getAsJsonObject("snippet");
            if (snippet == null || !item.has("id")) continue;
            JsonObject id = item.getAsJsonObject("id");
            if (!id.has("videoId")) continue;
            JsonObject thumbnails = snippet.getAsJsonObject("thumbnails");
            String artwork = thumbnails != null && thumbnails.has("medium")
                    ? thumbnails.getAsJsonObject("medium").get("url").getAsString() : "";
            String title = snippet.has("title") ? snippet.get("title").getAsString() : "YouTube video";
            String channel = snippet.has("channelTitle") ? snippet.get("channelTitle").getAsString() : "YouTube";
            tracks.add(new OnlineTrack(title, channel, "YouTube", artwork, "", "", 0,
                    OnlineTrack.Source.YOUTUBE, id.get("videoId").getAsString()));
        }
        return tracks;
    }
}
