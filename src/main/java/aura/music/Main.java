package aura.music;

import aura.music.library.LibraryManager;
import aura.music.ui.views.MainView;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    private MainViewModel viewModel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize MVVM components
        viewModel = new MainViewModel();
        
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

        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(700);
        primaryStage.setTitle("AuraMusicFX");
        primaryStage.setScene(scene);
        
        // Handle clean shutdown of background threads
        primaryStage.setOnCloseRequest(event -> {
            viewModel.savePlaybackState();
            LibraryManager.getInstance().shutdown();
            System.exit(0);
        });

        primaryStage.show();
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
