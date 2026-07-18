package aura.music;

public class Launcher {
    public static void main(String[] args) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                System.setProperty("javafx.macosx.windowScale", "1.0");
                System.setProperty("prism.allowhidpi", "true");
                System.setProperty("javafx.visualDpi", "120");
            }
        } catch (SecurityException e) {
        }
        // Start the main application
        Main.main(args);
    }
}
