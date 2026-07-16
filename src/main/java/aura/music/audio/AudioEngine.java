package aura.music.audio;

import aura.music.model.Song;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AudioEngine {
    private static AudioEngine instance;

    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private double volume = 0.8;
    private boolean muted = false;
    private File tempWavFile;

    // EQ Bands
    private final double[] eqGains = new double[10]; // default 0.0 dB

    // Listeners
    private final List<Runnable> onEndOfMediaListeners = new ArrayList<>();
    private final List<Consumer<Duration>> onReadyListeners = new ArrayList<>();
    private final List<Consumer<Duration>> onProgressListeners = new ArrayList<>();
    private final List<Consumer<float[]>> onSpectrumListeners = new ArrayList<>();

    private PlaybackState state = PlaybackState.STOPPED;

    public enum PlaybackState {
        PLAYING, PAUSED, STOPPED
    }

    private AudioEngine() {
        // Initialize default EQ gains to 0.0
        for (int i = 0; i < 10; i++) {
            eqGains[i] = 0.0;
        }
    }

    public static synchronized AudioEngine getInstance() {
        if (instance == null) {
            instance = new AudioEngine();
        }
        return instance;
    }

    public void play(Song song) {
        if (song == null)
            return;

        stop();
        currentSong = song;

        try {
            File inputFile = new File(song.getPath());
            if (!inputFile.exists()) {
                throw new java.io.FileNotFoundException("File does not exist: " + song.getPath());
            }

            // JavaFX Media requires a URI.
            String uriString;
            String pathLower = song.getPath().toLowerCase();

            // Decode any format other than native MP3/WAV to a temporary WAV using FFmpeg
            if (!pathLower.endsWith(".mp3") && !pathLower.endsWith(".wav")) {
                tempWavFile = decodeWithFFmpeg(inputFile);
                uriString = tempWavFile.toURI().toString();
            } else {
                uriString = inputFile.toURI().toString();
            }

            Media media = new Media(uriString);
            mediaPlayer = new MediaPlayer(media);

            // Apply volume & mute
            mediaPlayer.setVolume(muted ? 0.0 : volume);

            // Apply current EQ settings
            applyEqualizerSettings();

            // Set up listeners
            mediaPlayer.setOnReady(() -> {
                state = PlaybackState.PLAYING;
                Duration duration = mediaPlayer.getMedia().getDuration();
                for (Consumer<Duration> listener : onReadyListeners) {
                    listener.accept(duration);
                }
                mediaPlayer.play();
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                state = PlaybackState.STOPPED;
                for (Runnable listener : onEndOfMediaListeners) {
                    listener.run();
                }
            });

            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                for (Consumer<Duration> listener : onProgressListeners) {
                    listener.accept(newValue);
                }
            });

            // GPU-accelerated audio spectrum listener for the visualizer
            mediaPlayer.setAudioSpectrumInterval(0.016); // ~60 FPS
            mediaPlayer.setAudioSpectrumNumBands(64); // 64 bands
            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                if (state == PlaybackState.PLAYING) {
                    for (Consumer<float[]> listener : onSpectrumListeners) {
                        listener.accept(magnitudes);
                    }
                }
            });
            
            mediaPlayer.setOnError(() -> {
                System.err.println("MediaPlayer error: " + mediaPlayer.getError());
                state = PlaybackState.STOPPED;
                // DO NOT call onEndOfMediaListeners here to avoid infinite loops
            });

            mediaPlayer.play();
            state = PlaybackState.PLAYING;

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
            e.printStackTrace();
            state = PlaybackState.STOPPED;
            // DO NOT call onEndOfMediaListeners here to avoid infinite loop when many files are missing
        }
    }

    public void prepare(Song song) {
        if (song == null)
            return;

        stop();
        currentSong = song;

        try {
            File inputFile = new File(song.getPath());
            if (!inputFile.exists()) {
                throw new java.io.FileNotFoundException("File does not exist: " + song.getPath());
            }

            String uriString;
            String pathLower = song.getPath().toLowerCase();

            if (!pathLower.endsWith(".mp3") && !pathLower.endsWith(".wav")) {
                tempWavFile = decodeWithFFmpeg(new File(song.getPath()));
                uriString = tempWavFile.toURI().toString();
            } else {
                uriString = new File(song.getPath()).toURI().toString();
            }

            Media media = new Media(uriString);
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setVolume(muted ? 0.0 : volume);
            applyEqualizerSettings();

            mediaPlayer.setOnReady(() -> {
                state = PlaybackState.PAUSED;
                Duration duration = mediaPlayer.getMedia().getDuration();
                for (Consumer<Duration> listener : onReadyListeners) {
                    listener.accept(duration);
                }
                mediaPlayer.pause();
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                state = PlaybackState.STOPPED;
                for (Runnable listener : onEndOfMediaListeners) {
                    listener.run();
                }
            });

            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                for (Consumer<Duration> listener : onProgressListeners) {
                    listener.accept(newValue);
                }
            });

            // GPU-accelerated audio spectrum listener for the visualizer
            mediaPlayer.setAudioSpectrumInterval(0.016);
            mediaPlayer.setAudioSpectrumNumBands(64);
            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                if (state == PlaybackState.PLAYING) {
                    for (Consumer<float[]> listener : onSpectrumListeners) {
                        listener.accept(magnitudes);
                    }
                }
            });

            mediaPlayer.pause();
            state = PlaybackState.PAUSED;

        } catch (Exception e) {
            System.err.println("Error preparing song: " + e.getMessage());
            e.printStackTrace();
            state = PlaybackState.STOPPED;
            for (Runnable listener : onEndOfMediaListeners) {
                listener.run();
            }
        }
    }

    private File decodeWithFFmpeg(File inputFile) throws Exception {
        File tempFile = File.createTempFile("aura_playback_", ".wav");
        tempFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-loglevel", "error",
                "-i", inputFile.getAbsolutePath(),
                "-map_metadata", "-1", // Strip all metadata (prevents JavaFX from thinking it's compressed WAV)
                "-fflags", "+bitexact", // Write minimal, standard WAV headers
                "-acodec", "pcm_s16le", // Uncompressed 16-bit PCM
                "-ar", "44100", // Standard 44.1kHz sample rate
                "-ac", "2", // Stereo channels
                "-f", "wav",
                tempFile.getAbsolutePath());

        Process process = pb.start();

        // Read error stream
        StringBuilder errorOutput = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        // Wait for the process to complete with a 15-second timeout to prevent hangs
        boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg decoding timed out.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed to decode file. Exit code: " + exitCode + "\nError output:\n"
                    + errorOutput.toString().trim());
        }
        return tempFile;
    }

    public void pause() {
        if (mediaPlayer != null && state == PlaybackState.PLAYING) {
            mediaPlayer.pause();
            state = PlaybackState.PAUSED;
        }
    }

    public void resume() {
        if (mediaPlayer != null && state == PlaybackState.PAUSED) {
            mediaPlayer.play();
            state = PlaybackState.PLAYING;
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        state = PlaybackState.STOPPED;

        // Clean up temporary WAV file
        if (tempWavFile != null && tempWavFile.exists()) {
            tempWavFile.delete();
            tempWavFile = null;
        }
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
        }
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (mediaPlayer != null && !muted) {
            mediaPlayer.setVolume(this.volume);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(muted ? 0.0 : volume);
        }
    }

    public PlaybackState getState() {
        return state;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public Duration getCurrentTime() {
        return mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO;
    }

    public Duration getTotalDuration() {
        return (mediaPlayer != null && mediaPlayer.getMedia() != null)
                ? mediaPlayer.getMedia().getDuration()
                : Duration.ZERO;
    }

    // Equalizer Controls
    public void setEqBand(int bandIndex, double gainDb) {
        if (bandIndex >= 0 && bandIndex < 10) {
            eqGains[bandIndex] = Math.max(-24.0, Math.min(12.0, gainDb));
            if (mediaPlayer != null) {
                AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
                if (eq != null && eq.getBands().size() > bandIndex) {
                    eq.getBands().get(bandIndex).setGain(eqGains[bandIndex]);
                }
            }
        }
    }

    private void applyEqualizerSettings() {
        if (mediaPlayer == null)
            return;
        AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
        if (eq != null) {
            eq.setEnabled(true);
            // JavaFX provides 10 bands by default: 32, 64, 125, 250, 500, 1k, 2k, 4k, 8k,
            // 16k Hz
            List<EqualizerBand> bands = eq.getBands();
            for (int i = 0; i < Math.min(bands.size(), 10); i++) {
                bands.get(i).setGain(eqGains[i]);
            }
        }
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
