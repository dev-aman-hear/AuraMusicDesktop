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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.UUID;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class MainViewModel {

    private final AudioEngine audioEngine = AudioEngine.getInstance();
    private final LibraryManager libraryManager = LibraryManager.getInstance();
    private final ThemeEngine themeEngine = ThemeEngine.getInstance();
    private PauseTransition sleepTimer;

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

    public BooleanProperty shuffleModeProperty() { return shuffleMode; }
    public ObjectProperty<RepeatMode> repeatModeProperty() { return repeatMode; }

    private final BooleanProperty miniplayerAlwaysOnTop = new SimpleBooleanProperty(true);
    public BooleanProperty miniplayerAlwaysOnTopProperty() { return miniplayerAlwaysOnTop; }

    // Lists
    private final ObservableList<Song> librarySongs = FXCollections.observableArrayList();
    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private final ObservableList<Song> queue = FXCollections.observableArrayList();
    private final ObservableList<Song> recentlyPlayed = FXCollections.observableArrayList();
    private final IntegerProperty currentQueueIndex = new SimpleIntegerProperty(-1);
    // Filtered view of the queue that always places the currently playing song first
    private final ObservableList<Song> filteredQueue = FXCollections.observableArrayList();

    // Lyrics
    private final ObservableList<LyricLine> lyricsLines = FXCollections.observableArrayList();
    private final IntegerProperty activeLyricLineIndex = new SimpleIntegerProperty(-1);
    private final IntegerProperty lyricTextSize = new SimpleIntegerProperty(26);

    public ObservableList<LyricLine> getLyricsLines() { return lyricsLines; }
    public IntegerProperty activeLyricLineIndexProperty() { return activeLyricLineIndex; }
    public IntegerProperty lyricTextSizeProperty() { return lyricTextSize; }

    // Navigation/Selection
    private final ObjectProperty<Playlist> selectedPlaylist = new SimpleObjectProperty<>();
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final ObservableList<Song> searchResults = FXCollections.observableArrayList();

    public MainViewModel() {
        // Initialize from LibraryManager
        librarySongs.addAll(libraryManager.getSongs());
        playlists.addAll(libraryManager.getPlaylists());

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

        libraryManager.loadSettings(); // Triggers loading folders and scanning

        // Bind volume
        volume.addListener((obs, oldVal, newVal) -> audioEngine.setVolume(newVal.doubleValue()));
        isMuted.addListener((obs, oldVal, newVal) -> audioEngine.setMuted(newVal));

        // Connect AudioEngine listeners
        audioEngine.addOnReady(duration -> Platform.runLater(() -> {
            totalDuration.set(duration.toSeconds());
            isPlaying.set(true);
            updateThemeAndLyrics();
        }));

        audioEngine.addOnProgress(duration -> Platform.runLater(() -> {
            currentTime.set(duration.toSeconds());
            updateActiveLyricIndex(duration.toMillis());
        }));

        audioEngine.addOnEndOfMedia(() -> Platform.runLater(this::next));

        // Sync initial volume
        volume.set(audioEngine.getVolume());

        Platform.runLater(this::loadPlaybackState);
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

    // Control Methods
    public void play(Song song) {
        if (song == null)
            return;

        // Add to recently played list
        Platform.runLater(() -> {
            recentlyPlayed.remove(song);
            recentlyPlayed.add(0, song);
            if (recentlyPlayed.size() > 20) {
                recentlyPlayed.remove(recentlyPlayed.size() - 1);
            }
        });

        // If the song is already in queue, find its index. Otherwise, set up the queue.
        int idx = queue.indexOf(song);
        if (idx != -1) {
            currentQueueIndex.set(idx);
        } else {
            // Set queue to library songs starting from this song as fallback
            queue.setAll(librarySongs);
            currentQueueIndex.set(queue.indexOf(song));
        }

        currentSong.set(song);
        audioEngine.play(song);
        // Update the filtered queue so that the current song appears first
        updateFilteredQueue();
    }

    public void playQueueIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            currentQueueIndex.set(index);
            Song song = queue.get(index);
            currentSong.set(song);
            audioEngine.play(song);
            // Keep filtered queue in sync
            updateFilteredQueue();
        }
    }

    public void togglePlayPause() {
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
        audioEngine.seek(seconds);
    }

    public void performSearch(String query) {
        searchQuery.set(query);
        searchResults.setAll(libraryManager.search(query));
    }

    private void updateThemeAndLyrics() {
        Song song = currentSong.get();
        if (song == null)
            return;

        // 1. Update Theme from Album Art
        byte[] artworkBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
        if (artworkBytes != null) {
            Image image = new Image(new ByteArrayInputStream(artworkBytes));
            themeEngine.updateThemeFromImage(image);
        } else {
            themeEngine.updateThemeFromImage(null);
        }

        // 2. Load and Parse Lyrics
        lyricsLines.clear();
        activeLyricLineIndex.set(-1);

        // Try embedded lyrics first
        String lrcContent = song.getEmbeddedLyrics();
        if (lrcContent != null && !lrcContent.trim().isEmpty()) {
            lyricsLines.addAll(LyricParser.parse(lrcContent));
        }

        // Try external .lrc file in the same directory if no embedded lyrics were
        // successfully parsed
        if (lyricsLines.isEmpty()) {
            String songPath = song.getPath();
            int dotIndex = songPath.lastIndexOf('.');
            if (dotIndex != -1) {
                String lrcPath = songPath.substring(0, dotIndex) + ".lrc";
                File lrcFile = new File(lrcPath);
                if (lrcFile.exists()) {
                    lyricsLines.addAll(LyricParser.parse(lrcFile));
                }
            }
        }

        // Try fetching online if still empty
        if (lyricsLines.isEmpty()) {
            aura.music.lyrics.OnlineLyricsService.fetchLyricsAsync(song.getTitle(), song.getArtist(), song.getAlbum())
                .thenAccept(lyrics -> {
                    if (lyrics != null && !lyrics.isEmpty() && currentSong.get() == song) {
                        javafx.application.Platform.runLater(() -> {
                            lyricsLines.addAll(LyricParser.parse(lyrics));
                        });
                    }
                });
        }
    }

    private void updateActiveLyricIndex(double currentMs) {
        if (lyricsLines.isEmpty())
            return;

        int activeIdx = -1;
        for (int i = 0; i < lyricsLines.size(); i++) {
            if (currentMs >= lyricsLines.get(i).getTimestamp()) {
                activeIdx = i;
            } else {
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
        libraryManager.saveLibraryToCache();
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
                } catch (Exception ignored) {}
            }
            shuffleMode.set(state.shuffleMode);

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
