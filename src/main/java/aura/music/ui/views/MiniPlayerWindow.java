package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.ByteArrayInputStream;

public class MiniPlayerWindow extends Stage {

    private final MainViewModel viewModel;
    private final Stage mainStage;

    private double xOffset = 0;
    private double yOffset = 0;

    private ImageView artworkView;
    private javafx.scene.layout.StackPane titleContainer;
    private javafx.scene.layout.StackPane artistContainer;
    private Slider progressSlider;
    private Button playPauseBtn;

    // Listeners for cleanup
    private final javafx.beans.value.ChangeListener<Song> songListener = (obs, oldSong, newSong) -> updateSong(newSong);
    private final javafx.beans.value.ChangeListener<Boolean> playListener = (obs, oldVal, newVal) -> updatePlayState(newVal);
    private final javafx.beans.value.ChangeListener<Number> timeListener = (obs, oldVal, newVal) -> {
        if (!progressSlider.isValueChanging()) {
            progressSlider.setValue(newVal.doubleValue());
        }
    };
    private final javafx.beans.value.ChangeListener<Number> durationListener = (obs, oldVal, newVal) -> {
        progressSlider.setMax(newVal.doubleValue());
    };

    public MiniPlayerWindow(MainViewModel viewModel, Stage mainStage) {
        this.viewModel = viewModel;
        this.mainStage = mainStage;

        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setTitle("AuraMusicFX Mini");

        // Layout Container
        VBox root = new VBox(12);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(240, 320);
        root.setMinSize(240, 320);
        root.setMaxSize(240, 320);

        // Premium Dark Glass styling
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#181818"),
                new CornerRadii(16), Insets.EMPTY)));
        root.setStyle("-fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 16; -fx-border-width: 1;");

        DropShadow shadow = new DropShadow(20, Color.rgb(0, 0, 0, 0.5));
        root.setEffect(shadow);

        // Dragging
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            setX(event.getScreenX() - xOffset);
            setY(event.getScreenY() - yOffset);
        });

        // Top Controls Row (Fullscreen, Close, Expand)
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_RIGHT);
        topRow.setPadding(new Insets(0, 0, 4, 0));

        Button fsBtn = new Button("⛶");
        fsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 11px; -fx-cursor: hand;");
        fsBtn.setOnAction(e -> openFullScreenPlayer());

        Button expandBtn = new Button("⤢");
        expandBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 11px; -fx-cursor: hand;");
        expandBtn.setOnAction(e -> restoreMainPlayer());

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ff4757; -fx-font-size: 11px; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> restoreMainPlayer());

        topRow.getChildren().addAll(fsBtn, expandBtn, closeBtn);

        // Artwork
        StackPane artStack = new StackPane();
        artStack.setPrefSize(140, 140);
        artStack.setMinSize(140, 140);
        artStack.setMaxSize(140, 140);
        artStack.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(12), Insets.EMPTY)));

        artworkView = new ImageView();
        artworkView.setFitWidth(140);
        artworkView.setFitHeight(140);
        artworkView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(140, 140);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        artworkView.setClip(clip);

        Label placeholder = new Label("♫");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
        artStack.getChildren().addAll(placeholder, artworkView);

        // Metadata
        VBox metaBox = new VBox(2);
        metaBox.setAlignment(Pos.CENTER);

        titleContainer = new StackPane();
        titleContainer.setAlignment(Pos.CENTER);
        artistContainer = new StackPane();
        artistContainer.setAlignment(Pos.CENTER);

        metaBox.getChildren().addAll(titleContainer, artistContainer);

        // Progress Slider
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setStyle("-fx-pref-height: 4px;");
        progressSlider.getStyleClass().add("premium-slider");
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                viewModel.seek(newVal.doubleValue());
            }
        });

        // Playback Controls Row
        HBox controlRow = new HBox(15);
        controlRow.setAlignment(Pos.CENTER);

        Button prevBtn = new Button();
        prevBtn.setGraphic(SVGIcons.createPreviousIcon(12, Color.WHITE));
        prevBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        prevBtn.setOnAction(e -> viewModel.previous());

        playPauseBtn = new Button();
        playPauseBtn.setGraphic(SVGIcons.createPlayIcon(14, Color.BLACK));
        playPauseBtn
                .setStyle("-fx-background-color: white; -fx-background-radius: 50%; -fx-padding: 8; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> viewModel.togglePlayPause());

        Button nextBtn = new Button();
        nextBtn.setGraphic(SVGIcons.createNextIcon(12, Color.WHITE));
        nextBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        nextBtn.setOnAction(e -> viewModel.next());

        controlRow.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);

        root.getChildren().addAll(topRow, artStack, metaBox, progressSlider, controlRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        setScene(scene);

        // Setup bindings
        setupBindings();
    }

    private void setupBindings() {
        viewModel.currentSongProperty().addListener(songListener);
        viewModel.isPlayingProperty().addListener(playListener);
        viewModel.currentTimeProperty().addListener(timeListener);
        viewModel.totalDurationProperty().addListener(durationListener);

        updateSong(viewModel.currentSongProperty().get());
        updatePlayState(viewModel.isPlayingProperty().get());
    }

    private void updateSong(Song song) {
        if (song != null) {
            titleContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(), "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;", 200)
            );
            artistContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(), "-fx-text-fill: #b3b3b3; -fx-font-size: 11px;", 200)
            );

            byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            if (artBytes != null) {
                artworkView.setImage(new Image(new ByteArrayInputStream(artBytes), 140, 140, true, true));
            } else {
                artworkView.setImage(null);
            }
        } else {
            Label defaultTitle = new Label("Not Playing");
            defaultTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
            titleContainer.getChildren().setAll(defaultTitle);
            artistContainer.getChildren().clear();
            artworkView.setImage(null);
        }
    }

    private void updatePlayState(boolean playing) {
        Platform.runLater(() -> {
            if (playing) {
                playPauseBtn.setGraphic(SVGIcons.createPauseIcon(14, Color.BLACK));
            } else {
                playPauseBtn.setGraphic(SVGIcons.createPlayIcon(14, Color.BLACK));
            }
        });
    }

    private void restoreMainPlayer() {
        close();
        mainStage.show();
    }

    private void openFullScreenPlayer() {
        close();
        mainStage.show();
        if (mainStage.getScene().getRoot() instanceof MainView) {
            ((MainView) mainStage.getScene().getRoot()).enterFullScreenFromMiniPlayer();
        }
    }

    @Override
    public void close() {
        viewModel.currentSongProperty().removeListener(songListener);
        viewModel.isPlayingProperty().removeListener(playListener);
        viewModel.currentTimeProperty().removeListener(timeListener);
        viewModel.totalDurationProperty().removeListener(durationListener);
        super.close();
    }
}
