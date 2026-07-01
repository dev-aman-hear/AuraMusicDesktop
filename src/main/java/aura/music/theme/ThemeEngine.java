package aura.music.theme;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;

public class ThemeEngine {
    private static ThemeEngine instance;

    // Observable color properties for View bindings
    private final ObjectProperty<Color> primaryColor = new SimpleObjectProperty<>(Color.web("#ffffff"));
    private final ObjectProperty<Color> secondaryColor = new SimpleObjectProperty<>(Color.web("#b3b3b3"));
    private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web("#0078d4"));
    private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(Color.web("#121212"));
    private final ObjectProperty<Color> sidebarColor = new SimpleObjectProperty<>(Color.web("#1c1c1c"));

    private final javafx.beans.property.BooleanProperty dynamicColoring = new javafx.beans.property.SimpleBooleanProperty(true);
    private final javafx.beans.property.BooleanProperty lightMode = new javafx.beans.property.SimpleBooleanProperty(false);

    private Timeline themeAnimation;

    private ThemeEngine() {}

    public static synchronized ThemeEngine getInstance() {
        if (instance == null) {
            instance = new ThemeEngine();
        }
        return instance;
    }

    public ObjectProperty<Color> primaryColorProperty() { return primaryColor; }
    public ObjectProperty<Color> secondaryColorProperty() { return secondaryColor; }
    public ObjectProperty<Color> accentColorProperty() { return accentColor; }
    public ObjectProperty<Color> backgroundColorProperty() { return backgroundColor; }
    public ObjectProperty<Color> sidebarColorProperty() { return sidebarColor; }
    public javafx.beans.property.BooleanProperty dynamicColoringProperty() { return dynamicColoring; }
    public javafx.beans.property.BooleanProperty lightModeProperty() { return lightMode; }

    public void updateThemeFromImage(Image image) {
        if (!dynamicColoring.get()) {
            applyDefaultStaticTheme();
            return;
        }

        if (image == null || image.isError()) {
            applyDefaultTheme();
            return;
        }

        // Run extraction
        Palette palette = extractPalette(image);

        // Animate color transition over 400ms
        animateToPalette(palette);
    }

    public void applyDefaultStaticTheme() {
        if (lightMode.get()) {
            animateToPalette(new Palette(
                    Color.BLACK,
                    Color.BLACK,
                    Color.web("#ff2d55"),
                    Color.web("#f2f2f7"),
                    Color.web("#ffffff")
            ));
        } else {
            animateToPalette(new Palette(
                    Color.WHITE,
                    Color.WHITE,
                    Color.web("#0078d4"),
                    Color.web("#121212"),
                    Color.web("#1c1c1c")
            ));
        }
    }

    private void applyDefaultTheme() {
        if (!dynamicColoring.get()) {
            applyDefaultStaticTheme();
            return;
        }
        animateToPalette(new Palette(
                Color.WHITE,
                Color.WHITE,
                Color.web("#0078d4"),
                Color.web("#121212"),
                Color.web("#1c1c1c")
        ));
    }

    private void animateToPalette(Palette target) {
        if (themeAnimation != null) {
            themeAnimation.stop();
        }

        themeAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(primaryColor, primaryColor.get()),
                        new KeyValue(secondaryColor, secondaryColor.get()),
                        new KeyValue(accentColor, accentColor.get()),
                        new KeyValue(backgroundColor, backgroundColor.get()),
                        new KeyValue(sidebarColor, sidebarColor.get())
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(primaryColor, target.primary),
                        new KeyValue(secondaryColor, target.secondary),
                        new KeyValue(accentColor, target.accent),
                        new KeyValue(backgroundColor, target.background),
                        new KeyValue(sidebarColor, target.sidebar)
                )
        );
        themeAnimation.play();
    }

    private Palette extractPalette(Image image) {
        // Downsample image to 16x16 for performance and noise reduction
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        if (reader == null) return new Palette(Color.WHITE, Color.GRAY, Color.BLUE, Color.BLACK, Color.BLACK);

        List<Color> pixels = new ArrayList<>();
        int stepX = Math.max(1, w / 16);
        int stepY = Math.max(1, h / 16);

        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                pixels.add(reader.getColor(x, y));
            }
        }

        // Count color occurrences (approximate colors by rounding HSL/RGB)
        Map<Color, Integer> colorCounts = new HashMap<>();
        for (Color c : pixels) {
            // Round color to reduce unique colors
            Color rounded = Color.color(
                    Math.round(c.getRed() * 10) / 10.0,
                    Math.round(c.getGreen() * 10) / 10.0,
                    Math.round(c.getBlue() * 10) / 10.0
            );
            colorCounts.put(rounded, colorCounts.getOrDefault(rounded, 0) + 1);
        }

        // Sort by count
        List<Map.Entry<Color, Integer>> sorted = new ArrayList<>(colorCounts.entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        Color dominant = sorted.isEmpty() ? Color.DARKGRAY : sorted.get(0).getKey();

        // Find vibrant/accent color
        Color accent = Color.web("#0078d4"); // fallback
        double maxVibrancy = -1;
        for (Color c : pixels) {
            double saturation = c.getSaturation();
            double brightness = c.getBrightness();
            // We want saturated and reasonably bright colors for the accent
            if (saturation > 0.4 && brightness > 0.3 && brightness < 0.8) {
                double vibrancy = saturation * brightness;
                if (vibrancy > maxVibrancy) {
                    maxVibrancy = vibrancy;
                    accent = c;
                }
            }
        }

        // Generate background: extremely dark and desaturated version of dominant color (for dark mode feel)
        double hue = dominant.getHue();
        Color background = Color.hsb(hue, 0.2, 0.08); // 8% brightness, 20% saturation
        Color sidebar = Color.hsb(hue, 0.15, 0.05);   // 5% brightness, 15% saturation

        // Primary text: Pure black in light mode, pure white in dark mode
        Color primaryText = lightMode.get() ? Color.BLACK : Color.WHITE;
        // Secondary text: Pure black in light mode, pure white in dark mode
        Color secondaryText = lightMode.get() ? Color.BLACK : Color.WHITE;

        // Ensure accent color has good readability against the background
        if (accent.getBrightness() < 0.4) {
            accent = Color.hsb(accent.getHue(), accent.getSaturation(), 0.6); // Boost brightness
        }

        return new Palette(primaryText, secondaryText, accent, background, sidebar);
    }

    private static class Palette {
        final Color primary;
        final Color secondary;
        final Color accent;
        final Color background;
        final Color sidebar;

        Palette(Color primary, Color secondary, Color accent, Color background, Color sidebar) {
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.background = background;
            this.sidebar = sidebar;
        }
    }
}
