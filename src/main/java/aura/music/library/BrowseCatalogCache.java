package aura.music.library;

import aura.music.model.Song;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Persistent, compact index used by the library browse views. */
final class BrowseCatalogCache {
    private static final String ALBUMS = "albums";
    private static final String ARTISTS = "artists";
    private static final String GENRES = "genres";

    private final Path cacheFile;
    private final Gson gson = new Gson();
    private final Map<String, List<String>> pathsByCategory = new LinkedHashMap<>();
    private boolean loaded;

    BrowseCatalogCache(Path cacheFile) {
        this.cacheFile = cacheFile;
        load();
    }

    List<Song> resolve(String category, Map<String, Song> songsByPath) {
        List<String> paths = pathsByCategory.get(category);
        if (!loaded || paths == null) {
            return null;
        }
        List<Song> songs = new ArrayList<>(paths.size());
        for (String path : paths) {
            Song song = songsByPath.get(path);
            if (song == null) {
                return null;
            }
            songs.add(song);
        }
        return songs;
    }

    void rebuild(List<Song> songs) {
        pathsByCategory.put(ALBUMS, representatives(songs, Song::getAlbum));
        pathsByCategory.put(ARTISTS, representatives(songs, Song::getArtist));
        pathsByCategory.put(GENRES, representatives(songs, Song::getGenre));
        loaded = true;
        save();
    }

    private List<String> representatives(List<Song> songs, java.util.function.Function<Song, String> nameExtractor) {
        Map<String, String> representatives = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Song song : songs) {
            String name = nameExtractor.apply(song);
            if (name != null && !name.isBlank()) {
                representatives.putIfAbsent(name.trim().toLowerCase(Locale.ROOT), song.getPath());
            }
        }
        return new ArrayList<>(representatives.values());
    }

    private void load() {
        if (!Files.isRegularFile(cacheFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (String category : List.of(ALBUMS, ARTISTS, GENRES)) {
                if (root.has(category) && root.get(category).isJsonArray()) {
                    String[] paths = gson.fromJson(root.get(category), String[].class);
                    pathsByCategory.put(category, List.of(paths));
                }
            }
            loaded = pathsByCategory.size() == 3;
        } catch (Exception e) {
            pathsByCategory.clear();
            System.err.println("Unable to read browse cache: " + e.getMessage());
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(cacheFile)) {
            gson.toJson(pathsByCategory, writer);
        } catch (Exception e) {
            System.err.println("Unable to save browse cache: " + e.getMessage());
        }
    }
}
