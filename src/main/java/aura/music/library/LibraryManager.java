package aura.music.library;

import aura.music.model.Playlist;
import aura.music.model.Song;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class LibraryManager {
    private static LibraryManager instance;

    private final List<Song> songs = new CopyOnWriteArrayList<>();
    private final List<Playlist> playlists = new CopyOnWriteArrayList<>();
    private final Set<String> watchedFolders = new ConcurrentSkipListSet<>();

    private final Map<String, Song> songMap = new ConcurrentHashMap<>();
    private final ExecutorService scanExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("AuraMusic-ScanWorker");
                return t;
            });

    private final String appDataPath;
    private final String settingsFile;
    private BrowseCatalogCache browseCatalogCache;
    private boolean browseCatalogCacheDirty;
    // New username field with default
    private final javafx.beans.property.SimpleStringProperty usernameProperty = new javafx.beans.property.SimpleStringProperty(
            "Master");
    private final javafx.beans.property.SimpleBooleanProperty onlineMusicEnabledProperty = new javafx.beans.property.SimpleBooleanProperty(
            false);
    private final javafx.beans.property.SimpleStringProperty youtubeApiKeyProperty = new javafx.beans.property.SimpleStringProperty(
            "");
    private final javafx.beans.property.SimpleIntegerProperty lyricTextSizeProperty = new javafx.beans.property.SimpleIntegerProperty(
            26);

    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private boolean watching = false;

    // Getter and setter for username
    public String getUsername() {
        return usernameProperty.get();
    }

    public void setUsername(String username) {
        usernameProperty.set(username);
        saveSettings();
    }

    public javafx.beans.property.StringProperty usernameProperty() {
        return usernameProperty;
    }

    public boolean isOnlineMusicEnabled() {
        return onlineMusicEnabledProperty.get();
    }

    public void setOnlineMusicEnabled(boolean enabled) {
        onlineMusicEnabledProperty.set(enabled);
        saveSettings();
    }

    public javafx.beans.property.BooleanProperty onlineMusicEnabledProperty() {
        return onlineMusicEnabledProperty;
    }

    public String getYoutubeApiKey() {
        return youtubeApiKeyProperty.get();
    }

    /**
     * Stored in the current user's OS preferences, never in the project or settings
     * JSON.
     */
    public void setYoutubeApiKey(String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        youtubeApiKeyProperty.set(value);
        java.util.prefs.Preferences.userNodeForPackage(LibraryManager.class).put("youtubeDataApiKey", value);
    }

    public javafx.beans.property.StringProperty youtubeApiKeyProperty() {
        return youtubeApiKeyProperty;
    }

    public int getLyricTextSize() {
        return lyricTextSizeProperty.get();
    }

    public void setLyricTextSize(int size) {
        lyricTextSizeProperty.set(size);
        saveSettings();
    }

    public javafx.beans.property.IntegerProperty lyricTextSizeProperty() {
        return lyricTextSizeProperty;
    }

    public interface LibraryListener {
        void onSongAdded(Song song);

        void onSongRemoved(Song song);

        void onSongUpdated(Song song);

        default void onScanStarted(File folder) {
        }

        default void onScanProgress(int songsFound) {
        }

        default void onScanFinished(File folder, int songsFound) {
        }
    }

    private final List<LibraryListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(LibraryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LibraryListener listener) {
        listeners.remove(listener);
    }

    private LibraryManager() {
        String userHome = System.getProperty("user.home");
        appDataPath = userHome + File.separator + ".auramusic";
        new File(appDataPath).mkdirs();

        settingsFile = appDataPath + File.separator + "settings.json";
        youtubeApiKeyProperty.set(java.util.prefs.Preferences.userNodeForPackage(LibraryManager.class)
                .get("youtubeDataApiKey", ""));

        // Initialization deferred to init() to prevent blocking
    }

    public java.util.concurrent.CompletableFuture<Void> init() {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            loadLibraryFromDatabase();
            loadPlaylistsFromDatabase();
            startFolderWatcher();
            loadSettings();
        });
    }

    public static synchronized LibraryManager getInstance() {
        if (instance == null) {
            instance = new LibraryManager();
        }
        return instance;
    }

    public List<Song> getSongs() {
        return songs;
    }

    /**
     * Returns a small, persisted index of representative songs for the requested
     * browse category. The first call after a library change refreshes the JSON
     * cache; subsequent launches only resolve the saved paths.
     */
    public synchronized List<Song> getBrowseCatalog(String category) {
        if (browseCatalogCache == null) {
            browseCatalogCache = new BrowseCatalogCache(Paths.get(appDataPath, "browse-catalog-cache.json"));
        }
        List<Song> cached = browseCatalogCacheDirty ? null : browseCatalogCache.resolve(category, songMap);
        if (cached != null) {
            return cached;
        }
        browseCatalogCache.rebuild(songs);
        browseCatalogCacheDirty = false;
        List<Song> rebuilt = browseCatalogCache.resolve(category, songMap);
        return rebuilt == null ? List.of() : rebuilt;
    }

    private synchronized void invalidateBrowseCatalogCache() {
        browseCatalogCacheDirty = true;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public Set<String> getWatchedFolders() {
        return watchedFolders;
    }

    public void addWatchedFolder(String path) {
        if (!watchedFolders.contains(path)) {
            watchedFolders.add(path);
            saveSettings();
        }
        scanFolderAsync(new File(path));
        scanExecutor.submit(() -> registerWatchDir(Paths.get(path)));
    }

    public void removeWatchedFolder(String path) {
        if (watchedFolders.remove(path)) {
            saveSettings();
            // Remove songs under this folder
            List<Song> toRemove = songs.stream()
                    .filter(s -> s.getPath().startsWith(path))
                    .collect(Collectors.toList());
            songs.removeAll(toRemove);
            toRemove.forEach(s -> {
                songMap.remove(s.getPath());
                DatabaseManager.getInstance().removeSong(s.getPath());
                listeners.forEach(l -> l.onSongRemoved(s));
            });
            invalidateBrowseCatalogCache();
        }
    }

    public void scanFolderAsync(File folder) {
        if (!watchedFolders.contains(folder.getAbsolutePath())) {
            watchedFolders.add(folder.getAbsolutePath());
            saveSettings();
        }

        scanExecutor.submit(() -> {
            listeners.forEach(l -> l.onScanStarted(folder));
            List<Song> scannedSongs = new ArrayList<>();
            scanFolderRecursive(folder, scannedSongs);

            // Phase 2: Batching Updates
            // Process removals
            String folderPath = folder.getAbsolutePath();
            java.util.Set<String> scannedPaths = scannedSongs.stream()
                    .map(Song::getPath)
                    .collect(Collectors.toSet());

            List<Song> toRemove = songs.stream()
                    .filter(s -> s.getPath().startsWith(folderPath) && !scannedPaths.contains(s.getPath()))
                    .collect(Collectors.toList());

            if (!toRemove.isEmpty()) {
                songs.removeAll(toRemove);
                toRemove.forEach(s -> {
                    songMap.remove(s.getPath());
                    DatabaseManager.getInstance().removeSong(s.getPath());
                    listeners.forEach(l -> l.onSongRemoved(s));
                });
                invalidateBrowseCatalogCache();
            }
            listeners.forEach(l -> l.onScanFinished(folder, scannedSongs.size()));
        });
    }

    private void scanFolderRecursive(File folder, List<Song> scannedSongs) {
        File[] files = folder.listFiles();
        if (files == null)
            return;

        List<Song> batch = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolderRecursive(file, scannedSongs);
            } else if (isAudioFile(file.getName())) {
                String path = file.getAbsolutePath();
                long lastMod = file.lastModified();
                Song cached = songMap.get(path);

                Song processedSong = null;
                if (cached != null && cached.getLastModified() == lastMod) {
                    processedSong = cached;
                } else {
                    processedSong = MetadataExtractor.extract(file);
                    if (processedSong != null) {
                        processedSong.setLastModified(lastMod);
                        // Extract artwork during scan
                        ArtworkCache.getInstance().extractAndCacheThumbnail(processedSong);
                    }
                }

                if (processedSong != null) {
                    scannedSongs.add(processedSong);
                    batch.add(processedSong);

                    // Flush batch every 50 songs
                    if (batch.size() >= 50) {
                        processBatch(batch);
                        batch.clear();
                        listeners.forEach(l -> l.onScanProgress(scannedSongs.size()));
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ignored) {
                        } // Smooth scanning
                    }
                }
            }
        }

        // Process remaining in the folder
        if (!batch.isEmpty()) {
            processBatch(batch);
            batch.clear();
            listeners.forEach(l -> l.onScanProgress(scannedSongs.size()));
        }
    }

    private void processBatch(List<Song> batch) {
        for (Song song : batch) {
            Song existing = songMap.get(song.getPath());
            if (existing == null) {
                songs.add(song);
                songMap.put(song.getPath(), song);
                DatabaseManager.getInstance().insertOrUpdateSong(song);
                listeners.forEach(l -> l.onSongAdded(song));
                invalidateBrowseCatalogCache();
            } else if (existing.getLastModified() != song.getLastModified()) {
                song.setFavorite(existing.isFavorite());
                int index = songs.indexOf(existing);
                if (index != -1) {
                    songs.set(index, song);
                }
                songMap.put(song.getPath(), song);
                DatabaseManager.getInstance().insertOrUpdateSong(song);
                listeners.forEach(l -> l.onSongUpdated(song));
                invalidateBrowseCatalogCache();
            }
        }
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav")
                || lower.endsWith(".aac") || lower.endsWith(".alac") || lower.endsWith(".m4a")
                || lower.endsWith(".ogg") || lower.endsWith(".opus");
    }

    // Search Engine
    public List<Song> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(songs);
        }
        return DatabaseManager.getInstance().searchSongs(query);
    }

    // Persistence
    public void saveLibraryToCache() {
        // Obsolete, songs are saved incrementally to DB
    }

    private void loadLibraryFromDatabase() {
        List<Song> loadedSongs = DatabaseManager.getInstance().getAllSongs();
        for (Song song : loadedSongs) {
            if (new File(song.getPath()).exists()) {
                songs.add(song);
                songMap.put(song.getPath(), song);
            }
        }
        browseCatalogCache = new BrowseCatalogCache(Paths.get(appDataPath, "browse-catalog-cache.json"));
    }

    public void savePlaylists() {
        for (Playlist pl : playlists) {
            DatabaseManager.getInstance().savePlaylist(pl);
        }
    }

    // Delete a playlist and persist changes
    public void deletePlaylist(Playlist pl) {
        playlists.remove(pl);
        DatabaseManager.getInstance().removePlaylist(pl.getId());
    }

    // Create a new playlist with a unique ID and default name, then persist
    public Playlist createPlaylist(String name) {
        String id = java.util.UUID.randomUUID().toString();
        Playlist pl = new Playlist(id, name);
        playlists.add(pl);
        DatabaseManager.getInstance().savePlaylist(pl);
        return pl;
    }

    private void loadPlaylistsFromDatabase() {
        List<Playlist> loaded = DatabaseManager.getInstance().getAllPlaylists();
        playlists.addAll(loaded);
    }

    public void saveSettings() {
        try (Writer writer = new FileWriter(settingsFile)) {
            // Create a JSON object with watchedFolders, username, onlineMusicEnabled,
            // lyricTextSize
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("watchedFolders", watchedFolders);
            map.put("username", getUsername());
            map.put("onlineMusicEnabled", isOnlineMusicEnabled());
            map.put("lyricTextSize", getLyricTextSize());
            new GsonBuilder().setPrettyPrinting().create().toJson(map, writer);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public void loadSettings() {
        File file = new File(settingsFile);
        if (!file.exists()) {
            lyricTextSizeProperty.addListener((obs, oldVal, newVal) -> saveSettings());
            return;
        }

        try (Reader reader = new FileReader(file)) {
            com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(reader);
            if (json.isJsonObject()) {
                com.google.gson.JsonObject obj = json.getAsJsonObject();
                if (obj.has("watchedFolders")) {
                    com.google.gson.JsonElement foldersElement = obj.get("watchedFolders");
                    if (foldersElement.isJsonArray()) {
                        for (com.google.gson.JsonElement el : foldersElement.getAsJsonArray()) {
                            watchedFolders.add(el.getAsString());
                        }
                    }
                }
                if (obj.has("username")) {
                    usernameProperty.set(obj.get("username").getAsString());
                }
                if (obj.has("onlineMusicEnabled")) {
                    onlineMusicEnabledProperty.set(obj.get("onlineMusicEnabled").getAsBoolean());
                }
                if (obj.has("lyricTextSize")) {
                    lyricTextSizeProperty.set(obj.get("lyricTextSize").getAsInt());
                }
            } else if (json.isJsonArray()) {
                // Legacy format: array of strings (watched folders)
                for (com.google.gson.JsonElement el : json.getAsJsonArray()) {
                    watchedFolders.add(el.getAsString());
                }
            }
            // Initialize watchers for each folder
            for (String folder : watchedFolders) {
                scanFolderAsync(new File(folder));
                scanExecutor.submit(() -> registerWatchDir(Paths.get(folder)));
            }
        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
        }

        lyricTextSizeProperty.addListener((obs, oldVal, newVal) -> saveSettings());
    }

    // Folder Watcher using java.nio.file.WatchService
    private void startFolderWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            watching = true;

            Thread watchThread = new Thread(this::watchLoop);
            watchThread.setDaemon(true);
            watchThread.setName("AuraMusic-FolderWatcher");
            watchThread.start();
        } catch (IOException e) {
            System.err.println("Failed to initialize watch service: " + e.getMessage());
        }
    }

    private void registerWatchDir(Path dir) {
        if (watchService == null)
            return;
        try {
            // Register recursively
            Files.walk(dir).forEach(path -> {
                if (Files.isDirectory(path)) {
                    try {
                        WatchKey key = path.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE);
                        watchKeys.put(key, path);
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error registering watch directory: " + e.getMessage());
        }
    }

    private void watchLoop() {
        while (watching) {
            try {
                WatchKey key = watchService.take();
                Path dir = watchKeys.get(key);
                if (dir == null)
                    continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path name = (Path) event.context();
                    if (name == null) {
                        continue;
                    }
                    Path child = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child)) {
                            scanExecutor.submit(() -> registerWatchDir(child));
                        } else if (isAudioFile(child.toString())) {
                            Song newSong = MetadataExtractor.extract(child.toFile());
                            if (newSong != null && !songMap.containsKey(newSong.getPath())) {
                                songs.add(newSong);
                                songMap.put(newSong.getPath(), newSong);
                                DatabaseManager.getInstance().insertOrUpdateSong(newSong);
                                listeners.forEach(l -> l.onSongAdded(newSong));
                            }
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        Song removed = songMap.remove(child.toString());
                        if (removed != null) {
                            songs.remove(removed);
                            DatabaseManager.getInstance().removeSong(removed.getPath());
                            listeners.forEach(l -> l.onSongRemoved(removed));
                        }
                    }
                }

                if (!key.reset()) {
                    watchKeys.remove(key);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (java.nio.file.ClosedWatchServiceException e) {
                // Occurs normally during application shutdown/close
                break;
            } catch (Exception e) {
                if (watching) {
                    System.err.println("Error in watch loop: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
                }
            }
        }
    }

    public void shutdown() {
        watching = false;
        scanExecutor.shutdown();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
    }
}
