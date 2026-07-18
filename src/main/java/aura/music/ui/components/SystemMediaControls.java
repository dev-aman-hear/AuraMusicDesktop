package aura.music.ui.components;

import aura.music.model.Song;
import aura.music.viewmodel.MainViewModel;
import io.github.selemba1000.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemMediaControls {

    private JMTC jmtc;
    private final MainViewModel viewModel;
    // Use a single thread executor to ensure all COM calls happen on the same thread.
    private final ExecutorService smtcThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("SMTC-Thread");
        return t;
    });

    public SystemMediaControls(MainViewModel viewModel) {
        this.viewModel = viewModel;
        initializeJMTC();
    }

    private void initializeJMTC() {
        smtcThread.submit(() -> {
            try {
                JMTCSettings settings = new JMTCSettings("Aura Music", "aura-music");
                jmtc = JMTC.getInstance(settings);
                
                // Enable SMTC on Windows
                jmtc.setEnabled(true);
                
                // Set enabled buttons
                jmtc.setEnabledButtons(new JMTCEnabledButtons(true, true, true, true, true));

                // Set Callbacks
                JMTCCallbacks callbacks = new JMTCCallbacks();
                callbacks.onPlay = () -> Platform.runLater(viewModel::togglePlayPause);
                callbacks.onPause = () -> Platform.runLater(viewModel::togglePlayPause);
                callbacks.onStop = () -> Platform.runLater(() -> {
                    if (viewModel.isPlayingProperty().get()) {
                        viewModel.togglePlayPause();
                    }
                });
                callbacks.onNext = () -> Platform.runLater(viewModel::next);
                callbacks.onPrevious = () -> Platform.runLater(viewModel::previous);
                
                jmtc.setCallbacks(callbacks);

                // Listener for song changes
                Platform.runLater(() -> {
                    viewModel.currentSongProperty().addListener(new ChangeListener<Song>() {
                        @Override
                        public void changed(ObservableValue<? extends Song> observable, Song oldValue, Song newSong) {
                            if (newSong != null) {
                                updateMetadata(newSong);
                            }
                        }
                    });
                    
                    // Listener for play state changes
                    viewModel.isPlayingProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isPlaying) {
                            if (isPlaying != null) {
                                updatePlaybackState(isPlaying);
                            }
                        }
                    });
                    
                    if (viewModel.currentSongProperty().get() != null) {
                        updateMetadata(viewModel.currentSongProperty().get());
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to initialize JMTC: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updatePlaybackState(boolean isPlaying) {
        smtcThread.submit(() -> {
            if (jmtc == null) return;
            try {
                if (isPlaying) {
                    jmtc.setPlayingState(JMTCPlayingState.PLAYING);
                } else {
                    jmtc.setPlayingState(JMTCPlayingState.PAUSED);
                }
                jmtc.updateDisplay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateMetadata(Song song) {
        smtcThread.submit(() -> {
            if (jmtc == null) return;
            try {
                String title = song.getTitle();
                if (title == null || title.isEmpty()) {
                    title = new File(song.getPath()).getName();
                }
                
                String artist = song.getArtist();
                if (artist == null) artist = "Unknown Artist";
                
                String album = song.getAlbum();
                if (album == null) album = "Unknown Album";
                
                // Attempt to get cached cover art or extract it
                File artFile = null;
                try {
                    byte[] artworkBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
                    if (artworkBytes != null && artworkBytes.length > 0) {
                        File tempArtFile = File.createTempFile("smtc_art_", ".jpg");
                        tempArtFile.deleteOnExit();
                        java.nio.file.Files.write(tempArtFile.toPath(), artworkBytes);
                        artFile = tempArtFile;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to extract SMTC artwork: " + e.getMessage());
                }

                String genre = song.getGenre();
                if (genre == null) genre = "Unknown";
                JMTCMusicProperties props = new JMTCMusicProperties(
                        title, artist, album, artist, new String[]{genre}, 1, 1, artFile
                );
                
                jmtc.setMediaType(JMTCMediaType.Music);
                jmtc.setMediaProperties(props);
                
                if (viewModel.isPlayingProperty().get()) {
                    jmtc.setPlayingState(JMTCPlayingState.PLAYING);
                } else {
                    jmtc.setPlayingState(JMTCPlayingState.PAUSED);
                }
                
                jmtc.updateDisplay();
            } catch (Exception e) {
                 System.err.println("Failed to update SMTC metadata: " + e.getMessage());
            }
        });
    }
}
