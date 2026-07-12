package aura.music.utils;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    
    // Using SoftReference so the JVM can garbage collect images if RAM gets full
    private static final Map<String, SoftReference<Image>> cache = new ConcurrentHashMap<>();

    public static Image getImage(String albumIdentifier, byte[] imageBytes, int requestedWidth, int requestedHeight) {
        if (albumIdentifier == null || imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        SoftReference<Image> ref = cache.get(albumIdentifier);
        Image image = (ref != null) ? ref.get() : null;

        if (image == null) {
            // Image was garbage collected or not cached yet, create it
            image = new Image(new ByteArrayInputStream(imageBytes), requestedWidth, requestedHeight, true, true);
            cache.put(albumIdentifier, new SoftReference<>(image));
        }

        return image;
    }
    
    public static Image getCachedImage(String albumIdentifier) {
        if (albumIdentifier == null) return null;
        SoftReference<Image> ref = cache.get(albumIdentifier);
        return (ref != null) ? ref.get() : null;
    }
    
    public static void clear() {
        cache.clear();
    }
}
