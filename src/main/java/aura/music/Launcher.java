package aura.music;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Optional: Set system properties for better HiDPI scaling on Windows
            // This helps "TouchEnvy" (which uses JavaFX) render text and UI at the
            // correct size
            // on high-resolution screens (like 4K/QHD) without looking tiny.
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                System.setProperty("javafx.macosx.windowScale", "1.0");
                System.setProperty("prism.allowhidpi", "true");
                // High DPI scaling factor. 1.0 is native. 2.0 is 200%, 1.5 is 150%, etc.
                // 1.25 is often a good starting point for 1440p/4K screens.
                System.setProperty("javafx.visualDpi", "120");
            }
        } catch (SecurityException e) {
            // Ignore security exceptions (e.g. applets or restricted environments)
        }

        // Start the main application
        Main.main(args);
    }
}
