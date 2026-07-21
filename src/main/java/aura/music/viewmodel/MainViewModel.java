package aura.music.viewmodel;

import aura.music.audio.AudioEngine;
import aura.music.library.LibraryManager;
import aura.music.lyrics.LyricLine;
import aura.music.lyrics.LyricParser;
import aura.music.model.Playlist;
import aura.music.model.Song;
import aura.music.theme.ThemeEngine;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel {

    private final AudioEngine audioEngine = AudioEngine.getInstance();
    private final LibraryManager libraryManager = LibraryManager.getInstance();
    private final ThemeEngine themeEngine = ThemeEngine.getInstance();
    private PauseTransition sleepTimer;
    // Used by provider-owned playback (currently YouTube's official embedded
    // player).
    private boolean externalPlaybackActive;
    private Runnable externalResume;
    private Runnable externalPause;
    private Runnable externalStop;
    private Consumer<Double> externalSeek;

    // Player Properties
    private final ObjectProperty<Song> currentSong = new SimpleObjectProperty<>();
    private final BooleanProperty isPlaying = new SimpleBooleanProperty(false);
    private final DoubleProperty currentTime = new SimpleDoubleProperty(0.0);
    private final DoubleProperty totalDuration = new SimpleDoubleProperty(0.0);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.8);
    private final BooleanProperty isMuted = new SimpleBooleanProperty(false);

    public enum RepeatMode {
        OFF, ONE, ALL
    }

    private final BooleanProperty shuffleMode = new SimpleBooleanProperty(false);
    private final ObjectProperty<RepeatMode> repeatMode = new SimpleObjectProperty<>(RepeatMode.OFF);

    public BooleanProperty shuffleModeProperty() {
        return shuffleMode;
    }

    public ObjectProperty<RepeatMode> repeatModeProperty() {
        return repeatMode;
    }

    private final BooleanProperty miniplayerAlwaysOnTop = new SimpleBooleanProperty(true);

    public BooleanProperty miniplayerAlwaysOnTopProperty() {
        return miniplayerAlwaysOnTop;
    }

    public BooleanProperty onlineMusicEnabledProperty() {
        return libraryManager.onlineMusicEnabledProperty();
    }

    public StringProperty youtubeApiKeyProperty() {
        return libraryManager.youtubeApiKeyProperty();
    }

    // Lists
    private final ObservableList<Song> librarySongs = FXCollections.observableArrayList();
    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private final ObservableList<Song> queue = FXCollections.observableArrayList();
    private final ObservableList<Song> recentlyPlayed = FXCollections.observableArrayList();
    private final IntegerProperty currentQueueIndex = new SimpleIntegerProperty(-1);
    // Filtered view of the queue that always places the currently playing song
    // first
    private final ObservableList<Song> filteredQueue = FXCollections.observableArrayList();

    // Lyrics
    private final ObservableList<LyricLine> lyricsLines = FXCollections.observableArrayList();
    private final IntegerProperty activeLyricLineIndex = new SimpleIntegerProperty(-1);
    private final DoubleProperty lyricSyncOffsetSeconds = new SimpleDoubleProperty(0.0);

    public ObservableList<LyricLine> getLyricsLines() {
        return lyricsLines;
    }

    public IntegerProperty activeLyricLineIndexProperty() {
        return activeLyricLineIndex;
    }

    public DoubleProperty lyricSyncOffsetSecondsProperty() {
        return lyricSyncOffsetSeconds;
    }

    public void adjustLyricOffset(double deltaSeconds) {
        lyricSyncOffsetSeconds.set(Math.round((lyricSyncOffsetSeconds.get() + deltaSeconds) * 10.0) / 10.0);
    }

    public void resetLyricOffset() {
        lyricSyncOffsetSeconds.set(0.0);
    }

    public void loadManualLyricFile(File lrcFile) {
        Song song = currentSong.get();
        if (song == null || lrcFile == null || !lrcFile.exists())
            return;

        try {
            String content = java.nio.file.Files.readString(lrcFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (!content.trim().isEmpty()) {
                song.setEmbeddedLyrics(content);
                aura.music.library.DatabaseManager.getInstance().insertOrUpdateSong(song);
                java.util.List<LyricLine> parsed = LyricParser.parse(content);
                Platform.runLater(() -> {
                    lyricsLines.setAll(parsed);
                });
            }
        } catch (Exception e) {
            System.err.println("Error reading manual lyric file: " + e.getMessage());
        }
    }

    public void saveSyncedLrc(Song song, String lrcContent) {
        if (song == null || lrcContent == null || lrcContent.trim().isEmpty())
            return;

        try {
            song.setEmbeddedLyrics(lrcContent);
            aura.music.library.DatabaseManager.getInstance().insertOrUpdateSong(song);

            // Also save as .lrc file next to the local audio file
            String path = song.getPath();
            if (path != null && !path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("youtube:")) {
                File audioFile = new File(path);
                if (audioFile.exists() && audioFile.getParentFile() != null) {
                    String baseName = audioFile.getName();
                    int dotIdx = baseName.lastIndexOf('.');
                    if (dotIdx != -1) baseName = baseName.substring(0, dotIdx);
                    File lrcFile = new File(audioFile.getParentFile(), baseName + ".lrc");
                    java.nio.file.Files.writeString(lrcFile.toPath(), lrcContent, java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            java.util.List<LyricLine> parsed = LyricParser.parse(lrcContent);
            Platform.runLater(() -> {
                lyricsLines.setAll(parsed);
            });
        } catch (Exception e) {
            System.err.println("Failed to save synced LRC file: " + e.getMessage());
        }
    }

    public IntegerProperty lyricTextSizeProperty() {
        return libraryManager.lyricTextSizeProperty();
    }

    // Navigation/Selection
    private final ObjectProperty<Playlist> selectedPlaylist = new SimpleObjectProperty<>();
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final ObservableList<Song> searchResults = FXCollections.observableArrayList();
    private final StringProperty windowTitle = new SimpleStringProperty("AuraMusic Desktop");

    public StringProperty windowTitleProperty() {
        return windowTitle;
    }

    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("AuraMusic-Background-Executor");
        return t;
    });

    public MainViewModel() {
        // Initialization is deferred to allow UI to show immediately.
        // Listen to changes in library
        libraryManager.addListener(new aura.music.library.LibraryManager.LibraryListener() {
            @Override
            public void onSongAdded(Song song) {
                Platform.runLater(() -> {
                    if (!librarySongs.contains(song)) {
                        librarySongs.add(song);
                    }
                });
            }

            @Override
            public void onSongRemoved(Song song) {
                Platform.runLater(() -> librarySongs.remove(song));
            }

            @Override
            public void onSongUpdated(Song song) {
                Platform.runLater(() -> {
                    int index = librarySongs.indexOf(song);
                    if (index != -1) {
                        librarySongs.set(index, song);
                    }
                });
            }
        });
        // Bind volume
        volume.addListener((obs, oldVal, newVal) -> audioEngine.setVolume(newVal.doubleValue()));
        isMuted.addListener((obs, oldVal, newVal) -> audioEngine.setMuted(newVal));

        // Ensure lyrics are always synced to the current time, regardless of the playback source
        currentTime.addListener((obs, oldVal, newVal) -> {
            updateActiveLyricIndex(newVal.doubleValue() * 1000.0);
        });

        // Smooth progress tracking using AnimationTimer (60fps)
        javafx.animation.AnimationTimer progressTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPlaying.get() && !externalPlaybackActive) {
                    double ms = audioEngine.getCurrentTime().toMillis();
                    if (ms > 0) {
                        currentTime.set(ms / 1000.0);
                    }
                }
            }
        };
        progressTimer.start();

        // Connect AudioEngine listeners
        audioEngine.addOnReady(duration -> Platform.runLater(() -> {
            totalDuration.set(duration.toSeconds());
            isPlaying.set(true);
            updateThemeAndLyrics();
        }));

        audioEngine.addOnProgress(duration -> Platform.runLater(() -> {
            // currentTime is handled by AnimationTimer for smoother updates, 
            // but we can still use this as a fallback.
        }));

        audioEngine.addOnEndOfMedia(() -> Platform.runLater(this::next));

        volume.set(audioEngine.getVolume());

        // Run Library initialization in the background
        libraryManager.init().thenRun(() -> {
            Platform.runLater(() -> {
                librarySongs.addAll(libraryManager.getSongs());
                playlists.addAll(libraryManager.getPlaylists());
                loadPlaybackState();
            });
        });
    }

    public ObjectProperty<Song> currentSongProperty() {
        return currentSong;
    }

    public BooleanProperty isPlayingProperty() {
        return isPlaying;
    }

    public DoubleProperty currentTimeProperty() {
        return currentTime;
    }

    public DoubleProperty totalDurationProperty() {
        return totalDuration;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public BooleanProperty isMutedProperty() {
        return isMuted;
    }

    public ObservableList<Song> getLibrarySongs() {
        return librarySongs;
    }

    public ObservableList<Playlist> getPlaylists() {
        return playlists;
    }

    public ObservableList<Song> getQueue() {
        return queue;
    }

    public ObservableList<Song> getFilteredQueue() {
        return filteredQueue;
    }

    public ObservableList<Song> getRecentlyPlayed() {
        return recentlyPlayed;
    }

    public IntegerProperty currentQueueIndexProperty() {
        return currentQueueIndex;
    }

    public ObjectProperty<Playlist> selectedPlaylistProperty() {
        return selectedPlaylist;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public ObservableList<Song> getSearchResults() {
        return searchResults;
    }

    /**
     * Finds the user's local full track for an online catalog result, when
     * available.
     */
    public Song findLocalTrack(String title, String artist) {
        String wantedTitle = normalizeTrackText(title);
        String wantedArtist = normalizeTrackText(artist);
        if (wantedTitle.isEmpty())
            return null;

        for (Song song : librarySongs) {
            if (!normalizeTrackText(song.getTitle()).equals(wantedTitle))
                continue;
            if (normalizeTrackText(song.getArtist()).equals(wantedArtist))
                return song;
        }
        return null;
    }

    private String normalizeTrackText(String value) {
        if (value == null)
            return "";
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s*\\([^)]*\\)|\\s*\\[[^]]*]", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    // Control Methods
    public void play(Song song) {
        if (song == null)
            return;

        stopExternalPlayback();

        // Add to recently played list
        Platform.runLater(() -> {
            recentlyPlayed.remove(song);
            recentlyPlayed.add(0, song);
            if (recentlyPlayed.size() > 20) {
                recentlyPlayed.remove(recentlyPlayed.size() - 1);
            }
        });

        // Online previews are standalone streams, so ending a preview must not jump
        // into
        // the user's local library queue.
        boolean onlinePreview = song.getPath().startsWith("http://") || song.getPath().startsWith("https://");
        if (onlinePreview) {
            queue.setAll(song);
            currentQueueIndex.set(0);
            // If the song is already in queue, find its index. Otherwise, set up the queue.
        } else {
            int idx = queue.indexOf(song);
            if (idx != -1) {
                currentQueueIndex.set(idx);
            } else {
                // Set queue to library songs starting from this song as fallback
                queue.setAll(librarySongs);
                currentQueueIndex.set(queue.indexOf(song));
            }
        }

        currentSong.set(song);
        updateThemeAndLyrics();
        audioEngine.play(song);
        // Update the filtered queue so that the current song appears first
        updateFilteredQueue();
    }

    public void playQueueIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            currentQueueIndex.set(index);
            Song song = queue.get(index);
            currentSong.set(song);
            updateThemeAndLyrics();
            audioEngine.play(song);
            // Keep filtered queue in sync
            updateFilteredQueue();
        }
    }

    public void togglePlayPause() {
        if (externalPlaybackActive) {
            if (isPlaying.get()) {
                externalPause.run();
                isPlaying.set(false);
            } else {
                externalResume.run();
                isPlaying.set(true);
            }
            return;
        }
        if (audioEngine.getState() == AudioEngine.PlaybackState.PLAYING) {
            audioEngine.pause();
            isPlaying.set(false);
        } else if (audioEngine.getState() == AudioEngine.PlaybackState.PAUSED) {
            audioEngine.resume();
            isPlaying.set(true);
        } else if (currentSong.get() != null) {
            audioEngine.play(currentSong.get());
            isPlaying.set(true);
        } else if (!librarySongs.isEmpty()) {
            play(librarySongs.get(0));
        }
    }

    public void next() {
        if (externalPlaybackActive) {
            seek(0);
            return;
        }
        if (repeatMode.get() == RepeatMode.ONE) {
            playQueueIndex(currentQueueIndex.get());
            return;
        }

        if (shuffleMode.get()) {
            if (!queue.isEmpty()) {
                int nextIndex = (int) (Math.random() * queue.size());
                playQueueIndex(nextIndex);
            }
            return;
        }

        int nextIndex = currentQueueIndex.get() + 1;
        if (nextIndex < queue.size()) {
            playQueueIndex(nextIndex);
        } else {
            if (repeatMode.get() == RepeatMode.ALL) {
                playQueueIndex(0);
            } else {
                audioEngine.stop();
                isPlaying.set(false);
            }
        }
        // Ensure filtered queue reflects new current song
        updateFilteredQueue();
    }

    public void previous() {
        if (externalPlaybackActive) {
            seek(0);
            return;
        }
        if (repeatMode.get() == RepeatMode.ONE) {
            playQueueIndex(currentQueueIndex.get());
            return;
        }

        if (shuffleMode.get()) {
            if (!queue.isEmpty()) {
                int nextIndex = (int) (Math.random() * queue.size());
                playQueueIndex(nextIndex);
            }
            return;
        }

        int prevIndex = currentQueueIndex.get() - 1;
        if (prevIndex >= 0) {
            playQueueIndex(prevIndex);
        } else {
            if (repeatMode.get() == RepeatMode.ALL) {
                playQueueIndex(queue.size() - 1);
            } else {
                playQueueIndex(0);
            }
        }
        // Sync filtered queue after seeking previous
        updateFilteredQueue();
    }

    public void seek(double seconds) {
        if (externalPlaybackActive) {
            externalSeek.accept(seconds);
            currentTime.set(Math.max(0, seconds));
            return;
        }
        audioEngine.seek(seconds);
    }

    public void startExternalPlayback(Song song, Runnable resume, Runnable pause, Runnable stop,
            Consumer<Double> seek) {
        stopExternalPlayback();
        // A provider-owned stream must never overlap the previously playing local file.
        audioEngine.stop();
        lyricsLines.clear();
        activeLyricLineIndex.set(-1);
        externalPlaybackActive = true;
        externalResume = resume;
        externalPause = pause;
        externalStop = stop;
        externalSeek = seek;
        queue.setAll(song);
        currentQueueIndex.set(0);
        currentSong.set(song);
        currentTime.set(0);
        totalDuration.set(0);
        isPlaying.set(true);
        updateFilteredQueue();
    }

    public void updateExternalPlayback(double position, double duration, boolean playing) {
        if (!externalPlaybackActive)
            return;
        currentTime.set(Math.max(0, position));
        if (duration > 0)
            totalDuration.set(duration);
        isPlaying.set(playing);
    }

    public void finishExternalPlayback(Song song) {
        if (externalPlaybackActive && currentSong.get() == song) {
            externalPlaybackActive = false;
            externalResume = externalPause = externalStop = null;
            externalSeek = null;
            isPlaying.set(false);
        }
    }

    private void stopExternalPlayback() {
        if (!externalPlaybackActive)
            return;
        Runnable stop = externalStop;
        externalPlaybackActive = false;
        externalResume = externalPause = externalStop = null;
        externalSeek = null;
        if (stop != null)
            stop.run();
    }

    public void performSearch(String query) {
        searchQuery.set(query);
        searchResults.setAll(libraryManager.search(query));
    }

    private void updateThemeAndLyrics() {
        Song song = currentSong.get();
        if (song == null)
            return;

        lyricsLines.clear();
        activeLyricLineIndex.set(-1);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            // 1. Update Theme from Album Art
            Image cachedArtwork = aura.music.library.ArtworkCache.getInstance().getArtwork(song);

            Platform.runLater(() -> {
                if (currentSong.get() != song)
                    return;
                themeEngine.updateThemeFromImage(cachedArtwork);
            });

            // 2. Load Lyrics STRICTLY from Local Offline Library
            // PRIORITY A: Check sidecar .lrc or .txt files on disk first (fresh from disk)
            java.util.List<LyricLine> sidecarParsed = findOfflineSidecarLyrics(song);
            if (!sidecarParsed.isEmpty()) {
                Platform.runLater(() -> {
                    if (currentSong.get() == song)
                        lyricsLines.setAll(sidecarParsed);
                });
                return; // done
            }

            // PRIORITY B: Check embedded lyrics in audio file metadata / cached DB
            String lrcContent = song.getEmbeddedLyrics();
            if (lrcContent == null || lrcContent.trim().isEmpty()) {
                Song freshMetadata = aura.music.library.MetadataExtractor.extract(new File(song.getPath()));
                if (freshMetadata != null && freshMetadata.getEmbeddedLyrics() != null) {
                    lrcContent = freshMetadata.getEmbeddedLyrics();
                    song.setEmbeddedLyrics(lrcContent);
                }
            }

            if (lrcContent != null && !lrcContent.trim().isEmpty()) {
                java.util.List<LyricLine> parsed = LyricParser.parse(lrcContent);
                if (!parsed.isEmpty()) {
                    Platform.runLater(() -> {
                        if (currentSong.get() == song)
                            lyricsLines.setAll(parsed);
                    });
                    return;
                }
            }
        }, backgroundExecutor);
    }

    private java.util.List<LyricLine> findOfflineSidecarLyrics(Song song) {
        String songPath = song.getPath();
        if (songPath == null || songPath.startsWith("http://") || songPath.startsWith("https://") || songPath.startsWith("youtube:")) {
            return java.util.Collections.emptyList();
        }
        File audioFile = new File(songPath);
        File parentDir = audioFile.getParentFile();
        if (parentDir == null || !parentDir.exists()) return java.util.Collections.emptyList();

        String baseName = audioFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = baseName.substring(0, dotIndex);
        }

        List<File> candidates = new ArrayList<>();
        // Same directory candidates
        candidates.add(new File(parentDir, baseName + ".lrc"));
        candidates.add(new File(parentDir, baseName + ".LRC"));
        candidates.add(new File(parentDir, baseName + ".txt"));
        candidates.add(new File(parentDir, baseName + ".TXT"));
        if (song.getTitle() != null && !song.getTitle().isBlank()) {
            candidates.add(new File(parentDir, song.getTitle() + ".lrc"));
            candidates.add(new File(parentDir, song.getTitle() + ".LRC"));
            candidates.add(new File(parentDir, song.getTitle() + ".txt"));
        }

        // Subfolders "lyrics" or "Lyrics"
        File[] subdirs = new File[] { new File(parentDir, "lyrics"), new File(parentDir, "Lyrics") };
        for (File subdir : subdirs) {
            if (subdir.exists() && subdir.isDirectory()) {
                candidates.add(new File(subdir, baseName + ".lrc"));
                candidates.add(new File(subdir, baseName + ".LRC"));
                candidates.add(new File(subdir, baseName + ".txt"));
                if (song.getTitle() != null && !song.getTitle().isBlank()) {
                    candidates.add(new File(subdir, song.getTitle() + ".lrc"));
                    candidates.add(new File(subdir, song.getTitle() + ".txt"));
                }
            }
        }

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                java.util.List<LyricLine> parsed = LyricParser.parse(file);
                if (!parsed.isEmpty()) return parsed;
            }
        }

        return java.util.Collections.emptyList();
    }

    private void updateActiveLyricIndex(double currentMs) {
        if (lyricsLines.isEmpty())
            return;

        double adjustedMs = currentMs + (lyricSyncOffsetSeconds.get() * 1000.0);

        int activeIdx = -1;
        for (int i = 0; i < lyricsLines.size(); i++) {
            long ts = lyricsLines.get(i).getTimestamp();
            if (ts >= 0 && adjustedMs >= ts) {
                activeIdx = i;
            } else if (ts >= 0 && adjustedMs < ts) {
                break;
            }
        }
        activeLyricLineIndex.set(activeIdx);
    }

    // Playlist Operations
    public void createPlaylist(String name) {
        Playlist p = new Playlist(UUID.randomUUID().toString(), name);
        libraryManager.getPlaylists().add(p);
        playlists.add(p);
        libraryManager.savePlaylists();
    }

    /**
     * Rebuilds the filteredQueue so that the currently playing song appears first,
     * followed by the upcoming songs in the queue.
     */
    public void updateFilteredQueue() {
        filteredQueue.clear();
        Song current = currentSong.get();
        if (current != null) {
            filteredQueue.add(current);
        }
        int startIdx = currentQueueIndex.get() + 1;
        for (int i = startIdx; i < queue.size(); i++) {
            filteredQueue.add(queue.get(i));
        }
    }

    // Listener registration performed in constructor
    {
        queue.addListener((javafx.collections.ListChangeListener<Song>) c -> updateFilteredQueue());
        currentSongProperty().addListener((obs, oldV, newV) -> updateFilteredQueue());
    }

    public void deletePlaylist(Playlist playlist) {
        libraryManager.getPlaylists().remove(playlist);
        playlists.remove(playlist);
        libraryManager.savePlaylists();
        if (selectedPlaylist.get() == playlist) {
            selectedPlaylist.set(null);
        }
    }

    private final IntegerProperty favoritesVersion = new SimpleIntegerProperty(0);

    public IntegerProperty favoritesVersionProperty() {
        return favoritesVersion;
    }

    public void toggleFavorite(Song song) {
        if (song == null)
            return;
        song.setFavorite(!song.isFavorite());
        aura.music.library.DatabaseManager.getInstance().insertOrUpdateSong(song);
        favoritesVersion.set(favoritesVersion.get() + 1);
    }

    private static class PlaybackState {
        String lastSongPath;
        double currentTime;
        java.util.List<String> queuePaths;
        int queueIndex;
        double volume;
        String repeatMode;
        boolean shuffleMode;
        java.util.List<String> recentlyPlayedPaths;
    }

    public void savePlaybackState() {
        try {
            String userHome = System.getProperty("user.home");
            String appDataPath = userHome + File.separator + ".auramusic";
            File stateFile = new File(appDataPath, "playback_state.json");

            PlaybackState state = new PlaybackState();
            if (currentSong.get() != null) {
                state.lastSongPath = currentSong.get().getPath();
            }
            state.currentTime = currentTime.get();
            state.queueIndex = currentQueueIndex.get();
            state.volume = volume.get();
            state.repeatMode = repeatMode.get().name();
            state.shuffleMode = shuffleMode.get();

            state.queuePaths = new java.util.ArrayList<>();
            for (Song s : queue) {
                state.queuePaths.add(s.getPath());
            }

            state.recentlyPlayedPaths = new java.util.ArrayList<>();
            for (Song s : recentlyPlayed) {
                state.recentlyPlayedPaths.add(s.getPath());
            }

            try (java.io.Writer writer = new java.io.FileWriter(stateFile)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(state, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save playback state: " + e.getMessage());
        }
    }

    public void loadPlaybackState() {
        try {
            String userHome = System.getProperty("user.home");
            String appDataPath = userHome + File.separator + ".auramusic";
            File stateFile = new File(appDataPath, "playback_state.json");
            if (!stateFile.exists()) {
                return;
            }

            PlaybackState state;
            try (java.io.Reader reader = new java.io.FileReader(stateFile)) {
                state = new com.google.gson.Gson().fromJson(reader, PlaybackState.class);
            }

            if (state == null) {
                return;
            }

            if (state.volume >= 0.0 && state.volume <= 1.0) {
                volume.set(state.volume);
            }
            if (state.repeatMode != null) {
                try {
                    repeatMode.set(RepeatMode.valueOf(state.repeatMode));
                } catch (Exception ignored) {
                }
            }
            shuffleMode.set(state.shuffleMode);

            if (state.recentlyPlayedPaths != null && !state.recentlyPlayedPaths.isEmpty()) {
                java.util.List<Song> loadedRecent = new java.util.ArrayList<>();
                for (String path : state.recentlyPlayedPaths) {
                    Song s = librarySongs.stream()
                            .filter(song -> song.getPath().equals(path))
                            .findFirst()
                            .orElse(null);
                    if (s != null) {
                        loadedRecent.add(s);
                    }
                }
                recentlyPlayed.setAll(loadedRecent);
            }

            if (state.queuePaths != null && !state.queuePaths.isEmpty()) {
                java.util.List<Song> restoredQueue = new java.util.ArrayList<>();
                for (String path : state.queuePaths) {
                    Song matched = librarySongs.stream()
                            .filter(s -> s.getPath().equals(path))
                            .findFirst()
                            .orElse(null);
                    if (matched != null) {
                        restoredQueue.add(matched);
                    }
                }
                if (!restoredQueue.isEmpty()) {
                    queue.setAll(restoredQueue);
                }
            }

            if (state.queueIndex >= 0 && state.queueIndex < queue.size()) {
                currentQueueIndex.set(state.queueIndex);
                Song matchedSong = queue.get(state.queueIndex);
                currentSong.set(matchedSong);

                audioEngine.prepare(matchedSong);
                audioEngine.setVolume(volume.get());
                if (state.currentTime > 0) {
                    currentTime.set(state.currentTime);
                    audioEngine.seek(state.currentTime);
                }
                updateFilteredQueue();
            } else if (state.lastSongPath != null) {
                Song matched = librarySongs.stream()
                        .filter(s -> s.getPath().equals(state.lastSongPath))
                        .findFirst()
                        .orElse(null);
                if (matched != null) {
                    currentSong.set(matched);
                    audioEngine.prepare(matched);
                    audioEngine.setVolume(volume.get());
                    if (state.currentTime > 0) {
                        currentTime.set(state.currentTime);
                        audioEngine.seek(state.currentTime);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load playback state: " + e.getMessage());
        }
    }

    public void startSleepTimer(int minutes) {
        cancelSleepTimer();
        sleepTimer = new PauseTransition(Duration.minutes(minutes));
        sleepTimer.setOnFinished(e -> {
            if (audioEngine.getState() == AudioEngine.PlaybackState.PLAYING) {
                audioEngine.pause();
                isPlaying.set(false);
            }
        });
        sleepTimer.play();
    }

    public void cancelSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.stop();
            sleepTimer = null;
        }
    }
}
