package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.ui.components.MenuUtils;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import aura.music.lyrics.LyricLine;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MiniPlayerWindow extends Stage {

    public enum MiniPlayerViewMode {
        ARTWORK, LYRICS, QUEUE, COMPACT
    }

    private final MainViewModel viewModel;
    private final Stage mainStage;

    private double xOffset = 0;
    private double yOffset = 0;

    // View State
    private MiniPlayerViewMode currentMode = MiniPlayerViewMode.ARTWORK;
    private boolean isPinned = true;
    private boolean isQueueTabActive = true; // true = Playing Next, false = History
    private boolean autoplayEnabled = true;

    // Root elements
    private StackPane root;
    private Rectangle rootClip;
    private ImageView artworkView;
    private Region dimOverlay;
    private VBox mainLayout;

    // Header elements
    private HBox headerBox;
    private StackPane headerThumbnailContainer;
    private ImageView headerThumbnailView;
    private VBox headerMetadataContainer;
    private StackPane headerTitleContainer;
    private StackPane headerArtistContainer;
    private HBox headerQualityBadgeBox;

    // Content containers
    private StackPane contentStack;
    private Region artworkSpacer; // pushes controls to bottom in Artwork mode
    private ScrollPane lyricsScroll;
    private VBox lyricsLinesContainer;
    private VBox queueContainer;
    private VBox queueListContainer;
    private ScrollPane queueScroll;
    private Button tabPlayingNextBtn;
    private Button tabHistoryBtn;
    private Label queueHeaderTitle;
    private Button queueClearBtn;
    private Button queueAutoplayBtn;

    // Footer elements
    private VBox footerBox;
    private Slider progressSlider;
    private Label currentTimeLbl;
    private Label remainingTimeLbl;
    private Button playPauseBtn;
    private Button volumeBtn;
    private Button pinBtn;
    private Button lyricsBtn;
    private Button queueBtn;

    // Artwork mode metadata elements
    private BorderPane artworkMetadataRow;
    private StackPane artworkTitleContainer;
    private StackPane artworkArtistContainer;
    private HBox artworkQualityBadgeBox;
    private Rectangle thumbClip;
    private Button restoreFromThumbBtn;
    private Button fsBtn;
    private MiniPlayerViewMode previousCompactMode = MiniPlayerViewMode.COMPACT;
    private Color dominantColor = Color.web("#2a2a2a"); // default, updated per song
    private final java.util.Map<String, Image> artworkCache = new java.util.HashMap<>();

    // Quality details popup elements
    private Popup qualityPopup;
    private Label qPopupHeader;
    private Label qPopupDetails;
    private Popup volumePopup;

    // Lyrics tracking
    private final List<Label> lyricLabels = new ArrayList<>();
    private int currentActiveLyricIndex = -1;
    private Timeline lyricScrollTimeline;

    // Listeners for cleanup
    private ChangeListener<Song> songListener;
    private ChangeListener<Boolean> playListener;
    private ChangeListener<Number> timeListener;
    private ChangeListener<Number> durationListener;
    private ChangeListener<Number> volumeListener;
    private ChangeListener<Boolean> muteListener;
    private ChangeListener<Number> lyricIndexListener;
    private ListChangeListener<Song> queueListener;
    private ListChangeListener<LyricLine> lyricsListListener;

    // Hover fade transitions
    private FadeTransition fadeInBottom;
    private FadeTransition fadeOutBottom;
    private FadeTransition fadeInTop;
    private FadeTransition fadeOutTop;

    public MiniPlayerWindow(MainViewModel viewModel, Stage mainStage) {
        this.viewModel = viewModel;
        this.mainStage = mainStage;

        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(isPinned);
        setTitle("AuraMusicFX Mini");

        // Initialize Listeners
        this.songListener = (obs, oldSong, newSong) -> {
            updateSong(newSong);
            if (currentMode == MiniPlayerViewMode.QUEUE) {
                rebuildQueueList();
            }
        };
        this.playListener = (obs, oldVal, newVal) -> updatePlayState(newVal);
        this.timeListener = (obs, oldVal, newVal) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newVal.doubleValue());
            }
            updateTimeLabels(newVal.doubleValue(), viewModel.totalDurationProperty().get());
        };
        this.durationListener = (obs, oldVal, newVal) -> {
            progressSlider.setMax(newVal.doubleValue());
            updateTimeLabels(viewModel.currentTimeProperty().get(), newVal.doubleValue());
        };
        this.volumeListener = (obs, oldVal, newVal) -> updateVolumeState(newVal.doubleValue(),
                viewModel.isMutedProperty().get());
        this.muteListener = (obs, oldVal, newVal) -> updateVolumeState(viewModel.volumeProperty().get(), newVal);
        this.lyricIndexListener = (obs, oldVal, newVal) -> updateActiveLyricLine(newVal.intValue());
        this.queueListener = change -> {
            if (currentMode == MiniPlayerViewMode.QUEUE) {
                Platform.runLater(this::rebuildQueueList);
            }
        };
        this.lyricsListListener = change -> {
            if (currentMode == MiniPlayerViewMode.LYRICS) {
                Platform.runLater(this::rebuildLyrics);
            }
        };

        // Main StackPane Root to hold Artwork (Background) and Overlays
        root = new StackPane();
        root.setPrefSize(360, 360);
        root.setMinSize(360, 360);
        root.setMaxSize(360, 360);
        root.setStyle("-fx-background-color: #121212;");

        // Clip to rounded corners dynamically based on window size
        rootClip = new Rectangle(360, 360);
        rootClip.setArcWidth(20);
        rootClip.setArcHeight(20);
        rootClip.widthProperty().bind(root.widthProperty());
        rootClip.heightProperty().bind(root.heightProperty());
        root.setClip(rootClip);

        DropShadow shadow = new DropShadow(25, Color.rgb(0, 0, 0, 0.6));
        root.setEffect(shadow);

        // Dragging & Double-Click to Restore
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            setX(event.getScreenX() - xOffset);
            setY(event.getScreenY() - yOffset);
        });
        root.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                restoreMainPlayer();
            }
        });

        // 1. Full-Window Background Artwork (visible in ARTWORK mode)
        artworkView = new ImageView();
        artworkView.setFitWidth(360);
        artworkView.setFitHeight(360);
        artworkView.fitWidthProperty().bind(root.widthProperty());
        artworkView.fitHeightProperty().bind(root.heightProperty());
        artworkView.setPreserveRatio(false);

        // Dark dimming layer to keep text readable on any artwork background
        dimOverlay = new Region();
        dimOverlay.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.4) 50%, rgba(0,0,0,0.8) 100%);");
        dimOverlay.prefWidthProperty().bind(root.widthProperty());
        dimOverlay.prefHeightProperty().bind(root.heightProperty());

        // 2. Main Vertical Layout Container
        mainLayout = new VBox(0);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());
        mainLayout.setMaxWidth(Double.MAX_VALUE);

        // --- HEADER SECTION ---
        headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(12, 20, 8, 20));
        headerBox.setPickOnBounds(false);
        headerBox.setMaxWidth(Double.MAX_VALUE);

        // Left Side: Small thumbnail with overlayed PiP button (Only visible in LYRICS
        // / QUEUE modes)
        headerThumbnailContainer = new StackPane();
        headerThumbnailContainer.setPrefSize(40, 40);
        headerThumbnailContainer.setMinSize(40, 40);
        headerThumbnailContainer.setMaxSize(40, 40);

        headerThumbnailView = new ImageView();
        headerThumbnailView.setFitWidth(40);
        headerThumbnailView.setFitHeight(40);
        thumbClip = new Rectangle(40, 40);
        thumbClip.setArcWidth(8);
        thumbClip.setArcHeight(8);
        headerThumbnailView.setClip(thumbClip);

        restoreFromThumbBtn = new Button();
        restoreFromThumbBtn.setGraphic(createPipIcon(12, Color.WHITE));
        restoreFromThumbBtn.setStyle(
                "-fx-background-color: rgba(20, 20, 20, 0.7);" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                        "-fx-border-radius: 8px;" +
                        "-fx-border-width: 1px;" +
                        "-fx-padding: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 8, 0, 0, 2);");
        restoreFromThumbBtn.setOpacity(0.0); // Show only on hover
        headerThumbnailContainer.getChildren().addAll(headerThumbnailView, restoreFromThumbBtn);

        headerThumbnailContainer.setOnMouseEntered(e -> {
            restoreFromThumbBtn.setOpacity(1.0);
        });
        headerThumbnailContainer.setOnMouseExited(e -> {
            restoreFromThumbBtn.setOpacity(0.0);
        });

        restoreFromThumbBtn.setOnMouseEntered(e -> {
            restoreFromThumbBtn.setOpacity(1.0);
            restoreFromThumbBtn.setStyle(
                    "-fx-background-color: rgba(40, 40, 40, 0.85);" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                            "-fx-border-radius: 8px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-padding: 8px;" +
                            "-fx-cursor: hand;" +
                            "-fx-scale-x: 1.05; -fx-scale-y: 1.05;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.6), 10, 0, 0, 2);");
        });
        restoreFromThumbBtn.setOnMouseExited(e -> {
            restoreFromThumbBtn.setOpacity(0.0);
            restoreFromThumbBtn.setStyle(
                    "-fx-background-color: rgba(20, 20, 20, 0.7);" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                            "-fx-border-radius: 8px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-padding: 8px;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 8, 0, 0, 2);");
        });
        restoreFromThumbBtn.setOnAction(e -> setViewMode(MiniPlayerViewMode.ARTWORK));

        // Center: Metadata labels (Only visible in LYRICS / QUEUE modes)
        headerMetadataContainer = new VBox(2);
        headerMetadataContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerMetadataContainer, Priority.ALWAYS);

        headerTitleContainer = new StackPane();
        headerTitleContainer.setAlignment(Pos.CENTER_LEFT);
        headerArtistContainer = new StackPane();
        headerArtistContainer.setAlignment(Pos.CENTER_LEFT);
        headerQualityBadgeBox = new HBox();
        headerQualityBadgeBox.setAlignment(Pos.CENTER_LEFT);
        headerMetadataContainer.getChildren().addAll(headerTitleContainer, headerArtistContainer,
                headerQualityBadgeBox);

        // Right Side: Window Control Buttons – live as a TOP-RIGHT overlay on root
        HBox windowControls = new HBox(4);
        windowControls.setAlignment(Pos.TOP_RIGHT);
        windowControls.setPadding(new Insets(12, 16, 6, 16));
        windowControls.setPickOnBounds(false);
        windowControls.setMaxHeight(Region.USE_PREF_SIZE);

        pinBtn = new Button();
        pinBtn.setGraphic(createPinIcon(10, Color.WHITE));
        pinBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-opacity: 0.8; -fx-padding: 3;");
        pinBtn.setTooltip(new Tooltip("Always on Top"));
        pinBtn.setOnAction(e -> toggleAlwaysOnTop());

        Button minBtn = new Button();
        SVGPath minIcon = new SVGPath();
        minIcon.setContent("M19 13H5v-2h14v2z");
        minIcon.setFill(Color.WHITE);
        minIcon.setScaleX(9.0 / 24.0);
        minIcon.setScaleY(9.0 / 24.0);
        minBtn.setGraphic(minIcon);
        minBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-opacity: 0.75; -fx-padding: 3;");
        minBtn.setOnMouseEntered(e -> minBtn.setOpacity(1.0));
        minBtn.setOnMouseExited(e -> minBtn.setOpacity(0.75));
        minBtn.setOnAction(e -> setIconified(true));

        fsBtn = new Button();
        SVGPath fsIcon = new SVGPath();
        fsIcon.setContent(
                "M18.17 5.83L14 10l1.41 1.41 4.17-4.17V11h2V3h-8v2h3.59zM5.83 18.17L10 14l-1.41-1.41-4.17 4.17V13H2.41v8h8v-2H6.83z");
        fsIcon.setFill(Color.WHITE);
        fsIcon.setScaleX(10.0 / 24.0);
        fsIcon.setScaleY(10.0 / 24.0);
        fsBtn.setGraphic(fsIcon);
        fsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-opacity: 0.75; -fx-padding: 3;");
        fsBtn.setOnMouseEntered(e -> fsBtn.setOpacity(1.0));
        fsBtn.setOnMouseExited(e -> fsBtn.setOpacity(0.75));
        fsBtn.setTooltip(new Tooltip("Fullscreen Mode"));
        fsBtn.setOnAction(e -> openFullScreenPlayer());

        Button closeBtn = new Button();
        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent(
                "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        closeIcon.setFill(Color.WHITE);
        closeIcon.setScaleX(10.0 / 24.0);
        closeIcon.setScaleY(10.0 / 24.0);
        closeBtn.setGraphic(closeIcon);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-opacity: 0.75; -fx-padding: 3;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setOpacity(1.0));
        closeBtn.setOnMouseExited(e -> closeBtn.setOpacity(0.75));
        closeBtn.setOnAction(e -> restoreMainPlayer());

        windowControls.getChildren().addAll(minBtn, fsBtn, closeBtn);

        // Overlay anchor: place windowControls at the very top-right of root
        StackPane.setAlignment(windowControls, Pos.TOP_RIGHT);

        // Add left margin to thumbnail so artwork shifts slightly right
        HBox.setMargin(headerThumbnailContainer, new Insets(0, 0, 0, 4));

        // headerBox now holds: [thumbnail] [metadata] (no window controls)
        headerBox.getChildren().addAll(headerThumbnailContainer, headerMetadataContainer);

        // --- CONTENT SECTION (STRETCHES VERTICALLY) ---
        contentStack = new StackPane();
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // 1) Spacer for Artwork mode (pushes controls to the bottom)
        artworkSpacer = new Region();
        artworkSpacer.setPrefHeight(0);

        // 2) Lyrics Scroll Area (visible in LYRICS mode)
        lyricsScroll = new ScrollPane();
        lyricsScroll.setFitToWidth(true);
        lyricsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lyricsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lyricsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        lyricsLinesContainer = new VBox(22);
        lyricsLinesContainer.setAlignment(Pos.CENTER);
        lyricsLinesContainer.setPadding(new Insets(160, 20, 160, 20));
        lyricsScroll.setContent(lyricsLinesContainer);

        // 3) Queue Container Area (visible in QUEUE mode)
        queueContainer = new VBox(12);
        queueContainer.setPadding(new Insets(4, 16, 8, 16));

        // Tab Selector HBox: [Playing Next] [History]
        HBox tabSelectorBox = new HBox(8);
        tabSelectorBox.setAlignment(Pos.CENTER);
        tabSelectorBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 8px; -fx-padding: 4px;");

        tabPlayingNextBtn = new Button("Playing Next");
        tabPlayingNextBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabPlayingNextBtn, Priority.ALWAYS);
        tabPlayingNextBtn.setOnAction(e -> toggleQueueTab(true));

        tabHistoryBtn = new Button("History");
        tabHistoryBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabHistoryBtn, Priority.ALWAYS);
        tabHistoryBtn.setOnAction(e -> toggleQueueTab(false));

        tabSelectorBox.getChildren().addAll(tabPlayingNextBtn, tabHistoryBtn);

        // Sub-header row: Title on left, Clear/Autoplay on right
        HBox queueSubHeader = new HBox(8);
        queueSubHeader.setAlignment(Pos.CENTER_LEFT);

        queueHeaderTitle = new Label("Playing Next");
        queueHeaderTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        HBox.setHgrow(queueHeaderTitle, Priority.ALWAYS);

        queueClearBtn = new Button("Clear");
        queueClearBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 11px; -fx-cursor: hand;");
        queueClearBtn.setOnAction(e -> clearQueueList());

        queueAutoplayBtn = new Button("∞");
        queueAutoplayBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #0078d4; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
        queueAutoplayBtn.setOnAction(e -> toggleAutoplay());
        queueAutoplayBtn.setTooltip(new Tooltip("Autoplay Loop"));

        queueSubHeader.getChildren().addAll(queueHeaderTitle, queueClearBtn, queueAutoplayBtn);

        // Scrollable song list
        queueScroll = new ScrollPane();
        VBox.setVgrow(queueScroll, Priority.ALWAYS);
        queueScroll.setFitToWidth(true);
        queueScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        queueScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        queueScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        queueListContainer = new VBox(6);
        queueListContainer.setPadding(new Insets(2, 0, 8, 0));
        queueScroll.setContent(queueListContainer);

        queueContainer.getChildren().addAll(tabSelectorBox, queueSubHeader, queueScroll);

        contentStack.getChildren().addAll(artworkSpacer, lyricsScroll, queueContainer);

        // --- FOOTER SECTION ---
        footerBox = new VBox(12);
        footerBox.setPadding(new Insets(16, 20, 20, 20));
        footerBox.setAlignment(Pos.BOTTOM_CENTER);
        footerBox.setMaxWidth(Double.MAX_VALUE);

        // Artwork Metadata Row (Visible only in ARTWORK mode)
        artworkMetadataRow = new BorderPane();
        artworkMetadataRow.setPadding(new Insets(0, 12, 4, 12));

        Button pipBtn = new Button();
        pipBtn.setGraphic(createPipIcon(14, Color.WHITE));
        pipBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6; -fx-opacity: 0.8;");
        pipBtn.setOnMouseEntered(e -> pipBtn.setOpacity(1.0));
        pipBtn.setOnMouseExited(e -> pipBtn.setOpacity(0.8));
        pipBtn.setOnAction(e -> setViewMode(MiniPlayerViewMode.COMPACT));

        VBox artworkMetadataContainer = new VBox(2);
        artworkMetadataContainer.setAlignment(Pos.CENTER_LEFT);

        artworkTitleContainer = new StackPane();
        artworkTitleContainer.setAlignment(Pos.CENTER_LEFT);
        artworkArtistContainer = new StackPane();
        artworkArtistContainer.setAlignment(Pos.CENTER_LEFT);
        artworkQualityBadgeBox = new HBox();
        artworkQualityBadgeBox.setAlignment(Pos.CENTER_LEFT);

        artworkMetadataContainer.getChildren().addAll(artworkTitleContainer, artworkArtistContainer,
                artworkQualityBadgeBox);

        HBox leftAlignedBox = new HBox(8);
        leftAlignedBox.setAlignment(Pos.CENTER_LEFT);
        leftAlignedBox.getChildren().addAll(pipBtn, artworkMetadataContainer);

        artworkMetadataRow.setLeft(leftAlignedBox);

        // Progress Row (Current Time + Slider + Remaining Time)
        HBox progressRow = new HBox(10);
        progressRow.setAlignment(Pos.CENTER);

        currentTimeLbl = new Label("0:00");
        currentTimeLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 10px; -fx-font-family: monospace;");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setStyle("-fx-pref-height: 4px;");
        progressSlider.getStyleClass().add("premium-slider");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                viewModel.seek(newVal.doubleValue());
            }
        });

        remainingTimeLbl = new Label("-0:00");
        remainingTimeLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 10px; -fx-font-family: monospace;");

        progressRow.getChildren().addAll(currentTimeLbl, progressSlider, remainingTimeLbl);

        // Bottom playback control row - Apple Music style: [Vol] [...] [Prev] [Play]
        // [Next] [Lyrics] [Queue]
        HBox controlRow = new HBox();
        controlRow.setAlignment(Pos.CENTER);

        // Left Side Controls (Volume, More) - Apple Music style
        HBox leftSide = new HBox(2);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        volumeBtn = new Button();
        volumeBtn.setGraphic(SVGIcons.createVolumeIcon(14, Color.web("rgba(255,255,255,0.75)")));
        volumeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        volumeBtn.setOnMouseEntered(e -> volumeBtn.setOpacity(1.0));
        volumeBtn.setOnMouseExited(e -> volumeBtn.setOpacity(0.75));
        volumeBtn.setOpacity(0.75);

        volumePopup = new Popup();
        volumePopup.setAutoHide(true);

        HBox volPopupRoot = new HBox(8);
        volPopupRoot.setPadding(new Insets(6, 12, 6, 12));
        volPopupRoot.setAlignment(Pos.CENTER);
        volPopupRoot.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5);");

        SVGPath lowVolIcon = SVGIcons.createVolumeIcon(12, Color.web("rgba(255,255,255,0.6)"));
        SVGPath highVolIcon = SVGIcons.createVolumeIcon(12, Color.WHITE);

        Slider volSlider = new Slider(0, 1, viewModel.volumeProperty().get());
        volSlider.getStyleClass().add("volume-popup-slider");
        volSlider.valueProperty().bindBidirectional(viewModel.volumeProperty());

        volPopupRoot.getChildren().addAll(lowVolIcon, volSlider, highVolIcon);
        volumePopup.getContent().add(volPopupRoot);

        volumeBtn.setOnAction(e -> {
            if (volumePopup.isShowing()) {
                volumePopup.hide();
            } else {
                javafx.geometry.Bounds bounds = volumeBtn.localToScreen(volumeBtn.getBoundsInLocal());
                if (bounds != null) {
                    volumePopup.show(volumeBtn, bounds.getMinX() - 80, bounds.getMinY() - 40);
                }
            }
        });

        Button moreBtn = new Button();
        moreBtn.setGraphic(createMoreIcon(13, Color.web("rgba(255,255,255,0.75)")));
        moreBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        moreBtn.setOpacity(0.75);
        moreBtn.setOnMouseEntered(e -> moreBtn.setOpacity(1.0));
        moreBtn.setOnMouseExited(e -> moreBtn.setOpacity(0.75));
        moreBtn.setOnAction(e -> showMoreMenu(moreBtn));

        leftSide.getChildren().addAll(volumeBtn, moreBtn);

        // Center Playback Buttons - Apple Music style: [Prev] [Play] [Next]
        HBox centerControls = new HBox(16);
        centerControls.setAlignment(Pos.CENTER);

        Button prevBtn = new Button();
        prevBtn.setGraphic(SVGIcons.createPreviousIcon(18, Color.WHITE));
        prevBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        prevBtn.setOnMouseEntered(e -> prevBtn.setOpacity(0.7));
        prevBtn.setOnMouseExited(e -> prevBtn.setOpacity(1.0));
        prevBtn.setOnAction(e -> viewModel.previous());

        playPauseBtn = new Button();
        playPauseBtn.setGraphic(SVGIcons.createPlayIcon(20, Color.WHITE));
        playPauseBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        playPauseBtn.setOnAction(e -> viewModel.togglePlayPause());
        playPauseBtn.setOnMouseEntered(e -> playPauseBtn.setOpacity(0.7));
        playPauseBtn.setOnMouseExited(e -> playPauseBtn.setOpacity(1.0));

        Button nextBtn = new Button();
        nextBtn.setGraphic(SVGIcons.createNextIcon(18, Color.WHITE));
        nextBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        nextBtn.setOnMouseEntered(e -> nextBtn.setOpacity(0.7));
        nextBtn.setOnMouseExited(e -> nextBtn.setOpacity(1.0));
        nextBtn.setOnAction(e -> viewModel.next());

        centerControls.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);

        // Right Side Controls - Apple Music style: [Lyrics] [Queue]
        HBox rightSide = new HBox(2);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        lyricsBtn = new Button();
        lyricsBtn.setGraphic(SVGIcons.createLyricsIcon(14, Color.web("rgba(255,255,255,0.75)")));
        lyricsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        lyricsBtn.setOpacity(0.75);
        lyricsBtn.setOnMouseEntered(e -> lyricsBtn.setOpacity(1.0));
        lyricsBtn.setOnMouseExited(e -> lyricsBtn.setOpacity(0.75));
        lyricsBtn.setTooltip(new Tooltip("Lyrics"));
        lyricsBtn.setOnAction(e -> toggleLyricsMode());

        queueBtn = new Button();
        queueBtn.setGraphic(SVGIcons.createQueueIcon(14, Color.web("rgba(255,255,255,0.75)")));
        queueBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        queueBtn.setOpacity(0.75);
        queueBtn.setOnMouseEntered(e -> queueBtn.setOpacity(1.0));
        queueBtn.setOnMouseExited(e -> queueBtn.setOpacity(0.75));
        queueBtn.setTooltip(new Tooltip("Play Queue"));
        queueBtn.setOnAction(e -> toggleQueueMode());

        rightSide.getChildren().addAll(lyricsBtn, queueBtn);

        controlRow.getChildren().addAll(leftSide, centerControls, rightSide);

        footerBox.getChildren().addAll(artworkMetadataRow, progressRow, controlRow);

        // Assemble VBox hierarchy into StackPane; windowControls floats at TOP_RIGHT
        mainLayout.getChildren().addAll(headerBox, contentStack, footerBox);
        root.getChildren().addAll(artworkView, dimOverlay, mainLayout, windowControls);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        setScene(scene);

        // Setup Hover Animations for Artwork mode (fades footerBox and headerBox)
        footerBox.setOpacity(0.0);
        headerBox.setOpacity(0.3);

        fadeInBottom = new FadeTransition(Duration.millis(250), footerBox);
        fadeInBottom.setToValue(1.0);
        fadeOutBottom = new FadeTransition(Duration.millis(300), footerBox);
        fadeOutBottom.setToValue(0.0);

        fadeInTop = new FadeTransition(Duration.millis(200), headerBox);
        fadeInTop.setToValue(1.0);
        fadeOutTop = new FadeTransition(Duration.millis(250), headerBox);
        fadeOutTop.setToValue(0.3);

        root.setOnMouseEntered(e -> {
            if (currentMode == MiniPlayerViewMode.ARTWORK) {
                fadeOutBottom.stop();
                fadeOutTop.stop();
                fadeInBottom.play();
                fadeInTop.play();
            }
        });

        root.setOnMouseExited(e -> {
            if (currentMode == MiniPlayerViewMode.ARTWORK) {
                fadeInBottom.stop();
                fadeInTop.stop();
                fadeOutBottom.play();
                fadeOutTop.play();
            }
        });

        // Initialize Quality Details Popover
        qualityPopup = new Popup();
        qualityPopup.setAutoHide(true);

        VBox qPopupRoot = new VBox(10);
        qPopupRoot.setPadding(new Insets(15, 20, 15, 20));
        qPopupRoot.setAlignment(Pos.CENTER);
        qPopupRoot.setPrefWidth(240);
        qPopupRoot.setStyle(
                "-fx-background-color: #202020; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);");

        SVGPath largeWave = SVGIcons.createWaveIcon(32, Color.web("rgba(255,255,255,0.75)"));

        qPopupHeader = new Label("Lossless");
        qPopupHeader.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        qPopupDetails = new Label("24-bit 48 kHz ALAC");
        qPopupDetails.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");

        qPopupRoot.getChildren().addAll(largeWave, qPopupHeader, qPopupDetails);
        qualityPopup.getContent().add(qPopupRoot);

        setupBindings();
        setViewMode(MiniPlayerViewMode.ARTWORK);
    }

    private void setupBindings() {
        viewModel.currentSongProperty().addListener(songListener);
        viewModel.isPlayingProperty().addListener(playListener);
        viewModel.currentTimeProperty().addListener(timeListener);
        viewModel.totalDurationProperty().addListener(durationListener);
        viewModel.volumeProperty().addListener(volumeListener);
        viewModel.isMutedProperty().addListener(muteListener);
        viewModel.activeLyricLineIndexProperty().addListener(lyricIndexListener);
        viewModel.getQueue().addListener(queueListener);
        viewModel.getLyricsLines().addListener(lyricsListListener);

        updateSong(viewModel.currentSongProperty().get());
        updatePlayState(viewModel.isPlayingProperty().get());
        updateVolumeState(viewModel.volumeProperty().get(), viewModel.isMutedProperty().get());
        updatePinState();
    }

    private void setViewMode(MiniPlayerViewMode mode) {
        currentMode = mode;

        if (mode == MiniPlayerViewMode.ARTWORK) {
            animateWindowSize(360, 360);

            // Show artwork elements
            artworkView.setVisible(true);
            dimOverlay.setVisible(true);

            // Hide sub containers
            lyricsScroll.setVisible(false);
            lyricsScroll.setManaged(false);
            queueContainer.setVisible(false);
            queueContainer.setManaged(false);

            // Disable metadata thumbnail header items
            headerThumbnailContainer.setVisible(false);
            headerThumbnailContainer.setManaged(false);
            headerMetadataContainer.setVisible(false);
            headerMetadataContainer.setManaged(false);

            // Enable hover behavior (initial state is faded out)
            headerBox.setOpacity(0.3);
            footerBox.setOpacity(0.0);

            // Button highlights
            lyricsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");

            if (queueBtn != null) {
                queueBtn.setGraphic(SVGIcons.createQueueIcon(12, Color.WHITE));
                queueBtn.setTooltip(new Tooltip("Play Queue"));
                queueBtn.setOnAction(e -> toggleQueueMode());
                queueBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
            }

            // Show artwork metadata row
            artworkMetadataRow.setVisible(true);
            artworkMetadataRow.setManaged(true);

            if (restoreFromThumbBtn != null) {
                restoreFromThumbBtn.setOpacity(0.0);
            }

            // Reset fsBtn action for Artwork mode
            if (fsBtn != null) {
                fsBtn.setTooltip(new Tooltip("Fullscreen Mode"));
                fsBtn.setOnAction(e -> openFullScreenPlayer());
            }

            // Apply solid transparency styling to glass overlay in artwork mode
            footerBox.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, rgba(20, 20, 20, 0.4) 0%, rgba(15, 15, 15, 0.75) 30%, rgba(10, 10, 10, 0.95) 100%);"
                            +
                            "-fx-background-radius: 0 0 16 16;" +
                            "-fx-border-color: rgba(255, 255, 255, 0.08) none none none;" +
                            "-fx-border-width: 1px;");
            footerBox.setPadding(new Insets(16, 20, 20, 20));
        } else if (mode == MiniPlayerViewMode.COMPACT) {
            previousCompactMode = mode;

            animateWindowSize(360, 180);

            // Hide artwork metadata row
            artworkMetadataRow.setVisible(false);
            artworkMetadataRow.setManaged(false);

            // Dynamic Apple Music-style background from album art dominant color
            artworkView.setVisible(false);
            dimOverlay.setVisible(false);
            applyDynamicBackground();
            // Make progress bar and time labels blend with the dynamic background
            currentTimeLbl.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 10px; -fx-font-family: monospace;");
            remainingTimeLbl.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 10px; -fx-font-family: monospace;");

            if (restoreFromThumbBtn != null) {
                restoreFromThumbBtn.setOpacity(0.0);
            }

            // Enable header thumbnail items
            headerThumbnailContainer.setVisible(true);
            headerThumbnailContainer.setManaged(true);
            headerMetadataContainer.setVisible(true);
            headerMetadataContainer.setManaged(true);

            // Keep header and footer always visible
            headerBox.setOpacity(1.0);
            footerBox.setOpacity(1.0);

            // Solid dark panel styling for footer in content modes
            footerBox.setStyle(
                    "-fx-background-color: #121212; -fx-border-color: rgba(255, 255, 255, 0.04) none none none; -fx-border-width: 1px;");
            footerBox.setPadding(new Insets(12, 20, 16, 20));

            // Hide scroll containers
            lyricsScroll.setVisible(false);
            lyricsScroll.setManaged(false);
            queueContainer.setVisible(false);
            queueContainer.setManaged(false);

            // Set size of the cover art thumbnail dynamically
            double thumbSize = 80;
            headerThumbnailContainer.setPrefSize(thumbSize, thumbSize);
            headerThumbnailContainer.setMinSize(thumbSize, thumbSize);
            headerThumbnailContainer.setMaxSize(thumbSize, thumbSize);

            headerThumbnailView.setFitWidth(thumbSize);
            headerThumbnailView.setFitHeight(thumbSize);

            thumbClip.setWidth(thumbSize);
            thumbClip.setHeight(thumbSize);
            thumbClip.setArcWidth(12);
            thumbClip.setArcHeight(12);

            // Update hover button icon, action and tooltip
            restoreFromThumbBtn.setGraphic(createExpandIcon(12, Color.WHITE));
            restoreFromThumbBtn.setTooltip(new Tooltip("Expand Album"));
            restoreFromThumbBtn.setOnAction(e -> setViewMode(MiniPlayerViewMode.ARTWORK));

            if (fsBtn != null) {
                fsBtn.setTooltip(new Tooltip("Expand Album View"));
                fsBtn.setOnAction(e -> setViewMode(MiniPlayerViewMode.ARTWORK));
            }

            if (queueBtn != null) {
                queueBtn.setGraphic(SVGIcons.createQueueIcon(14, Color.web("rgba(255,255,255,0.75)")));
                queueBtn.setTooltip(new Tooltip("Play Queue"));
                queueBtn.setOnAction(e -> toggleQueueMode());
                queueBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                queueBtn.setOpacity(0.75);
            }

            lyricsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
            lyricsBtn.setOpacity(0.75);
            footerBox.setPadding(new Insets(8, 20, 14, 20));
        } else {
            animateWindowSize(360, 600);

            // Hide artwork metadata row
            artworkMetadataRow.setVisible(false);
            artworkMetadataRow.setManaged(false);

            // Restore solid dark background (leaving COMPACT dynamic color)
            artworkView.setVisible(false);
            dimOverlay.setVisible(false);
            root.setStyle("-fx-background-color: #1a1a1a;");
            headerBox.setStyle("-fx-background-color: transparent;");

            // Enable header thumbnail items
            headerThumbnailContainer.setVisible(true);
            headerThumbnailContainer.setManaged(true);
            headerMetadataContainer.setVisible(true);
            headerMetadataContainer.setManaged(true);

            // Keep header and footer always visible
            headerBox.setOpacity(1.0);
            footerBox.setOpacity(1.0);

            // Solid dark panel styling for footer in content modes
            footerBox.setStyle(
                    "-fx-background-color: #121212; -fx-border-color: rgba(255, 255, 255, 0.04) none none none; -fx-border-width: 1px;");
            footerBox.setPadding(new Insets(12, 20, 16, 20));
            // Restore normal time label colors
            currentTimeLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 10px; -fx-font-family: monospace;");
            remainingTimeLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 10px; -fx-font-family: monospace;");

            // Reset thumbnail to default size and action for lyrics/queue mode
            headerThumbnailContainer.setPrefSize(40, 40);
            headerThumbnailContainer.setMinSize(40, 40);
            headerThumbnailContainer.setMaxSize(40, 40);

            headerThumbnailView.setFitWidth(40);
            headerThumbnailView.setFitHeight(40);

            thumbClip.setWidth(40);
            thumbClip.setHeight(40);
            thumbClip.setArcWidth(8);
            thumbClip.setArcHeight(8);

            restoreFromThumbBtn.setGraphic(createPipIcon(12, Color.WHITE));
            restoreFromThumbBtn.setTooltip(new Tooltip("Minimize Album"));
            restoreFromThumbBtn.setOnAction(e -> setViewMode(MiniPlayerViewMode.ARTWORK));
            restoreFromThumbBtn.setOpacity(0.0);

            if (fsBtn != null) {
                fsBtn.setTooltip(new Tooltip("Fullscreen Mode"));
                fsBtn.setOnAction(e -> openFullScreenPlayer());
            }

            if (queueBtn != null) {
                queueBtn.setGraphic(SVGIcons.createQueueIcon(12, Color.WHITE));
                queueBtn.setTooltip(new Tooltip("Play Queue"));
                queueBtn.setOnAction(e -> toggleQueueMode());
            }

            if (mode == MiniPlayerViewMode.LYRICS) {
                lyricsScroll.setVisible(true);
                lyricsScroll.setManaged(true);
                queueContainer.setVisible(false);
                queueContainer.setManaged(false);

                lyricsBtn.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.15); -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 6;");

                if (queueBtn != null) {
                    queueBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
                }

                rebuildLyrics();
            } else {
                lyricsScroll.setVisible(false);
                lyricsScroll.setManaged(false);
                queueContainer.setVisible(true);
                queueContainer.setManaged(true);

                lyricsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");

                if (queueBtn != null) {
                    queueBtn.setStyle(
                            "-fx-background-color: rgba(255, 255, 255, 0.15); -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 6;");
                }

                toggleQueueTab(isQueueTabActive); // Triggers rebuild of the active queue tab
            }
        }
    }

    private void animateWindowSize(double targetWidth, double targetHeight) {
        DoubleProperty widthAnim = new SimpleDoubleProperty(this.getWidth());
        DoubleProperty heightAnim = new SimpleDoubleProperty(this.getHeight());
        widthAnim.addListener((obs, oldVal, newVal) -> this.setWidth(newVal.doubleValue()));
        heightAnim.addListener((obs, oldVal, newVal) -> this.setHeight(newVal.doubleValue()));

        Timeline timeline = new Timeline();
        KeyValue kvWidth = new KeyValue(widthAnim, targetWidth);
        KeyValue kvHeight = new KeyValue(heightAnim, targetHeight);
        KeyFrame kf = new KeyFrame(Duration.millis(250), kvWidth, kvHeight);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    private void toggleLyricsMode() {
        if (currentMode == MiniPlayerViewMode.LYRICS) {
            setViewMode(previousCompactMode);
        } else {
            setViewMode(MiniPlayerViewMode.LYRICS);
        }
    }

    private void toggleQueueMode() {
        if (currentMode == MiniPlayerViewMode.QUEUE) {
            setViewMode(previousCompactMode);
        } else {
            setViewMode(MiniPlayerViewMode.QUEUE);
        }
    }

    private void toggleQueueTab(boolean playingNext) {
        isQueueTabActive = playingNext;
        if (isQueueTabActive) {
            tabPlayingNextBtn.setStyle(
                    "-fx-background-color: #000000; -fx-text-fill: white; -fx-background-radius: 6px; -fx-cursor: hand;");
            tabHistoryBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-cursor: hand;");
            queueHeaderTitle.setText("Playing Next");
            queueClearBtn.setVisible(true);
            queueAutoplayBtn.setVisible(true);
        } else {
            tabPlayingNextBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-cursor: hand;");
            tabHistoryBtn.setStyle(
                    "-fx-background-color: #000000; -fx-text-fill: white; -fx-background-radius: 6px; -fx-cursor: hand;");
            queueHeaderTitle.setText("History");
            queueClearBtn.setVisible(false);
            queueAutoplayBtn.setVisible(false);
        }
        rebuildQueueList();
    }

    private void toggleAutoplay() {
        autoplayEnabled = !autoplayEnabled;
        if (autoplayEnabled) {
            queueAutoplayBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #0078d4; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
            queueAutoplayBtn.setTooltip(new Tooltip("Autoplay Loop: ON"));
        } else {
            queueAutoplayBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
            queueAutoplayBtn.setTooltip(new Tooltip("Autoplay Loop: OFF"));
        }
    }

    private void clearQueueList() {
        int currentIdx = viewModel.currentQueueIndexProperty().get();
        if (currentIdx != -1 && currentIdx < viewModel.getQueue().size()) {
            List<Song> songsToKeep = new ArrayList<>(viewModel.getQueue().subList(0, currentIdx + 1));
            viewModel.getQueue().setAll(songsToKeep);
        } else {
            viewModel.getQueue().clear();
        }
        rebuildQueueList();
    }

    private void rebuildLyrics() {
        lyricsLinesContainer.getChildren().clear();
        lyricLabels.clear();
        currentActiveLyricIndex = -1;

        List<LyricLine> lines = viewModel.getLyricsLines();
        if (lines.isEmpty()) {
            Label placeholder = new Label("No Lyrics Available");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 18px; -fx-font-weight: bold;");
            lyricsLinesContainer.getChildren().add(placeholder);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            Label label = new Label(line.getText());
            label.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 18px; -fx-font-weight: bold;");
            label.setWrapText(true);
            label.setAlignment(Pos.CENTER);
            label.setMaxWidth(Double.MAX_VALUE);

            label.setOnMouseClicked(e -> {
                viewModel.seek(line.getTimestamp() / 1000.0);
            });

            lyricLabels.add(label);
            lyricsLinesContainer.getChildren().add(label);
        }

        updateActiveLyricLine(viewModel.activeLyricLineIndexProperty().get());
    }

    private void updateActiveLyricLine(int activeIndex) {
        if (activeIndex == currentActiveLyricIndex || lyricLabels.isEmpty())
            return;
        currentActiveLyricIndex = activeIndex;

        Platform.runLater(() -> {
            for (int i = 0; i < lyricLabels.size(); i++) {
                Label label = lyricLabels.get(i);

                double targetScale = 1.0;
                double targetOpacity = 0.45;

                if (i == activeIndex) {
                    targetScale = 1.08;
                    targetOpacity = 1.0;
                    label.setTextFill(Color.WHITE);
                } else if (i < activeIndex) {
                    targetScale = 0.96;
                    targetOpacity = 0.18;
                    label.setTextFill(Color.web("#888888"));
                } else {
                    targetScale = 1.0;
                    targetOpacity = 0.45;
                    label.setTextFill(Color.web("#cccccc"));
                }

                FadeTransition ft = new FadeTransition(Duration.millis(250), label);
                ft.setToValue(targetOpacity);
                ft.play();

                label.setScaleX(targetScale);
                label.setScaleY(targetScale);
            }

            scrollToActiveLyricLine();
        });
    }

    private void scrollToActiveLyricLine() {
        if (currentActiveLyricIndex < 0 || currentActiveLyricIndex >= lyricLabels.size())
            return;

        Label activeLabel = lyricLabels.get(currentActiveLyricIndex);
        double layoutY = activeLabel.getLayoutY();
        double containerHeight = lyricsLinesContainer.getHeight();
        double scrollPaneHeight = lyricsScroll.getHeight();

        if (containerHeight <= scrollPaneHeight)
            return;

        double targetV = (layoutY - (scrollPaneHeight / 2.0) + (activeLabel.getHeight() / 2.0))
                / (containerHeight - scrollPaneHeight);

        targetV = Math.max(0.0, Math.min(1.0, targetV));

        if (lyricScrollTimeline != null) {
            lyricScrollTimeline.stop();
        }

        lyricScrollTimeline = new Timeline(new KeyFrame(Duration.millis(350),
                new KeyValue(lyricsScroll.vvalueProperty(), targetV)));
        lyricScrollTimeline.play();
    }

    private void rebuildQueueList() {
        queueListContainer.getChildren().clear();

        List<Song> songs;

        if (isQueueTabActive) {
            List<Song> rawQueue = viewModel.getQueue();
            int currentIdx = viewModel.currentQueueIndexProperty().get();
            songs = new ArrayList<>();
            for (int i = currentIdx + 1; i < rawQueue.size(); i++) {
                songs.add(rawQueue.get(i));
            }
        } else {
            songs = viewModel.getRecentlyPlayed();
        }

        if (songs.isEmpty()) {
            Label placeholder = new Label(isQueueTabActive ? "Queue is empty" : "No recently played songs");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 12px; -fx-padding: 20 0 0 0;");
            HBox phBox = new HBox(placeholder);
            phBox.setAlignment(Pos.CENTER);
            queueListContainer.getChildren().add(phBox);
            return;
        }

        for (int i = 0; i < songs.size(); i++) {
            final int indexInSubList = i;
            Song song = songs.get(i);

            HBox cell = new HBox(10);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setStyle("-fx-padding: 6 8; -fx-background-radius: 8px; -fx-cursor: hand;");

            cell.setOnMouseEntered(e -> cell.setStyle(
                    "-fx-padding: 6 8; -fx-background-radius: 8px; -fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.06);"));
            cell.setOnMouseExited(e -> cell.setStyle(
                    "-fx-padding: 6 8; -fx-background-radius: 8px; -fx-cursor: hand; -fx-background-color: transparent;"));

            ImageView thumbView = new ImageView();
            thumbView.setFitWidth(32);
            thumbView.setFitHeight(32);
            Rectangle cellClip = new Rectangle(32, 32);
            cellClip.setArcWidth(6);
            cellClip.setArcHeight(6);
            thumbView.setClip(cellClip);

            if (artworkCache.containsKey(song.getPath())) {
                thumbView.setImage(artworkCache.get(song.getPath()));
            } else {
                byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
                Image img = null;
                if (artBytes != null) {
                    img = new Image(new ByteArrayInputStream(artBytes), 32, 32, true, true);
                }
                artworkCache.put(song.getPath(), img);
                thumbView.setImage(img);
            }

            VBox textCol = new VBox(2);
            textCol.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textCol, Priority.ALWAYS);
            Song currentPlaying = viewModel.currentSongProperty().get();
            boolean isCurrent = (currentPlaying != null && currentPlaying.getPath().equals(song.getPath()));

            if (isCurrent) {
                javafx.scene.Node titleMarquee = aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                        "-fx-text-fill: #0078d4; -fx-font-size: 12px; -fx-font-weight: bold;", 180);
                javafx.scene.Node artistMarquee = aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                        "-fx-text-fill: #0078d4; -fx-opacity: 0.85; -fx-font-size: 10px;", 180);
                Button cellBadgeBtn = createQualityBadgeButton(song, 8, 9);
                textCol.getChildren().addAll(titleMarquee, artistMarquee, cellBadgeBtn);
            } else {
                Label titleLbl = new Label(song.getTitle());
                titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

                Label artistLbl = new Label(song.getArtist());
                artistLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 10px;");

                Button cellBadgeBtn = createQualityBadgeButton(song, 8, 9);
                textCol.getChildren().addAll(titleLbl, artistLbl, cellBadgeBtn);
            }

            Label durationLbl = new Label(formatTime(song.getDuration()));
            durationLbl.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");

            cell.getChildren().addAll(thumbView, textCol, durationLbl);

            cell.setOnMouseClicked(e -> {
                if (isQueueTabActive) {
                    int currentIdx = viewModel.currentQueueIndexProperty().get();
                    viewModel.playQueueIndex(currentIdx + 1 + indexInSubList);
                } else {
                    viewModel.play(song);
                }
            });

            queueListContainer.getChildren().add(cell);
        }
    }

    private void toggleAlwaysOnTop() {
        isPinned = !isPinned;
        setAlwaysOnTop(isPinned);
        updatePinState();
    }

    private void updatePinState() {
        Platform.runLater(() -> {
            if (isPinned) {
                pinBtn.setGraphic(createPinIcon(11, Color.web("#0078d4")));
                pinBtn.setTooltip(new Tooltip("Always on Top: ON"));
            } else {
                pinBtn.setGraphic(createPinIcon(11, Color.WHITE));
                pinBtn.setTooltip(new Tooltip("Always on Top: OFF"));
            }
        });
    }

    private void updateSong(Song song) {
        if (song != null) {
            byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            Image artworkImage = null;
            if (artBytes != null) {
                artworkImage = new Image(new ByteArrayInputStream(artBytes), 360, 360, false, true);
            }
            artworkView.setImage(artworkImage);

            if (artBytes != null) {
                Image thumbImg = new Image(new ByteArrayInputStream(artBytes), 80, 80, true, true);
                headerThumbnailView.setImage(thumbImg);
                // Extract dominant color for Apple Music style dynamic background
                dominantColor = extractDominantColor(thumbImg);
                applyDynamicBackground();
            } else {
                headerThumbnailView.setImage(null);
                dominantColor = Color.web("#2a2a2a");
                applyDynamicBackground();
            }

            headerTitleContainer.getChildren().setAll(
                    aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;", 180));
            headerArtistContainer.getChildren().setAll(
                    aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                            "-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 10px;", 180));

            artworkTitleContainer.getChildren().setAll(
                    aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;", 220));
            artworkArtistContainer.getChildren().setAll(
                    aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                            "-fx-text-fill: #b3b3b3; -fx-font-size: 11px;", 220));
            updateQualityBadge(song);

            if (currentMode == MiniPlayerViewMode.LYRICS) {
                rebuildLyrics();
            }
        } else {
            artworkView.setImage(null);
            headerThumbnailView.setImage(null);
            dominantColor = Color.web("#2a2a2a");
            applyDynamicBackground();

            Label defaultTitle = new Label("Not Playing");
            defaultTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
            headerTitleContainer.getChildren().setAll(defaultTitle);
            headerArtistContainer.getChildren().clear();
            headerQualityBadgeBox.getChildren().clear();

            Label defaultArtTitle = new Label("Not Playing");
            defaultArtTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
            artworkTitleContainer.getChildren().setAll(defaultArtTitle);
            artworkArtistContainer.getChildren().clear();
            artworkQualityBadgeBox.getChildren().clear();
        }
    }

    /**
     * Extracts the dominant color from an album artwork image by sampling
     * a grid of pixels and averaging their hue/saturation weighted by vibrancy.
     */
    private Color extractDominantColor(Image image) {
        try {
            javafx.scene.image.PixelReader pr = image.getPixelReader();
            if (pr == null)
                return Color.web("#2a2a2a");
            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            if (w <= 0 || h <= 0)
                return Color.web("#2a2a2a");

            double rSum = 0, gSum = 0, bSum = 0;
            int count = 0;
            int step = Math.max(1, Math.min(w, h) / 16);

            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    Color c = pr.getColor(x, y);
                    double brightness = c.getBrightness();
                    double saturation = c.getSaturation();
                    // Weight by saturation to prefer vivid colors, skip near-black/near-white
                    if (brightness > 0.08 && brightness < 0.92 && saturation > 0.1) {
                        double weight = saturation * saturation;
                        rSum += c.getRed() * weight;
                        gSum += c.getGreen() * weight;
                        bSum += c.getBlue() * weight;
                        count += weight;
                    }
                }
            }

            if (count < 1)
                return Color.web("#2a2a2a");

            double r = rSum / count;
            double g = gSum / count;
            double b = bSum / count;

            // Deepen the color: reduce brightness to ~40-55% so it becomes a rich dark tone
            Color raw = Color.color(
                    Math.min(1.0, r),
                    Math.min(1.0, g),
                    Math.min(1.0, b));
            // Shift to HSB, reduce brightness to create a dark muted variant
            double hue = raw.getHue();
            double sat = Math.min(1.0, raw.getSaturation() * 0.85);
            double bri = Math.min(0.45, raw.getBrightness() * 0.65);
            return Color.hsb(hue, sat, bri);
        } catch (Exception ex) {
            return Color.web("#2a2a2a");
        }
    }

    /**
     * Applies the dynamic Apple Music-style color background to the COMPACT mode.
     * In ARTWORK/LYRICS/QUEUE modes, the existing styling is preserved.
     */
    private void applyDynamicBackground() {
        if (currentMode != MiniPlayerViewMode.COMPACT)
            return;
        String hex = toHexColor(dominantColor);
        String hexDark = toHexColor(dominantColor.darker().darker());
        String hexDarker = toHexColor(Color.color(
                dominantColor.getRed() * 0.25,
                dominantColor.getGreen() * 0.25,
                dominantColor.getBlue() * 0.25));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, " + hex + " 0%, " + hexDark + " 60%, "
                + hexDarker + " 100%);");
        footerBox.setStyle("-fx-background-color: transparent;");
        headerBox.setStyle("-fx-background-color: transparent;");
    }

    private String toHexColor(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private String getQualityText(Song song) {
        if (song == null)
            return "AAC";
        String pathLower = song.getPath().toLowerCase();
        String codec = "AAC";
        if (pathLower.endsWith(".flac")) {
            codec = "FLAC";
        } else if (pathLower.endsWith(".wav")) {
            codec = "WAV";
        } else if (pathLower.endsWith(".mp3")) {
            codec = "MP3";
        } else if (pathLower.endsWith(".m4a")) {
            codec = (song.getBitsPerSample() > 16 || song.isHiRes() || song.getBitRate() > 500) ? "ALAC" : "AAC";
        }

        boolean isHiRes = song.isHiRes();
        if (pathLower.endsWith(".mp3") || (pathLower.endsWith(".m4a") && !codec.equals("ALAC"))) {
            return "AAC High Quality";
        } else {
            return isHiRes ? "Hi-Res Lossless" : "Lossless";
        }
    }

    private Button createQualityBadgeButton(Song song, double waveSize, double fontSize) {
        Button badgeBtn = new Button();
        badgeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 4;");

        HBox content = new HBox(4);
        content.setAlignment(Pos.CENTER_LEFT);
        SVGPath waveIcon = SVGIcons.createWaveIcon(waveSize, Color.web("rgba(255,255,255,0.6)"));
        Label label = new Label(getQualityText(song));
        label.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: " + fontSize + "px; -fx-font-weight: 500;");
        content.getChildren().addAll(waveIcon, label);

        badgeBtn.setGraphic(content);
        badgeBtn.setOnAction(e -> {
            showQualityPopup(song, badgeBtn);
            e.consume();
        });
        return badgeBtn;
    }

    private void showQualityPopup(Song song, Button anchorBtn) {
        if (song == null)
            return;

        String pathLower = song.getPath().toLowerCase();
        String codec = "AAC";
        if (pathLower.endsWith(".flac")) {
            codec = "FLAC";
        } else if (pathLower.endsWith(".wav")) {
            codec = "WAV";
        } else if (pathLower.endsWith(".mp3")) {
            codec = "MP3";
        } else if (pathLower.endsWith(".m4a")) {
            codec = (song.getBitsPerSample() > 16 || song.isHiRes() || song.getBitRate() > 500) ? "ALAC" : "AAC";
        }

        String qualityText = getQualityText(song);
        qPopupHeader.setText(qualityText);

        String rate;
        if (song.getSampleRate() > 0) {
            double khz = song.getSampleRate() / 1000.0;
            if (khz == (int) khz) {
                rate = String.format("%d kHz", (int) khz);
            } else {
                rate = String.format("%.1f kHz", khz);
            }
        } else {
            rate = "44.1 kHz";
        }

        String depth = song.getBitsPerSample() > 0 ? song.getBitsPerSample() + "-bit" : "16-bit";
        qPopupDetails.setText(depth + " " + rate + " " + codec);

        if (qualityPopup.isShowing()) {
            qualityPopup.hide();
        } else {
            javafx.geometry.Bounds bounds = anchorBtn.localToScreen(anchorBtn.getBoundsInLocal());
            if (bounds != null) {
                // Show centered above the button
                qualityPopup.show(anchorBtn, bounds.getMinX() - (240 - bounds.getWidth()) / 2.0,
                        bounds.getMinY() - 110);
            }
        }
    }

    private void updateQualityBadge(Song song) {
        if (song == null) {
            artworkQualityBadgeBox.getChildren().clear();
            headerQualityBadgeBox.getChildren().clear();
            return;
        }

        // Update headerQualityBadgeBox (compact mode badge)
        headerQualityBadgeBox.getChildren().clear();
        Button headerBadgeBtn = createQualityBadgeButton(song, 10, 11);
        headerQualityBadgeBox.getChildren().setAll(headerBadgeBtn);

        // Build clickable quality badge for Artwork mode
        artworkQualityBadgeBox.getChildren().clear();
        Button artworkBadgeBtn = createQualityBadgeButton(song, 10, 11);
        artworkQualityBadgeBox.getChildren().setAll(artworkBadgeBtn);
    }

    private void updatePlayState(boolean playing) {
        Platform.runLater(() -> {
            if (playing) {
                playPauseBtn.setGraphic(SVGIcons.createPauseIcon(16, Color.WHITE));
            } else {
                playPauseBtn.setGraphic(SVGIcons.createPlayIcon(16, Color.WHITE));
            }
        });
    }

    private void updateVolumeState(double volume, boolean muted) {
        Platform.runLater(() -> {
            if (muted || volume == 0) {
                volumeBtn.setGraphic(SVGIcons.createMuteIcon(12, Color.web("#ff5f56")));
            } else {
                volumeBtn.setGraphic(SVGIcons.createVolumeIcon(12, Color.WHITE));
            }
        });
    }

    private void updateTimeLabels(double current, double total) {
        Platform.runLater(() -> {
            currentTimeLbl.setText(formatTime(current));
            remainingTimeLbl.setText(formatRemainingTime(current, total));
        });
    }

    private String formatTime(double seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", mins, secs);
    }

    private String formatRemainingTime(double current, double total) {
        double remaining = total - current;
        if (remaining < 0)
            remaining = 0;
        int mins = (int) (remaining / 60);
        int secs = (int) (remaining % 60);
        return String.format("-%d:%02d", mins, secs);
    }

    private void showMoreMenu(Button anchor) {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.CheckMenuItem alwaysOnTopItem = new javafx.scene.control.CheckMenuItem("Always miniplayer on top");
        alwaysOnTopItem.setSelected(isPinned);
        alwaysOnTopItem.setOnAction(e -> toggleAlwaysOnTop());

        javafx.scene.control.MenuItem restoreItem = new javafx.scene.control.MenuItem("Restore Main Player");
        restoreItem.setOnAction(e -> restoreMainPlayer());

        javafx.scene.control.MenuItem fsItem = new javafx.scene.control.MenuItem("Fullscreen Player");
        fsItem.setOnAction(e -> openFullScreenPlayer());

        javafx.scene.control.MenuItem closeItem = new javafx.scene.control.MenuItem("Exit Miniplayer");
        closeItem.setOnAction(e -> restoreMainPlayer());

        contextMenu.getItems().addAll(alwaysOnTopItem, new javafx.scene.control.SeparatorMenuItem(), restoreItem, fsItem, new javafx.scene.control.SeparatorMenuItem(), closeItem);

        Song current = viewModel.currentSongProperty().get();
        if (current != null) {
            contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            contextMenu.getItems().addAll(MenuUtils.getSongContextMenuItems(current, viewModel));
        }

        contextMenu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
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

    private SVGPath createPipIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent(
                "M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2-.02H3V4.97h18v14.01z");
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    private SVGPath createExpandIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent(
                "M19 19H5V5h7V3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z");
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    private SVGPath createMoreIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent(
                "M6 12c0 1.1.9 2 2 2s2-.9 2-2-.9-2-2-2-2 .9-2 2zm6 0c0 1.1.9 2 2 2s2-.9 2-2-.9-2-2-2-2 .9-2 2zm6 0c0 1.1.9 2 2 2s2-.9 2-2-.9-2-2-2-2 .9-2 2z");
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    private SVGPath createPinIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M16 12V4h1v-2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z");
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    @Override
    public void close() {
        viewModel.currentSongProperty().removeListener(songListener);
        viewModel.isPlayingProperty().removeListener(playListener);
        viewModel.currentTimeProperty().removeListener(timeListener);
        viewModel.totalDurationProperty().removeListener(durationListener);
        viewModel.volumeProperty().removeListener(volumeListener);
        viewModel.isMutedProperty().removeListener(muteListener);
        viewModel.activeLyricLineIndexProperty().removeListener(lyricIndexListener);
        viewModel.getQueue().removeListener(queueListener);
        viewModel.getLyricsLines().removeListener(lyricsListListener);
        super.close();
    }
}
