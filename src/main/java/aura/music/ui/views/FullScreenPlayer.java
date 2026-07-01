package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.theme.ThemeEngine;
import aura.music.ui.components.LyricsView;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;

public class FullScreenPlayer extends StackPane {

    private final MainViewModel viewModel;
    private final Runnable onClose;
    private final Runnable onExitFullScreen;

    // Background
    private final ImageView bgImageView;
    private final Pane ambientGlow;

    // Core Layout Containers
    private final HBox splitLayout;
    private final VBox leftCol;
    private final VBox rightCol;
    private final StackPane artContainer;
    private final ImageView artworkView;

    // Metadata & Quality
    private final HBox titleRow;
    private final StackPane titleContainer;
    private final StackPane artistContainer;
    private final Label qualityBadge;
    private final VBox infoBox;

    // Progress Section
    private final Slider progressSlider;
    private final Label timeLabel;
    private final Label durationLabel;

    // Playback Controls
    private final Button playPauseButton;
    private final Button favoriteBtn;
    private final Button lyricsToggleBtn;

    // Animations
    private TranslateTransition floatAnimation;

    // Listeners for cleanup
    private javafx.beans.value.ChangeListener<Song> songListener;
    private javafx.beans.value.ChangeListener<Boolean> playListener;
    private javafx.beans.value.ChangeListener<Number> timeListener;
    private javafx.beans.value.ChangeListener<Number> durationListener;
    private javafx.beans.value.ChangeListener<Color> themeListener;

    public FullScreenPlayer(MainViewModel viewModel, Runnable onClose, Runnable onExitFullScreen) {
        this.viewModel = viewModel;
        this.onClose = onClose;
        this.onExitFullScreen = onExitFullScreen;

        getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());

        // 1. Immersive Dynamic Background
        Region solidBg = new Region();
        solidBg.setStyle("-fx-background-color: #0b0c0d;"); // OLED Black / Very Dark

        bgImageView = new ImageView();
        bgImageView.setPreserveRatio(false);
        bgImageView.fitWidthProperty().bind(widthProperty());
        bgImageView.fitHeightProperty().bind(heightProperty());
        bgImageView.setEffect(new BoxBlur(110, 110, 4));
        bgImageView.setOpacity(0.45);

        ambientGlow = new Pane();
        ambientGlow.setStyle("-fx-background-color: transparent;");

        Pane darkOverlay = new Pane();
        darkOverlay.setStyle(
                "-fx-background-color: vertical-gradient(from 0% 0% to 0% 100%, rgba(17, 19, 21, 0.4), rgba(17, 19, 21, 0.95));");

        solidBg.setMouseTransparent(true);
        bgImageView.setMouseTransparent(true);
        ambientGlow.setMouseTransparent(true);
        darkOverlay.setMouseTransparent(true);

        getChildren().addAll(solidBg, bgImageView, ambientGlow, darkOverlay);

        // 3. Split Layout (Now just centered player controls)
        splitLayout = new HBox(80);
        splitLayout.setAlignment(Pos.CENTER);
        splitLayout.setPadding(new Insets(80, 80, 80, 80));

        // --- LEFT COLUMN (Artwork & Info) ---
        leftCol = new VBox(24);
        leftCol.setAlignment(Pos.CENTER);
        leftCol.setPrefWidth(480);
        leftCol.setMinWidth(480);

        // Album Artwork Container
        artContainer = new StackPane();
        artContainer.setPrefSize(480, 480);
        artContainer.setMaxSize(480, 480);

        artworkView = new ImageView();
        artworkView.setFitWidth(480);
        artworkView.setFitHeight(480);
        artworkView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(480, 480);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        artworkView.setClip(clip);

        // Glass border and Shadow
        artContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 18px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 18px; -fx-border-width: 1px;");
        artContainer.setEffect(new DropShadow(35, Color.rgb(0, 0, 0, 0.6)));
        artContainer.getChildren().add(artworkView);

