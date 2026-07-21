package aura.music;

import aura.music.library.LibraryManager;
import aura.music.ui.views.MainView;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    private static MainViewModel instanceViewModel;
    private static Stage primaryStageInstance;
    private MainViewModel viewModel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize MVVM components
        viewModel = new MainViewModel();
        instanceViewModel = viewModel;
        primaryStageInstance = primaryStage;
        
        // Make stage borderless & transparent
        primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        // Set application icon
        try {
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/aura/music/AuraMusicDesktop.png")));
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }

        MainView mainView = new MainView(viewModel);

        Scene scene = new Scene(mainView, 1280, 760);
        scene.setFill(Color.TRANSPARENT);

        // Bind ThemeEngine to CSS
        aura.music.theme.ThemeEngine themeEngine = aura.music.theme.ThemeEngine.getInstance();
        Runnable updateTheme = () -> {
            String css = String.format(
                "-fx-primary-text: %s; -fx-secondary-text: %s; -fx-bg: %s; -fx-accent-color: %s; -fx-player-bar-bg: %s; -fx-card-bg: %s; -fx-custom-border-color: %s;",
                toHexString(themeEngine.primaryColorProperty().get()),
                toHexString(themeEngine.secondaryColorProperty().get()),
                toHexString(themeEngine.backgroundColorProperty().get()),
                toHexString(themeEngine.accentColorProperty().get()),
                toHexString(themeEngine.sidebarColorProperty().get()),
                toHexString(themeEngine.sidebarColorProperty().get()), // reusing sidebar for card bg
                "rgba(255, 255, 255, 0.05)"
            );
            mainView.setStyle(css);
        };

        themeEngine.primaryColorProperty().addListener((obs, o, n) -> updateTheme.run());
        themeEngine.secondaryColorProperty().addListener((obs, o, n) -> updateTheme.run());
        themeEngine.accentColorProperty().addListener((obs, o, n) -> updateTheme.run());
        themeEngine.backgroundColorProperty().addListener((obs, o, n) -> updateTheme.run());
        themeEngine.sidebarColorProperty().addListener((obs, o, n) -> updateTheme.run());
        updateTheme.run(); // initial application

        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(700);
        primaryStage.setTitle("Aura Music Desktop");
        primaryStage.setScene(scene);
        
        // Handle clean shutdown of background threads
        primaryStage.setOnCloseRequest(event -> {
            viewModel.savePlaybackState();
            LibraryManager.getInstance().shutdown();
            System.exit(0);
        });

        primaryStage.show();

        // Process initial command line arguments
        java.util.List<String> parameters = getParameters().getRaw();
        if (parameters != null && !parameters.isEmpty()) {
            handleExternalArgument(String.join(" ", parameters));
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    public static void handleExternalArgument(String arg) {
        if (arg == null || arg.trim().isEmpty()) return;
        
        // Clean up argument if it has quotes (common from Windows File Explorer)
        String filePath = arg;
        if (filePath.startsWith("\"") && filePath.endsWith("\"")) {
            filePath = filePath.substring(1, filePath.length() - 1);
        }

        final String finalPath = filePath;
        javafx.application.Platform.runLater(() -> {
            if (primaryStageInstance != null) {
                primaryStageInstance.setIconified(false);
                primaryStageInstance.toFront();
                primaryStageInstance.requestFocus();
            }
            if (instanceViewModel != null) {
                java.io.File file = new java.io.File(finalPath);
                if (file.exists() && file.isFile()) {
                    aura.music.model.Song song = new aura.music.model.Song(file.getAbsolutePath());
                    // Set basic tags based on filename temporarily
                    song.setTitle(file.getName().replaceFirst("[.][^.]+$", ""));
                    instanceViewModel.play(song);
                }
            }
        });
    }

    public static void main(String[] args) {
        // Reset LogManager to completely silence console log spam from jaudiotagger
        try {
            java.util.logging.LogManager.getLogManager().reset();
            java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}
