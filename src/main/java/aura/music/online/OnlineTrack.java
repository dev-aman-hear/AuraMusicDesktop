package aura.music.online;

/** A playable preview supplied by the iTunes Search API. */
public record OnlineTrack(String title, String artist, String album, String artworkUrl,
                          String previewUrl, String storeUrl, int durationMillis,
                          Source source, String youtubeVideoId) {
    public enum Source { ITUNES, YOUTUBE }
}
