package aura.music.audio;

import aura.music.model.Song;
import javafx.util.Duration;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AudioEngine {
    private static AudioEngine instance;

    private MediaPlayerFactory mediaPlayerFactory;
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private double volume = 0.8;
    private boolean muted = false;

    // Listeners
    private final List<Runnable> onEndOfMediaListeners = new ArrayList<>();
    private final List<Consumer<Duration>> onReadyListeners = new ArrayList<>();
    private final List<Consumer<Duration>> onProgressListeners = new ArrayList<>();
    // Note: VLCJ doesn't have a direct equivalent to JavaFX AudioSpectrumListener that works out-of-the-box in Java
    private final List<Consumer<float[]>> onSpectrumListeners = new ArrayList<>();

    private PlaybackState state = PlaybackState.STOPPED;

    public enum PlaybackState {
        PLAYING, PAUSED, STOPPED
    }

    private AudioEngine() {
        try {
            try {
                // Production environment: vlc folder alongside the jar inside 'lib' or 'app' folder
                java.io.File jarDir = new java.io.File(AudioEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                java.io.File vlcDir = new java.io.File(jarDir, "vlc");
                
                // Development environment: vlc folder in project root
                java.io.File devVlcDir = new java.io.File(System.getProperty("user.dir"), "vlc");
                
                java.io.File activeDir = vlcDir.exists() ? vlcDir : (devVlcDir.exists() ? devVlcDir : null);
                
                if (activeDir != null) {
                    com.sun.jna.NativeLibrary.addSearchPath("libvlc", activeDir.getAbsolutePath());
                    com.sun.jna.NativeLibrary.addSearchPath("libvlccore", activeDir.getAbsolutePath());
                }
            } catch (Exception ignored) {
                // If path resolution fails, VLCJ will fallback to system paths
            }

            mediaPlayerFactory = new MediaPlayerFactory(
                "--avcodec-hw=any",
                "--no-video",
                "--no-sub-autodetect-file",
                "--audio-resampler=soxr"
            );
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer();

            mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void playing(MediaPlayer mediaPlayer) {
                    state = PlaybackState.PLAYING;
                }

                @Override
                public void paused(MediaPlayer mediaPlayer) {
                    state = PlaybackState.PAUSED;
                }

                @Override
                public void stopped(MediaPlayer mediaPlayer) {
                    state = PlaybackState.STOPPED;
                }

                @Override
                public void finished(MediaPlayer mediaPlayer) {
                    state = PlaybackState.STOPPED;
                    for (Runnable listener : onEndOfMediaListeners) {
                        listener.run();
                    }
                }

                @Override
                public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                    Duration duration = Duration.millis(newTime);
                    for (Consumer<Duration> listener : onProgressListeners) {
                        listener.accept(duration);
                    }
                }
                
                @Override
                public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                    if (newLength > 0) {
                        Duration duration = Duration.millis(newLength);
                        for (Consumer<Duration> listener : onReadyListeners) {
                            listener.accept(duration);
                        }
                    }
                }
            });
            
            // Apply initial volume
            mediaPlayer.audio().setVolume((int) (volume * 100));
        } catch (Exception e) {
            System.err.println("Failed to initialize VLCJ AudioPlayerComponent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized AudioEngine getInstance() {
        if (instance == null) {
            instance = new AudioEngine();
        }
        return instance;
    }

    public void play(Song song) {
        if (song == null || mediaPlayer == null) return;

        stop();
        currentSong = song;

        try {
            boolean remoteStream = song.getPath().startsWith("http://") || song.getPath().startsWith("https://");
            if (!remoteStream && !new File(song.getPath()).exists()) {
                throw new java.io.FileNotFoundException("File does not exist: " + song.getPath());
            }

            mediaPlayer.audio().setVolume(muted ? 0 : (int) (volume * 100));
            mediaPlayer.media().play(song.getPath());
            state = PlaybackState.PLAYING;

        } catch (Exception e) {
            System.err.println("Error playing song with VLCJ: " + e.getMessage());
            state = PlaybackState.STOPPED;
        }
    }

    public void prepare(Song song) {
        if (song == null || mediaPlayer == null) return;

        stop();
        currentSong = song;

        try {
            if (!new File(song.getPath()).exists()) {
                throw new java.io.FileNotFoundException("File does not exist: " + song.getPath());
            }

            mediaPlayer.audio().setVolume(muted ? 0 : (int) (volume * 100));
            mediaPlayer.media().prepare(song.getPath());
            state = PlaybackState.PAUSED;

        } catch (Exception e) {
            System.err.println("Error preparing song with VLCJ: " + e.getMessage());
            state = PlaybackState.STOPPED;
        }
    }

    public void pause() {
        if (mediaPlayer != null && state == PlaybackState.PLAYING) {
            mediaPlayer.controls().pause();
            state = PlaybackState.PAUSED;
        }
    }

    public void resume() {
        if (mediaPlayer != null && state == PlaybackState.PAUSED) {
            mediaPlayer.controls().play();
            state = PlaybackState.PLAYING;
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
        }
        state = PlaybackState.STOPPED;
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.controls().setTime((long) (seconds * 1000));
        }
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (mediaPlayer != null && !muted) {
            mediaPlayer.audio().setVolume((int) (this.volume * 100));
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (mediaPlayer != null) {
            mediaPlayer.audio().setVolume(muted ? 0 : (int) (volume * 100));
        }
    }

    public PlaybackState getState() {
        return state;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public Duration getCurrentTime() {
        return mediaPlayer != null ? Duration.millis(mediaPlayer.status().time()) : Duration.ZERO;
    }

    public Duration getTotalDuration() {
        return mediaPlayer != null ? Duration.millis(mediaPlayer.status().length()) : Duration.ZERO;
    }

    public void setEqBand(int bandIndex, double gainDb) {
        // Equalizer not implemented in this basic VLCJ migration
    }

    // Listener Registration
    public void addOnEndOfMedia(Runnable listener) {
        onEndOfMediaListeners.add(listener);
    }

    public void addOnReady(Consumer<Duration> listener) {
        onReadyListeners.add(listener);
    }

    public void addOnProgress(Consumer<Duration> listener) {
        onProgressListeners.add(listener);
    }

    public void addOnSpectrum(Consumer<float[]> listener) {
        onSpectrumListeners.add(listener);
    }

    public void clearListeners() {
        onEndOfMediaListeners.clear();
        onReadyListeners.clear();
        onProgressListeners.clear();
        onSpectrumListeners.clear();
    }
}