        // Double Click to Like / Favorite
        artContainer.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                toggleFavorite();
            }
        });


        // Setup Floating Animation
        setupFloatingAnimation();

        // Info Box (Title, Artist, Quality Badge)
        titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);

        titleContainer = new StackPane();
        titleContainer.setAlignment(Pos.CENTER);

        favoriteBtn = new Button();
        favoriteBtn.setGraphic(SVGIcons.createStarIcon(18, Color.WHITE, false));
        favoriteBtn.getStyleClass().add("icon-button");
        favoriteBtn.setOnAction(e -> toggleFavorite());

        titleRow.getChildren().addAll(titleContainer, favoriteBtn);

        artistContainer = new StackPane();
        artistContainer.setAlignment(Pos.CENTER);

        qualityBadge = new Label("LOSSLESS FLAC 24-BIT");
        qualityBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 4; -fx-border-width: 0.5;");

        infoBox = new VBox(8, titleRow, artistContainer, qualityBadge);
        infoBox.setAlignment(Pos.CENTER);

        // Progress Section - Inline as per screenshot
        progressSlider = new Slider(0, 100, 0);
        progressSlider.getStyleClass().add("premium-slider");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        timeLabel = new Label("0:00");
        timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px; -fx-font-weight: 500;");
        durationLabel = new Label("-0:00");
        durationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px; -fx-font-weight: 500;");

        HBox progressBox = new HBox(12, timeLabel, progressSlider, durationLabel);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMaxWidth(480);

        // Playback Controls
        Button prevBtn = new Button();
        prevBtn.setGraphic(SVGIcons.createPreviousIcon(18, Color.WHITE));
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setOnAction(e -> viewModel.previous());

        playPauseButton = new Button();
        playPauseButton.setGraphic(SVGIcons.createPlayIcon(22, Color.WHITE));
        playPauseButton.getStyleClass().add("icon-button");
        playPauseButton.setOnAction(e -> viewModel.togglePlayPause());

        Button nextBtn = new Button();
        nextBtn.setGraphic(SVGIcons.createNextIcon(18, Color.WHITE));
        nextBtn.getStyleClass().add("icon-button");
        nextBtn.setOnAction(e -> viewModel.next());

        // Left and Right controls parts in bottom row
        Button speakerBtn = new Button();
        speakerBtn.setGraphic(SVGIcons.createVolumeIcon(14, Color.WHITE));
        speakerBtn.getStyleClass().add("icon-button");
        speakerBtn.setOnAction(e -> viewModel.isMutedProperty().set(!viewModel.isMutedProperty().get()));

        Button optionsBtn = new Button("•••");
        optionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");

        HBox leftBottom = new HBox(15, speakerBtn, optionsBtn);
        leftBottom.setAlignment(Pos.CENTER_LEFT);
        leftBottom.setPrefWidth(120);

        HBox centerBottom = new HBox(25, prevBtn, playPauseButton, nextBtn);
        centerBottom.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBottom, Priority.ALWAYS);

        lyricsToggleBtn = new Button();
        lyricsToggleBtn.setGraphic(SVGIcons.createLyricsIcon(16, Color.WHITE));
        lyricsToggleBtn.getStyleClass().add("icon-button");

        HBox rightBottom = new HBox(15, lyricsToggleBtn);
        rightBottom.setAlignment(Pos.CENTER_RIGHT);
        rightBottom.setPrefWidth(120);

        HBox bottomControls = new HBox(leftBottom, centerBottom, rightBottom);
        bottomControls.setAlignment(Pos.CENTER);
        bottomControls.setMaxWidth(480);
        bottomControls.setPadding(new Insets(15, 0, 0, 0));

        leftCol.getChildren().addAll(artContainer, infoBox, progressBox, bottomControls);

        // --- RIGHT COLUMN (Lyrics / Queue) ---
        rightCol = new VBox(20);
        rightCol.setAlignment(Pos.CENTER);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        LyricsView lyricsView = new LyricsView(viewModel);
        
        ListView<Song> queueListView = new ListView<>(viewModel.getQueue());
        VBox.setVgrow(queueListView, Priority.ALWAYS);
        queueListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        queueListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Song>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getTitle() + " - " + item.getArtist());
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 12;");
                }
            }
        });

        lyricsView.managedProperty().bind(lyricsView.visibleProperty());
        queueListView.managedProperty().bind(queueListView.visibleProperty());

        lyricsView.setVisible(true);
        queueListView.setVisible(false);

        rightCol.getChildren().addAll(lyricsView, queueListView);

        // Toggle Actions
        lyricsToggleBtn.setOnAction(e -> {
            boolean lVisible = !lyricsView.isVisible();
            lyricsView.setVisible(lVisible);
            if (lVisible) {
                queueListView.setVisible(false);
                rightCol.setVisible(true);
                rightCol.setManaged(true);
                updateLayoutAlignment(true);
                lyricsToggleBtn.setGraphic(SVGIcons.createLyricsIcon(16, Color.WHITE));
            } else {
                rightCol.setVisible(false);
                rightCol.setManaged(false);
                updateLayoutAlignment(false);
                lyricsToggleBtn.setGraphic(SVGIcons.createLyricsIcon(16, Color.WHITE));
            }
        });

        // Top bar for Exit & Close buttons matching screenshot exactly
        HBox topBar = new HBox(15);
        topBar.setPickOnBounds(false); // Make container click-through
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(20));
        
        Button exitFsBtnTop = new Button();
        exitFsBtnTop.setGraphic(SVGIcons.createExitFullScreenIcon(14, Color.WHITE));
        exitFsBtnTop.getStyleClass().add("icon-button");
        exitFsBtnTop.setOnAction(e -> onExitFullScreen.run());

        Button closeBtnTop = new Button();
        closeBtnTop.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 4;");
        closeBtnTop.setText("✕");
        closeBtnTop.setOnAction(e -> onClose.run());
        
        topBar.getChildren().addAll(exitFsBtnTop, closeBtnTop);

        splitLayout.getChildren().addAll(leftCol, rightCol);
        getChildren().addAll(splitLayout, topBar);
        StackPane.setAlignment(topBar, Pos.TOP_RIGHT);

        // Set up initial state of lyrics panel
        boolean initialNoLyrics = viewModel.getLyricsLines().isEmpty();
        rightCol.setVisible(!initialNoLyrics);
        rightCol.setManaged(!initialNoLyrics);
        updateLayoutAlignment(!initialNoLyrics);

        // Initialize listeners
        songListener = (obs, oldSong, newSong) -> updateSong(newSong);
        playListener = (obs, oldVal, newVal) -> updatePlayState(newVal);
        timeListener = (obs, oldVal, newVal) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newVal.doubleValue());
            }
            timeLabel.setText(formatTime(newVal.doubleValue()));
        };
        durationListener = (obs, oldVal, newVal) -> {
            progressSlider.setMax(newVal.doubleValue());
        };
        themeListener = (obs, oldCol, newCol) -> {
            updatePlayState(viewModel.isPlayingProperty().get());
        };

        // Listeners & Key Events
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
                oldScene.removeEventFilter(ScrollEvent.SCROLL, this::handleScrollVolume);
                
                viewModel.currentSongProperty().removeListener(songListener);
                viewModel.isPlayingProperty().removeListener(playListener);
                viewModel.currentTimeProperty().removeListener(timeListener);
                viewModel.totalDurationProperty().removeListener(durationListener);
                ThemeEngine.getInstance().primaryColorProperty().removeListener(themeListener);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
                newScene.addEventFilter(ScrollEvent.SCROLL, this::handleScrollVolume);
            }
        });

        viewModel.currentSongProperty().addListener(songListener);
        viewModel.isPlayingProperty().addListener(playListener);
        viewModel.currentTimeProperty().addListener(timeListener);
        viewModel.totalDurationProperty().addListener(durationListener);
        ThemeEngine.getInstance().primaryColorProperty().addListener(themeListener);

        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                viewModel.seek(newVal.doubleValue());
            }
        });

        updateSong(viewModel.currentSongProperty().get());
        updatePlayState(viewModel.isPlayingProperty().get());
    }

    private void setupFloatingAnimation() {
        floatAnimation = new TranslateTransition(Duration.seconds(3.5), artContainer);
        floatAnimation.setFromY(0);
        floatAnimation.setToY(-12);
        floatAnimation.setCycleCount(Animation.INDEFINITE);
        floatAnimation.setAutoReverse(true);
        floatAnimation.setInterpolator(Interpolator.EASE_BOTH);
        floatAnimation.play();
    }

    private void toggleLyricsLayout() {
        boolean visible = !rightCol.isVisible();
        rightCol.setVisible(visible);
        rightCol.setManaged(visible);
        updateLayoutAlignment(visible);
    }

    private void updateLayoutAlignment(boolean splitMode) {
        if (splitMode) {
            infoBox.setAlignment(Pos.CENTER_LEFT);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            titleContainer.setAlignment(Pos.CENTER_LEFT);
            artistContainer.setAlignment(Pos.CENTER_LEFT);
            lyricsToggleBtn.setGraphic(SVGIcons.createLyricsIcon(16, Color.WHITE));
        } else {
            infoBox.setAlignment(Pos.CENTER);
            titleRow.setAlignment(Pos.CENTER);
            titleContainer.setAlignment(Pos.CENTER);
            artistContainer.setAlignment(Pos.CENTER);
            lyricsToggleBtn.setGraphic(SVGIcons.createLyricsIcon(16, Color.WHITE));
        }
    }

    private void toggleFavorite() {
        Song song = viewModel.currentSongProperty().get();
        if (song != null) {
            viewModel.toggleFavorite(song);
            updateFavoriteButtonState(song);
        }
    }

    private void updateFavoriteButtonState(Song song) {
        if (song.isFavorite()) {
            favoriteBtn.setGraphic(SVGIcons.createStarIcon(18, Color.web("#ff2d55"), true));
            if (!favoriteBtn.getGraphic().getStyleClass().contains("fav-active")) {
                favoriteBtn.getGraphic().getStyleClass().add("fav-active");
            }
        } else {
            favoriteBtn.setGraphic(SVGIcons.createStarIcon(18, Color.WHITE, false));
            favoriteBtn.getGraphic().getStyleClass().remove("fav-active");
        }
    }

    private void updateSong(Song song) {
        if (song == null)
            return;
        Platform.runLater(() -> {
            titleContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(), "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;", 400)
            );
            artistContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist() + " • " + song.getAlbum(), "-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 500;", 440)
            );

            updateFavoriteButtonState(song);

            byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            if (artBytes != null) {
                Image image = new Image(new ByteArrayInputStream(artBytes));
                artworkView.setImage(image);
                bgImageView.setImage(image);

                // Extract artwork color for ambient glow tint
                updateAmbientGlow(image);
            } else {
                artworkView.setImage(null);
                bgImageView.setImage(null);
                ambientGlow.setBackground(null);
            }

            // Check if song is hi-res
            if (song.isHiRes()) {
                qualityBadge.setText("HI-RES LOSSLESS FLAC 24-BIT");
            } else {
                qualityBadge.setText("LOSSLESS AAC");
            }
        });
    }

    private void updateAmbientGlow(Image image) {
        // Simple fallback gradient using extracted/ambient theme colors
        Color color = Color.web("#0078d4"); // Default Accent
        RadialGradient gradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1, 0.15)),
                new Stop(1, Color.TRANSPARENT));
        ambientGlow.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void updatePlayState(boolean playing) {
        Platform.runLater(() -> {
            Color iconColor = Color.WHITE;
            if (playing) {
                playPauseButton.setGraphic(SVGIcons.createPauseIcon(22, iconColor));
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(400),
                        artContainer);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            } else {
                playPauseButton.setGraphic(SVGIcons.createPlayIcon(22, iconColor));
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(400),
                        artContainer);
                st.setToX(0.92);
                st.setToY(0.92);
                st.play();
            }
        });
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.SPACE) {
            viewModel.togglePlayPause();
            event.consume();
        } else if (code == KeyCode.LEFT) {
            viewModel.previous();
            event.consume();
        } else if (code == KeyCode.RIGHT) {
            viewModel.next();
            event.consume();
        } else if (code == KeyCode.ESCAPE) {
            onClose.run();
            event.consume();
        } else if (code == KeyCode.L) {
            toggleLyricsLayout();
            event.consume();
        } else if (code == KeyCode.F) {
            toggleFavorite();
            event.consume();
        }
    }

    private void handleScrollVolume(ScrollEvent event) {
        if (event.isControlDown()) {
            double delta = event.getDeltaY();
            double currentVol = viewModel.volumeProperty().get();
            double newVol = Math.max(0, Math.min(1, currentVol + (delta > 0 ? 0.05 : -0.05)));
            viewModel.volumeProperty().set(newVol);
            event.consume();
        }
    }

    private String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", minutes, secs);
    }
}
