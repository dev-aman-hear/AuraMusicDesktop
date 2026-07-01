package aura.music.library;

import aura.music.model.Playlist;
import aura.music.model.Song;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );
    
    private final String appDataPath;
    private final String libraryCacheFile;
    private final String playlistsFile;
    private final String settingsFile;
    // New username field with default
    private final javafx.beans.property.SimpleStringProperty usernameProperty = new javafx.beans.property.SimpleStringProperty("Master");

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

    public interface LibraryListener {
        void onSongAdded(Song song);
        void onSongRemoved(Song song);
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
        
        libraryCacheFile = appDataPath + File.separator + "library_cache.json";
        playlistsFile = appDataPath + File.separator + "playlists.json";
        settingsFile = appDataPath + File.separator + "settings.json";

        loadLibraryFromCache();
        loadPlaylists();
        startFolderWatcher();
    }

    public static synchronized LibraryManager getInstance() {
        if (instance == null) {
            instance = new LibraryManager();
        }
        return instance;
    }

    public List<Song> getSongs() { return songs; }
    public List<Playlist> getPlaylists() { return playlists; }
    public Set<String> getWatchedFolders() { return watchedFolders; }

    public void addWatchedFolder(String path) {
        if (watchedFolders.add(path)) {
            saveSettings();
            scanFolderAsync(new File(path));
            scanExecutor.submit(() -> registerWatchDir(Paths.get(path)));
        }
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
                listeners.forEach(l -> l.onSongRemoved(s));
            });
            saveLibraryToCache();
        }
    }

    public void scanFolderAsync(File folder) {
        scanExecutor.submit(() -> {
            List<Song> scannedSongs = new ArrayList<>();
            scanFolderRecursive(folder, scannedSongs);
            
            boolean changed = false;
            
            // 1. Add new or updated songs
            for (Song song : scannedSongs) {
                Song existing = songMap.get(song.getPath());
                if (existing == null) {
                    songs.add(song);
                    songMap.put(song.getPath(), song);
                    listeners.forEach(l -> l.onSongAdded(song));
                    changed = true;
                } else if (existing.getLastModified() != song.getLastModified()) {
                    // Update existing song metadata in place
                    int index = songs.indexOf(existing);
                    if (index != -1) {
                        songs.set(index, song);
                    }
                    songMap.put(song.getPath(), song);
                    changed = true;
                }
            }
            
            // 2. Remove songs that no longer exist under this folder
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
                    listeners.forEach(l -> l.onSongRemoved(s));
                });
                changed = true;
            }
            
            if (changed) {
                saveLibraryToCache();
            }
        });
    }

    private void scanFolderRecursive(File folder, List<Song> scannedSongs) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolderRecursive(file, scannedSongs);
            } else if (isAudioFile(file.getName())) {
                String path = file.getAbsolutePath();
                long lastMod = file.lastModified();
                Song cached = songMap.get(path);
                if (cached != null && cached.getLastModified() == lastMod) {
                    scannedSongs.add(cached);
                } else {
                    Song newSong = MetadataExtractor.extract(file);
                    if (newSong != null) {
                        newSong.setLastModified(lastMod);
                        scannedSongs.add(newSong);
                    }
                }
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
        String q = query.toLowerCase();
        return songs.stream()
                .filter(s -> (s.getTitle() != null && s.getTitle().toLowerCase().contains(q))
                        || (s.getArtist() != null && s.getArtist().toLowerCase().contains(q))
                        || (s.getAlbum() != null && s.getAlbum().toLowerCase().contains(q))
                        || (s.getGenre() != null && s.getGenre().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    // Persistence
    public void saveLibraryToCache() {
        try (Writer writer = new FileWriter(libraryCacheFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(songs, writer);
        } catch (IOException e) {
            System.err.println("Failed to save library cache: " + e.getMessage());
        }
    }

    private void loadLibraryFromCache() {
        File file = new File(libraryCacheFile);
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            List<Song> cached = new Gson().fromJson(reader, new TypeToken<List<Song>>(){}.getType());
            if (cached != null) {
                songs.addAll(cached);
                for (Song s : songs) {
                    songMap.put(s.getPath(), s);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load library cache: " + e.getMessage());
        }
    }

    public void savePlaylists() {
        try (Writer writer = new FileWriter(playlistsFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(playlists, writer);
        } catch (IOException e) {
            System.err.println("Failed to save playlists: " + e.getMessage());
        }
    }

    // Delete a playlist and persist changes
    public void deletePlaylist(Playlist pl) {
        playlists.remove(pl);
        savePlaylists();
    }

    // Create a new playlist with a unique ID and default name, then persist
    public Playlist createPlaylist(String name) {
        String id = java.util.UUID.randomUUID().toString();
        Playlist pl = new Playlist(id, name);
        playlists.add(pl);
        savePlaylists();
        return pl;
    }

    private void loadPlaylists() {
        File file = new File(playlistsFile);
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            List<Playlist> loaded = new Gson().fromJson(reader, new TypeToken<List<Playlist>>(){}.getType());
            if (loaded != null) {
                playlists.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Failed to load playlists: " + e.getMessage());
        }
    }

    public void saveSettings() {
        try (Writer writer = new FileWriter(settingsFile)) {
            // Create a JSON object with both watchedFolders and username
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("watchedFolders", watchedFolders);
            map.put("username", getUsername());
            new GsonBuilder().setPrettyPrinting().create().toJson(map, writer);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public void loadSettings() {
        File file = new File(settingsFile);
        if (!file.exists()) return;

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
                    setUsername(obj.get("username").getAsString());
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
        if (watchService == null) return;
        try {
            // Register recursively
            Files.walk(dir).forEach(path -> {
                if (Files.isDirectory(path)) {
                    try {
                        WatchKey key = path.register(watchService, 
                                StandardWatchEventKinds.ENTRY_CREATE, 
                                StandardWatchEventKinds.ENTRY_DELETE);
                        watchKeys.put(key, path);
                    } catch (IOException ignored) {}
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
                if (dir == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path name = (Path) event.context();
                    Path child = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child)) {
                            scanExecutor.submit(() -> registerWatchDir(child));
                        } else if (isAudioFile(child.toString())) {
                            Song newSong = MetadataExtractor.extract(child.toFile());
                            if (!songMap.containsKey(newSong.getPath())) {
                                songs.add(newSong);
                                songMap.put(newSong.getPath(), newSong);
                                listeners.forEach(l -> l.onSongAdded(newSong));
                                saveLibraryToCache();
                            }
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        Song removed = songMap.remove(child.toString());
                        if (removed != null) {
                            songs.remove(removed);
                            listeners.forEach(l -> l.onSongRemoved(removed));
                            saveLibraryToCache();
                        }
                    }
                }
                
                boolean valid = key.reset();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error in watch loop: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        watching = false;
        scanExecutor.shutdown();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
    }
}
