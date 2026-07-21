package aura.music.library;

import aura.music.model.Song;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ArtworkCache {
    private static ArtworkCache instance;
    private final String cacheDir;
    private final ExecutorService artworkExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("AuraMusic-Artwork-Worker");
        return t;
    });
    
    // Tier 1: In-Memory LRU Cache (max 20 images)
    private final Map<String, Image> memoryCache = new LinkedHashMap<String, Image>(20, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 20;
        }
    };

    private ArtworkCache() {
        String userHome = System.getProperty("user.home");
        cacheDir = userHome + File.separator + ".auramusic" + File.separator + "art";
        new File(cacheDir).mkdirs();
    }

    public static synchronized ArtworkCache getInstance() {
        if (instance == null) {
            instance = new ArtworkCache();
        }
        return instance;
    }

    /**
     * Retrieves the artwork. Hits the memory cache first, then the disk cache thumbnail.
     * If not found on disk, it returns null. (Extraction should happen during library scan).
     */
    public Image getArtwork(Song song) {
        if (song.getArtworkUrl() != null && !song.getArtworkUrl().isBlank()) {
            return new Image(song.getArtworkUrl(), true);
        }

        String id = getSongIdentifier(song);
        
        // 1. Check Memory Cache
        synchronized (memoryCache) {
            if (memoryCache.containsKey(id)) {
                return memoryCache.get(id);
            }
        }

        // 2. Check Disk Cache
        File diskFile = new File(cacheDir, id + ".jpg");
        if (diskFile.exists()) {
            Image img = new Image(diskFile.toURI().toString());
            synchronized (memoryCache) {
                memoryCache.put(id, img);
            }
            return img;
        }

        return null; // Not found (either missing or not yet extracted)
    }

    /**
     * Fast asynchronous artwork loader.
     * Checks memory & disk thumbnail cache first.
     * If missing, extracts thumbnail in background executor without blocking the UI thread.
     */
    public void getArtworkAsync(Song song, Consumer<Image> callback) {
        if (song == null) {
            callback.accept(null);
            return;
        }

        Image cached = getArtwork(song);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        artworkExecutor.submit(() -> {
            extractAndCacheThumbnail(song);
            Image loaded = getArtwork(song);
            javafx.application.Platform.runLater(() -> callback.accept(loaded));
        });
    }

    /**
     * Extracts artwork from the audio file, scales it to 300x300, and saves to disk cache.
     * This is designed to be called sequentially during a background scan.
     */
    public void extractAndCacheThumbnail(Song song) {
        if (song.getArtworkUrl() != null && !song.getArtworkUrl().isBlank()) {
            return; // Uses online URL
        }

        String id = getSongIdentifier(song);
        File diskFile = new File(cacheDir, id + ".jpg");
        
        if (diskFile.exists()) {
            return; // Already cached
        }

        byte[] rawBytes = MetadataExtractor.extractArtworkBytes(song.getPath());
        if (rawBytes != null && rawBytes.length > 0) {
            try {
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(rawBytes));
                if (original != null) {
                    BufferedImage thumbnail = createThumbnail(original, 300, 300);
                    // Write as JPEG
                    ImageIO.write(thumbnail, "jpg", diskFile);
                }
            } catch (Exception e) {
                System.err.println("Failed to create artwork thumbnail for: " + song.getPath());
            }
        }
    }

    private BufferedImage createThumbnail(BufferedImage original, int width, int height) {
        // Calculate aspect-preserving dimensions
        double scale = Math.min((double) width / original.getWidth(), (double) height / original.getHeight());
        if (scale >= 1.0) {
            // Already smaller than thumbnail bounds, convert to RGB if needed (JPEG requires no alpha)
            BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();
            return rgb;
        }

        int scaledW = (int) (original.getWidth() * scale);
        int scaledH = (int) (original.getHeight() * scale);

        BufferedImage resized = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        // High quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, scaledW, scaledH, null);
        g2d.dispose();
        
        return resized;
    }

    private String getSongIdentifier(Song song) {
        // A simple hash based on file path and last modified time to ensure updates refresh the cache
        return Integer.toHexString((song.getPath() + "_" + song.getLastModified()).hashCode());
    }
}
