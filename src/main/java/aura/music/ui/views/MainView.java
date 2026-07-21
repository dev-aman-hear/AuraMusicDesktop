package aura.music.ui.views;

import aura.music.library.LibraryManager;
import aura.music.model.Song;
import aura.music.theme.ThemeEngine;
import aura.music.ui.components.LyricsView;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;

public class MainView extends StackPane {

    private final MainViewModel viewModel;
    private final ThemeEngine themeEngine = ThemeEngine.getInstance();

    // Window Dragging
    private double xOffset = 0;
    private double yOffset = 0;

    // UI Elements
    private ImageView bgImageView;
    private ListView<Song> songListView;
    private VBox rightPanel;
    private Label sectionTitle;

    // Mini Player Elements
    private ImageView miniArtwork;
    private javafx.scene.layout.StackPane miniTitleContainer;
    private javafx.scene.layout.StackPane miniArtistContainer;
    private Button miniPlayPause;
    private Slider miniProgress;
    private Label miniTime;
    private Label miniDuration;
    private Slider volumeSlider;
    private final java.util.Map<String, Image> queueArtCache = new java.util.HashMap<>();
    private final javafx.collections.ObservableList<Song> filteredQueue = FXCollections.observableArrayList();

    private FullScreenPlayer fullScreenPlayer;
    private StackPane mainLayout;

    // Switchable center sections
    private StackPane centerContentContainer;
    private VBox centerSection;
    private ScrollPane albumsScrollPane;
    private GridPane albumsGridPane;
    private ScrollPane artistsScrollPane;
    private GridPane artistsGridPane;
    private ScrollPane genresScrollPane;
    private GridPane genresGridPane;
    private HomeView homeView;
    private OnlineMusicView onlineMusicView;
    private AlbumDetailView albumDetailView;
    private final java.util.List<Button> sidebarButtons = new java.util.ArrayList<>();
    private Region sidebarSpacer;

    // Keep the UI bounded: render a small ready-ahead window instead of creating
    // a card (and starting artwork extraction) for every library item at once.
    private static final int INITIAL_BROWSE_ITEMS = 60;
    private static final int BROWSE_PREFETCH_ITEMS = 40;
    private static final double BROWSE_PREFETCH_THRESHOLD = 0.70;
    private java.util.List<Song> albumCatalog = java.util.List.of();
    private java.util.List<Song> artistCatalog = java.util.List.of();
    private java.util.List<Song> genreCatalog = java.util.List.of();
    private int loadedAlbumCards;
    private int loadedArtistCards;
    private int loadedGenreCards;
    private boolean isLoadingAlbumCards;
    private boolean isLoadingArtistCards;
    private boolean isLoadingGenreCards;
    private javafx.scene.Node currentActiveView;

    private SettingsView settingsView;
    private PlaylistView playlistView;
    private ListView<String> albumsListView;
    private ListView<String> artistsListView;
    private ListView<String> genresListView;

    private final javafx.beans.property.BooleanProperty albumsGridViewEnabled = new javafx.beans.property.SimpleBooleanProperty(
            true);
    private final javafx.beans.property.BooleanProperty artistsGridViewEnabled = new javafx.beans.property.SimpleBooleanProperty(
            true);
    private final javafx.beans.property.BooleanProperty genresGridViewEnabled = new javafx.beans.property.SimpleBooleanProperty(
            true);

    private VBox sidebar;
    private Button backBtn;
    private Button toggleSidebarBtn;
    private Button shuffleBtn;
    private Button prevBtn;
    private Button nextBtn;
    private Button repeatBtn;
    private Button speakerBtn;
    private Button lyricsBtn;
    private Button queueBtn;
    private Button homeBtn;
    private Button browseBtn;
    private Button onlineBtn;
    private Button albumsBtn;
    private Button artistsBtn;
    private Button genresBtn;
    private Button songsBtn;
    private Button favoritesBtn;
    private Button playlistsBtn;
    private Button settingsBtn;
    private Label logo;
    private Label navHeader;
    private Label libraryHeader;
    private TextField searchField;
    private javafx.scene.shape.SVGPath searchIcon;
    private Label profileName;
    private Label statsLabel;
    private Label avatarText;
    private Button addFolderBtn;
    private final javafx.beans.property.BooleanProperty sidebarExpanded = new javafx.beans.property.SimpleBooleanProperty(
            false);

    private Pane bgDarkening;
    private StackPane initialImportOverlay;
    private Label initialImportStatus;
    private boolean showingInitialImport;

    public MainView(MainViewModel viewModel) {
        this.viewModel = viewModel;
        getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());

        // 1. Acrylic Blur Background
        bgImageView = new ImageView();
        bgImageView.setPreserveRatio(false);
        bgImageView.fitWidthProperty().bind(widthProperty());
        bgImageView.fitHeightProperty().bind(heightProperty());
        bgImageView.setEffect(new BoxBlur(100, 100, 3));

        bgDarkening = new Pane();
        bgDarkening.setStyle("-fx-background-color: rgba(18, 18, 18, 0.85);"); // #121212 base

        getChildren().addAll(bgImageView, bgDarkening);

        // 2. Main BorderPane Layout
        BorderPane rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: transparent;");

        // --- LEFT SIDEBAR (Collapsible, Full Height) ---
        sidebar = createSidebar();

        sidebarSpacer = new Region();
        double initialSpacerWidth = sidebarExpanded.get() ? 255 : 87;
        sidebarSpacer.setPrefWidth(initialSpacerWidth);
        sidebarSpacer.setMinWidth(initialSpacerWidth);
        sidebarSpacer.setMaxWidth(initialSpacerWidth);
        rootLayout.setLeft(sidebarSpacer);

        // --- CENTER CONTENT CONTAINER ---
        centerContentContainer = new StackPane();
        VBox.setVgrow(centerContentContainer, Priority.ALWAYS);

        // Create Song List Section
        centerSection = createCenterSection();

        // Create Home View
        homeView = new HomeView(viewModel, this::showFavorites, this::showAlbumDetail, this::selectGenre, this::selectArtist);

        // Online discovery is intentionally separate from the local-library Browse page.
        onlineMusicView = new OnlineMusicView(viewModel);

        // Create Album Detail View
        albumDetailView = new AlbumDetailView(viewModel);

        // Create Settings View
        settingsView = new SettingsView(albumsGridViewEnabled, artistsGridViewEnabled, genresGridViewEnabled, viewModel.miniplayerAlwaysOnTopProperty(), viewModel.onlineMusicEnabledProperty(), viewModel.youtubeApiKeyProperty(), viewModel.lyricTextSizeProperty(), () -> {
            if (playlistView != null) {
                playlistView.refreshPlaylists();
            }
        });

        // Create Playlist View
        playlistView = new PlaylistView(viewModel);

        // Initialize Native Windows Media Controls
        new aura.music.ui.components.SystemMediaControls(viewModel);

        albumsGridViewEnabled.addListener((obs, oldVal, newVal) -> {
            if (currentActiveView == centerSection && "Albums".equals(sectionTitle.getText())) {
                showAlbumsGrid();
            }
        });
        artistsGridViewEnabled.addListener((obs, oldVal, newVal) -> {
            if (currentActiveView == centerSection && "Artists".equals(sectionTitle.getText())) {
                showArtistsGrid();
            }
        });
        genresGridViewEnabled.addListener((obs, oldVal, newVal) -> {
            if (currentActiveView == centerSection && "Genres".equals(sectionTitle.getText())) {
                showGenresGrid();
            }
        });

        centerContentContainer.getChildren().addAll(centerSection, homeView, onlineMusicView, albumDetailView, settingsView,
                playlistView);

        // --- TOP PLAYBACK BAR (Next to Sidebar) ---
        HBox playerBar = createPlayerBar();

        VBox rightArea = new VBox(0);
        rightArea.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(rightArea, Priority.ALWAYS);
        rightArea.getChildren().addAll(playerBar, centerContentContainer);

        rootLayout.setCenter(rightArea);

        // --- RIGHT PANEL (Collapsible Floating Queue & Lyrics Drawer, Width 360px) ---
        rightPanel = createRightPanel();
        rightPanel.setPrefWidth(360);
        rightPanel.setMaxWidth(360);
        rightPanel.setVisible(false);

        mainLayout = new StackPane(rootLayout, sidebar, rightPanel);
        StackPane.setAlignment(rootLayout, Pos.TOP_LEFT);
        StackPane.setAlignment(sidebar, Pos.TOP_LEFT);
        StackPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        StackPane.setAlignment(rightPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(rightPanel, new Insets(110, 20, 20, 20)); // Float below top player bar

        // Bind heights to prevent layout shifts from shadows
        rootLayout.prefHeightProperty().bind(heightProperty());
        rootLayout.maxHeightProperty().bind(heightProperty());
        sidebar.prefHeightProperty().bind(heightProperty().subtract(30));
        sidebar.maxHeightProperty().bind(heightProperty().subtract(30));

        getChildren().add(mainLayout);
        createInitialImportOverlay();

        LibraryManager.getInstance().addListener(new LibraryManager.LibraryListener() {
            @Override public void onSongAdded(Song song) { }
            @Override public void onSongRemoved(Song song) { }
            @Override public void onSongUpdated(Song song) { }

            @Override public void onScanProgress(int songsFound) {
                Platform.runLater(() -> updateInitialImportStatus(songsFound));
            }

            @Override public void onScanFinished(File folder, int songsFound) {
                Platform.runLater(() -> finishInitialImport(songsFound));
            }
        });

        // Setup Window Dragging on the top player bar
        setupWindowDragging(playerBar);

        // Dynamic theme styling
        themeEngine.backgroundColorProperty().addListener((obs, oldCol, newCol) -> {
            String hex = toHexString(newCol);
            String accentHex = toHexString(themeEngine.accentColorProperty().get());
            setStyle(String.format("--accent-color: %s; -fx-background-color: %s;", accentHex, hex));
        });

        // Listen for window size changes to handle responsive sidebar layout
        widthProperty().addListener((obs, oldVal, newVal) -> {
            Stage stage = (Stage) getScene().getWindow();
            boolean maximized = stage != null && stage.isMaximized();
            updateSidebarLayout(maximized, newVal.doubleValue());
        });

        // Initially hide all except homeView
        centerSection.setVisible(false);
        onlineMusicView.setVisible(false);
        albumDetailView.setVisible(false);
        settingsView.setVisible(false);
        playlistView.setVisible(false);
        homeView.setVisible(true);
        currentActiveView = homeView;
        updateBackBtnVisibility();

        viewModel.onlineMusicEnabledProperty().addListener((obs, wasEnabled, enabled) -> {
            if (!enabled && currentActiveView == onlineMusicView) {
                showHomeView(false);
            }
        });

        // Rebuild the small browse indexes only when the library changes.
        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            invalidateBrowseCatalogs();
        });

        // Initialize bindings
        setupBindings();

        // Initialize theme listeners
        themeEngine.backgroundColorProperty().addListener((obs, oldCol, newCol) -> updateThemeStyles());
        themeEngine.accentColorProperty().addListener((obs, oldCol, newCol) -> updateThemeStyles());
        themeEngine.primaryColorProperty().addListener((obs, oldCol, newCol) -> updateThemeStyles());
        themeEngine.secondaryColorProperty().addListener((obs, oldCol, newCol) -> updateThemeStyles());
        themeEngine.sidebarColorProperty().addListener((obs, oldCol, newCol) -> updateThemeStyles());
        themeEngine.lightModeProperty().addListener((obs, oldVal, newVal) -> updateThemeStyles());

        // --- Drag and Drop ---
        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        if (file.isDirectory()) {
                            LibraryManager.getInstance().addWatchedFolder(file.getAbsolutePath());
                        } else {
                            LibraryManager.getInstance().addWatchedFolder(file.getParentFile().getAbsolutePath());
                        }
                    });
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Auto-hide right panel (queue/lyrics) when clicking elsewhere
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (rightPanel != null && rightPanel.isVisible()) {
                javafx.scene.Node target = (javafx.scene.Node) event.getTarget();
                // Check if target is inside the rightPanel or is a toggle button
                boolean inRightPanel = false;
                javafx.scene.Node p = target;
                while (p != null) {
                    if (p == rightPanel || p == queueBtn || p == lyricsBtn) {
                        inRightPanel = true;
                        break;
                    }
                    p = p.getParent();
                }

                // If it's a button click outside the right panel, close it
                if (!inRightPanel) {
                    boolean isButton = false;
                    p = target;
                    while (p != null) {
                        if (p instanceof javafx.scene.control.Button) {
                            isButton = true;
                            break;
                        }
                        p = p.getParent();
                    }
                    if (isButton) {
                        TranslateTransition transition = new TranslateTransition(Duration.millis(250), rightPanel);
                        transition.setFromX(0);
                        transition.setToX(400);
                        transition.setOnFinished(e -> rightPanel.setVisible(false));
                        transition.play();
                    }
                }
            }
        });

        updateThemeStyles(); // apply initial
    }

    private void setupWindowDragging(Pane titleBar) {
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null && !stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    private void updateBackBtnVisibility() {
        if (backBtn != null) {
            boolean visible = currentActiveView != homeView && currentActiveView != null;
            backBtn.setVisible(visible);
            backBtn.setManaged(visible);
        }
    }

    private void updateActiveSidebarButton(String buttonText) {
        for (Button b : sidebarButtons) {
            if (buttonText != null && b.getText().equalsIgnoreCase(buttonText)) {
                if (!b.getStyleClass().contains("sidebar-item-active")) {
                    b.getStyleClass().add("sidebar-item-active");
                }
            } else {
                b.getStyleClass().remove("sidebar-item-active");
            }
        }
    }

    private void switchViewWithFade(javafx.scene.Node targetView) {
        switchViewWithFade(targetView, true);
    }

    private void switchViewWithFade(javafx.scene.Node targetView, boolean autoCollapseSidebar) {
        if (autoCollapseSidebar) {
            // Auto-collapse sidebar if it's acting as an overlay popup in a narrow window
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null) {
                double windowWidth = stage.getWidth();
                boolean isFullScreen = stage.isFullScreen() || stage.isMaximized();
                boolean shouldDockExpanded = isFullScreen || windowWidth > 1350;
                if (!shouldDockExpanded && sidebarExpanded.get()) {
                    sidebarExpanded.set(false);
                    updateSidebarLayout(isFullScreen, windowWidth);
                }
            }
        }

        if (currentActiveView == targetView)
            return;

        javafx.scene.Node previousView = currentActiveView;
        currentActiveView = targetView;
        updateBackBtnVisibility();

        targetView.setOpacity(0);
        targetView.setVisible(true);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), targetView);
        slideIn.setFromY(15);
        slideIn.setToY(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), previousView);
        slideOut.setFromY(0);
        slideOut.setToY(-15);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(250), targetView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        if (previousView != null) {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(180),
                    previousView);
            fadeOut.setFromValue(previousView.getOpacity());
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                previousView.setVisible(false);
                previousView.setTranslateY(0);
                slideIn.play();
                fadeIn.play();
            });
            slideOut.play();
            fadeOut.play();
        } else {
            slideIn.play();
            fadeIn.play();
        }
    }

    private void showSongListSection(String title) {
        showSongListSection(title, true);
    }

    private void showSongListSection(String title, boolean autoCollapseSidebar) {
        songListView.setVisible(true);
        albumsScrollPane.setVisible(false);
        albumsListView.setVisible(false);
        artistsScrollPane.setVisible(false);
        artistsListView.setVisible(false);
        if (genresScrollPane != null)
            genresScrollPane.setVisible(false);
        if (genresListView != null)
            genresListView.setVisible(false);
        sectionTitle.setText(title);
        switchViewWithFade(centerSection, autoCollapseSidebar);

        // Clear active states on sidebar buttons
        for (Button b : sidebarButtons) {
            b.getStyleClass().remove("sidebar-item-active");
        }
    }

    private void showHomeView() {
        showHomeView(true);
    }

    private void showHomeView(boolean autoCollapseSidebar) {
        updateActiveSidebarButton("Home");
        switchViewWithFade(homeView, autoCollapseSidebar);
    }

    private void updateSidebarLayout(boolean maximized, double windowWidth) {
        Stage stage = (Stage) getScene().getWindow();
        boolean isFullScreen = stage != null && (stage.isFullScreen() || stage.isMaximized());
        boolean shouldDockExpanded = isFullScreen || windowWidth > 1350;

        if (shouldDockExpanded) {
            sidebarSpacer.setPrefWidth(255);
            sidebarSpacer.setMinWidth(255);
            sidebarSpacer.setMaxWidth(255);

            sidebarExpanded.set(true);
            sidebar.setPrefWidth(240);
            sidebar.setMinWidth(240);
            sidebar.setMaxWidth(240);

            if (toggleSidebarBtn != null) {
                toggleSidebarBtn.setVisible(false);
                toggleSidebarBtn.setManaged(false);
            }
        } else {
            boolean expanded = sidebarExpanded.get();
            sidebarSpacer.setPrefWidth(87);
            sidebarSpacer.setMinWidth(87);
            sidebarSpacer.setMaxWidth(87);

            double targetWidth = expanded ? 240 : 72;
            sidebar.setPrefWidth(targetWidth);
            sidebar.setMinWidth(targetWidth);
            sidebar.setMaxWidth(targetWidth);

            if (toggleSidebarBtn != null) {
                toggleSidebarBtn.setVisible(true);
                toggleSidebarBtn.setManaged(true);
            }
        }
    }

    private void showOnlineMusicView() {
        if (!viewModel.onlineMusicEnabledProperty().get()) return;
        updateActiveSidebarButton("Online");
        onlineMusicView.refreshTrending();
        switchViewWithFade(onlineMusicView);
    }

    public void showFavorites() {
        showFavorites(null);
    }

    public void showFavorites(String ignored) {
        updateActiveSidebarButton("Favorites");
        switchViewWithFade(albumDetailView);
        javafx.collections.ObservableList<Song> favs = FXCollections.observableArrayList();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.isFavorite())
                favs.add(s);
        }
        albumDetailView.setPlaylist("Favorites", favs);
    }

    private void showAlbumsGrid() {
        updateActiveSidebarButton("Albums");
        songListView.setVisible(false);
        artistsScrollPane.setVisible(false);
        artistsListView.setVisible(false);
        genresScrollPane.setVisible(false);
        genresListView.setVisible(false);
        sectionTitle.setText("Albums");
        switchViewWithFade(centerSection);

        if (albumsGridViewEnabled.get()) {
            albumsScrollPane.setVisible(true);
            albumsListView.setVisible(false);
            populateAlbumsGrid();
        } else {
            albumsScrollPane.setVisible(false);
            albumsListView.setVisible(true);
            populateAlbumsList();
        }
    }

    private void showArtistsGrid() {
        updateActiveSidebarButton("Artists");
        songListView.setVisible(false);
        albumsScrollPane.setVisible(false);
        albumsListView.setVisible(false);
        genresScrollPane.setVisible(false);
        genresListView.setVisible(false);
        sectionTitle.setText("Artists");
        switchViewWithFade(centerSection);

        if (artistsGridViewEnabled.get()) {
            artistsScrollPane.setVisible(true);
            artistsListView.setVisible(false);
            populateArtistsGrid();
        } else {
            artistsScrollPane.setVisible(false);
            artistsListView.setVisible(true);
            populateArtistsList();
        }
    }

    private void showGenresGrid() {
        updateActiveSidebarButton("Genres");
        songListView.setVisible(false);
        albumsScrollPane.setVisible(false);
        albumsListView.setVisible(false);
        artistsScrollPane.setVisible(false);
        artistsListView.setVisible(false);
        genresScrollPane.setVisible(true);
        genresListView.setVisible(false);
        sectionTitle.setText("Genres");
        switchViewWithFade(centerSection);

        if (genresGridViewEnabled.get()) {
            genresScrollPane.setVisible(true);
            genresListView.setVisible(false);
            populateGenresGrid();
        } else {
            genresScrollPane.setVisible(false);
            genresListView.setVisible(true);
            populateGenresList();
        }
    }

    private void showAlbumDetail(String albumName) {
        System.out.println("showAlbumDetail() called for: " + albumName);
        switchViewWithFade(albumDetailView);

        // Filter songs by album
        javafx.collections.ObservableList<Song> albumSongs = FXCollections.observableArrayList();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.getAlbum().equalsIgnoreCase(albumName)) {
                albumSongs.add(s);
            }
        }
        albumDetailView.setAlbum(albumName, albumSongs);
    }

    public void selectGenre(String genre) {
        switchViewWithFade(albumDetailView);
        javafx.collections.ObservableList<Song> genreSongs = FXCollections.observableArrayList();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.getGenre() != null && s.getGenre().equalsIgnoreCase(genre)) {
                genreSongs.add(s);
            }
        }
        albumDetailView.setPlaylist(genre, genreSongs);
    }

    public void selectArtist(String artist) {
        showArtistDetail(artist);
    }

    private void showArtistDetail(String artistName) {
        switchViewWithFade(albumDetailView);
        javafx.collections.ObservableList<Song> artistSongs = FXCollections.observableArrayList();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.getArtist().equalsIgnoreCase(artistName)) {
                artistSongs.add(s);
            }
        }
        albumDetailView.setArtist(artistName, artistSongs);
    }

    private void populateAlbumsGrid() {
        if (albumCatalog.isEmpty()) {
            albumCatalog = buildBrowseCatalog("albums", Song::getAlbum);
        }
        if (loadedAlbumCards == 0) {
            appendBrowseCards(albumsGridPane, albumCatalog, 0, INITIAL_BROWSE_ITEMS, albumsScrollPane, "album");
            loadedAlbumCards = Math.min(INITIAL_BROWSE_ITEMS, albumCatalog.size());
        }
    }

    private void populateArtistsGrid() {
        if (artistCatalog.isEmpty()) {
            artistCatalog = buildBrowseCatalog("artists", Song::getArtist);
        }
        if (loadedArtistCards == 0) {
            appendBrowseCards(artistsGridPane, artistCatalog, 0, INITIAL_BROWSE_ITEMS, artistsScrollPane, "artist");
            loadedArtistCards = Math.min(INITIAL_BROWSE_ITEMS, artistCatalog.size());
        }
    }

    private void populateGenresGrid() {
        if (genreCatalog.isEmpty()) {
            genreCatalog = buildBrowseCatalog("genres", Song::getGenre);
        }
        if (loadedGenreCards == 0) {
            appendBrowseCards(genresGridPane, genreCatalog, 0, INITIAL_BROWSE_ITEMS, genresScrollPane, "genre");
            loadedGenreCards = Math.min(INITIAL_BROWSE_ITEMS, genreCatalog.size());
        }
    }

    private java.util.List<Song> buildBrowseCatalog(String category,
            java.util.function.Function<Song, String> keyExtractor) {
        java.util.List<Song> cachedCatalog = LibraryManager.getInstance().getBrowseCatalog(category);
        if (!cachedCatalog.isEmpty() || viewModel.getLibrarySongs().isEmpty()) {
            return cachedCatalog;
        }
        // The UI may receive a library update a little before the manager's cache.
        // Keep the fallback local and deterministic in that short transition.
        java.util.Map<String, Song> representatives = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Song song : viewModel.getLibrarySongs()) {
            String key = keyExtractor.apply(song);
            if (key != null && !key.isBlank()) {
                representatives.putIfAbsent(key, song);
            }
        }
        return new java.util.ArrayList<>(representatives.values());
    }

    private void appendBrowseCards(GridPane gridPane, java.util.List<Song> songs, int startIndex, int count,
            ScrollPane scrollPane, String type) {
        int endIndex = Math.min(startIndex + count, songs.size());
        for (int i = startIndex; i < endIndex; i++) {
            Song song = songs.get(i);
            if (type.equals("album")) {
                String album = song.getAlbum();
                Pane card = createGridItemCard(album, song.getArtist(), song.getPath(), () -> {
                    showAlbumDetail(album);
                });
                gridPane.add(card, 0, 0);
            } else if (type.equals("artist")) {
                String artist = song.getArtist();
                Pane card = createGridItemCard(artist, "", song.getPath(), () -> {
                    showArtistDetail(artist);
                });
                gridPane.add(card, 0, 0);
            } else if (type.equals("genre")) {
                String genre = song.getGenre();
                Pane card = createGridItemCard(genre, "", song.getPath(), () -> {
                    selectGenre(genre);
                });
                gridPane.add(card, 0, 0);
            }
        }

        layoutGridResponsively(gridPane, scrollPane.getWidth());
    }

    private void invalidateBrowseCatalogs() {
        albumCatalog = java.util.List.of();
        artistCatalog = java.util.List.of();
        genreCatalog = java.util.List.of();
        loadedAlbumCards = loadedArtistCards = loadedGenreCards = 0;
        albumsGridPane.getChildren().clear();
        artistsGridPane.getChildren().clear();
        genresGridPane.getChildren().clear();
        albumsListView.getItems().clear();
        artistsListView.getItems().clear();
        genresListView.getItems().clear();
    }

    private void prefetchAlbumCards(double scrollPosition) {
        if (scrollPosition < BROWSE_PREFETCH_THRESHOLD || isLoadingAlbumCards
                || loadedAlbumCards >= albumCatalog.size()) {
            return;
        }
        isLoadingAlbumCards = true;
        Platform.runLater(() -> {
            appendBrowseCards(albumsGridPane, albumCatalog, loadedAlbumCards, BROWSE_PREFETCH_ITEMS,
                    albumsScrollPane, "album");
            loadedAlbumCards = Math.min(loadedAlbumCards + BROWSE_PREFETCH_ITEMS, albumCatalog.size());
            isLoadingAlbumCards = false;
        });
    }

    private void prefetchArtistCards(double scrollPosition) {
        if (scrollPosition < BROWSE_PREFETCH_THRESHOLD || isLoadingArtistCards
                || loadedArtistCards >= artistCatalog.size()) {
            return;
        }
        isLoadingArtistCards = true;
        Platform.runLater(() -> {
            appendBrowseCards(artistsGridPane, artistCatalog, loadedArtistCards, BROWSE_PREFETCH_ITEMS,
                    artistsScrollPane, "artist");
            loadedArtistCards = Math.min(loadedArtistCards + BROWSE_PREFETCH_ITEMS, artistCatalog.size());
            isLoadingArtistCards = false;
        });
    }

    private void prefetchGenreCards(double scrollPosition) {
        if (scrollPosition < BROWSE_PREFETCH_THRESHOLD || isLoadingGenreCards
                || loadedGenreCards >= genreCatalog.size()) {
            return;
        }
        isLoadingGenreCards = true;
        Platform.runLater(() -> {
            appendBrowseCards(genresGridPane, genreCatalog, loadedGenreCards, BROWSE_PREFETCH_ITEMS,
                    genresScrollPane, "genre");
            loadedGenreCards = Math.min(loadedGenreCards + BROWSE_PREFETCH_ITEMS, genreCatalog.size());
            isLoadingGenreCards = false;
        });
    }

    private void layoutGridResponsively(GridPane gridPane, double containerWidth) {
        if (gridPane == null || gridPane.getChildren().isEmpty())
            return;

        double hgap = gridPane.getHgap();

        // Keep a deliberate, poster-like layout instead of filling the page with
        // many tiny cards. A regular window shows four columns; a maximised or
        // fullscreen window has room for five noticeably larger columns.
        Stage stage = getScene() == null ? null : (Stage) getScene().getWindow();
        boolean largeWindow = stage != null && (stage.isFullScreen() || stage.isMaximized());
        int preferredColumns = largeWindow ? 5 : 4;
        double minimumCardWidth = largeWindow ? 220 : 160;

        // Account for the scrollbar and the grid's visual breathing room.
        double availableWidth = containerWidth - 60;
        if (availableWidth < 200)
            availableWidth = 200;

        int columnsThatFit = Math.max(1, (int) ((availableWidth + hgap) / (minimumCardWidth + hgap)));
        int cols = Math.min(preferredColumns, columnsThatFit);

        double cardWidth = (availableWidth - (cols - 1) * hgap) / cols;

        int index = 0;
        for (javafx.scene.Node node : gridPane.getChildren()) {
            if (node instanceof Region) {
                Region card = (Region) node;
                card.setPrefWidth(cardWidth);
                card.setMinWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
            int col = index % cols;
            int row = index / cols;
            GridPane.setColumnIndex(node, col);
            GridPane.setRowIndex(node, row);
            index++;
        }
    }

    private Pane createGridItemCard(String title, String subtitle, String songPath, Runnable onClick) {
        VBox card = new VBox(8);
        card.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");

        StackPane artContainer = new StackPane();
        artContainer.prefWidthProperty().bind(card.widthProperty());
        artContainer.prefHeightProperty().bind(card.widthProperty());
        artContainer.minWidthProperty().bind(card.widthProperty());
        artContainer.minHeightProperty().bind(card.widthProperty());
        artContainer.maxWidthProperty().bind(card.widthProperty());
        artContainer.maxHeightProperty().bind(card.widthProperty());
        artContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(12), Insets.EMPTY)));

        ImageView artView = new ImageView();
        artView.fitWidthProperty().bind(card.widthProperty());
        artView.fitHeightProperty().bind(card.widthProperty());
        artView.setPreserveRatio(true);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.widthProperty());
        artView.setClip(clip);

        Label placeholder = new Label("💿");
        placeholder.setStyle("-fx-font-size: 64px; -fx-text-fill: rgba(255,255,255,0.15);");
        artContainer.getChildren().addAll(placeholder, artView);

        if (songPath != null) {
            Image cachedImg = aura.music.utils.ImageCache.getCachedImage(songPath);
            if (cachedImg != null) {
                artView.setImage(cachedImg);
                placeholder.setVisible(false);
            } else {
                java.util.concurrent.CompletableFuture
                        .supplyAsync(() -> aura.music.library.MetadataExtractor.extractArtworkBytes(songPath))
                        .thenAcceptAsync(artBytes -> {
                            if (artBytes != null) {
                                Image img = aura.music.utils.ImageCache.getImage(songPath, artBytes, 250, 250);
                                if (img != null) {
                                    artView.setImage(img);
                                    placeholder.setVisible(false);
                                }
                            }
                        }, javafx.application.Platform::runLater);
            }
        }

        DropShadow shadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.3));
        artContainer.setEffect(shadow);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(artContainer, titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            Label artistLabel = new Label(subtitle);
            artistLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 12px;");
            artistLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            artistLabel.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(artistLabel);
        }

        // Smooth hover animation using ScaleTransition
        javafx.animation.ScaleTransition hoverScale = new javafx.animation.ScaleTransition(Duration.millis(180),
                artContainer);
        card.setOnMouseEntered(e -> {
            hoverScale.setToX(1.04);
            hoverScale.setToY(1.04);
            hoverScale.playFromStart();
        });
        card.setOnMouseExited(e -> {
            hoverScale.setToX(1.0);
            hoverScale.setToY(1.0);
            hoverScale.playFromStart();
        });

        card.setOnMouseClicked(e -> onClick.run());

        return card;
    }

    private VBox createSidebar() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        double initialWidth = sidebarExpanded.get() ? 240 : 72;
        sidebar.setPrefWidth(initialWidth);
        sidebar.setMinWidth(initialWidth); // Prevent squeezing
        sidebar.setMaxWidth(Region.USE_PREF_SIZE); // Prevent StackPane stretching!
        if (sidebarExpanded.get()) {
            sidebar.setPadding(new Insets(20, 15, 20, 15));
        } else {
            sidebar.setPadding(new Insets(20, 8, 20, 8));
        }
        sidebar.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.45); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 1;");

        // Header with Logo, Navigation and Toggle Button
        HBox logoContainer = new HBox(10);
        logoContainer.setAlignment(Pos.CENTER_LEFT);
        logoContainer.setPadding(new Insets(10, 0, 15, 5));

        toggleSidebarBtn = new Button();
        toggleSidebarBtn.setGraphic(SVGIcons.createMenuIcon(16, Color.WHITE));
        toggleSidebarBtn.getStyleClass().add("icon-button");
        toggleSidebarBtn.setOnAction(e -> toggleSidebar());

        backBtn = new Button("←");
        backBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 5 0 0;");
        backBtn.setOnAction(e -> showHomeView());

        logo = new Label("Aura Music");
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");

        // Set initial back button visibility
        updateBackBtnVisibility();
        logo.visibleProperty().bind(sidebarExpanded);
        logo.managedProperty().bind(sidebarExpanded);

        logoContainer.getChildren().addAll(toggleSidebarBtn, logo); // , optionsBtn
        sidebar.getChildren().add(logoContainer);

        // Search Box
        StackPane searchContainer = new StackPane();
        searchContainer.setPadding(new Insets(0, 5, 15, 5));
        searchField = new TextField();
        searchField.setPromptText("Search");
        searchField.setPrefHeight(38);
        searchField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 0 10 0 35; -fx-prompt-text-fill: rgba(255,255,255,0.4); -fx-font-size: 13px;");

        searchIcon = SVGIcons.createSearchIcon(14, Color.web("rgba(255,255,255,0.4)"));
        searchIcon.setTranslateX(12);

        searchContainer.getChildren().addAll(searchField, searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        sidebar.getChildren().add(searchContainer);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.performSearch(newVal);
            if (!newVal.trim().isEmpty()) {
                showSongListSection("Search Results", false);
                songListView.setItems(viewModel.getSearchResults());
            } else {
                showHomeView(false);
            }
        });

        // Groups and Buttons
        navHeader = new Label("Discover");
        navHeader.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 10 0 5 10;");
        sidebar.getChildren().add(navHeader);

        homeBtn = createSidebarButton("Home", SVGIcons.createHomeIcon(16, Color.WHITE));
        homeBtn.getStyleClass().add("sidebar-item-active");
        onlineBtn = createSidebarButton("Online", SVGIcons.createRadioIcon(16, Color.WHITE));
        onlineBtn.visibleProperty().bind(viewModel.onlineMusicEnabledProperty());
        onlineBtn.managedProperty().bind(viewModel.onlineMusicEnabledProperty());

        sidebar.getChildren().addAll(homeBtn, onlineBtn);

        libraryHeader = new Label("Library");
        libraryHeader.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 15 0 5 10;");
        sidebar.getChildren().add(libraryHeader);

        albumsBtn = createSidebarButton("Albums", SVGIcons.createAlbumIcon(14, Color.WHITE));
        artistsBtn = createSidebarButton("Artists", SVGIcons.createArtistIcon(14, Color.WHITE));
        genresBtn = createSidebarButton("Genres", SVGIcons.createGenreIcon(14, Color.WHITE));
        songsBtn = createSidebarButton("Songs", SVGIcons.createMusicIcon(14, Color.WHITE));
        favoritesBtn = createSidebarButton("Favorites", SVGIcons.createHeartIcon(14, Color.WHITE, true));
        playlistsBtn = createSidebarButton("Playlists", SVGIcons.createPlaylistIcon(14, Color.WHITE));

        sidebar.getChildren().addAll(albumsBtn, artistsBtn, genresBtn, songsBtn, favoritesBtn, playlistsBtn);

        sidebarButtons.addAll(java.util.Arrays.asList(homeBtn, onlineBtn, albumsBtn, artistsBtn, genresBtn, songsBtn,
                favoritesBtn, playlistsBtn));

        // Click actions
        for (Button btn : sidebarButtons) {
            btn.setOnAction(e -> {
                for (Button b : sidebarButtons) {
                    b.getStyleClass().remove("sidebar-item-active");
                }
                btn.getStyleClass().add("sidebar-item-active");

                if (btn == homeBtn) {
                    showHomeView();
                } else if (btn == onlineBtn) {
                    showOnlineMusicView();
                } else if (btn == albumsBtn) {
                    showAlbumsGrid();
                } else if (btn == artistsBtn) {
                    showArtistsGrid();
                } else if (btn == genresBtn) {
                    showGenresGrid();
                } else if (btn == favoritesBtn) {
                    showFavorites();
                } else if (btn == songsBtn) {
                    showSongListSection("Songs");
                    songsBtn.getStyleClass().add("sidebar-item-active");
                } else if (btn == playlistsBtn) {
                    showPlaylistView();
                }
            });
        }

        // Bind visibility of secondary items to sidebarExpanded to keep collapsed view
        // Add Folder Button
        addFolderBtn = new Button("+ Add Folder");
        addFolderBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;");
        addFolderBtn.setMaxWidth(Double.MAX_VALUE);
        addFolderBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Music Folder");
            File selected = chooser.showDialog(getScene().getWindow());
            if (selected != null) {
                if (viewModel.getLibrarySongs().isEmpty()) {
                    showInitialImport();
                }
                LibraryManager.getInstance().addWatchedFolder(selected.getAbsolutePath());
                viewModel.getLibrarySongs().setAll(LibraryManager.getInstance().getSongs());
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom Stats Section
        VBox bottomStats = new VBox(8);
        bottomStats.setPadding(new Insets(15, 10, 0, 10));
        bottomStats.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1px 0 0 0;");

        HBox profileRow = new HBox(8);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        Circle avatar = new Circle(12);
        // Bind avatar color to username hash for a unique hue
        avatar.fillProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() -> {
            String name = LibraryManager.getInstance().getUsername();
            if (name == null || name.isEmpty())
                return Color.web("#ff2d55");
            int hash = name.hashCode();
            double hue = (hash % 360 + 360) % 360; // Ensure positive hue
            return Color.hsb(hue, 0.6, 0.8);
        }, LibraryManager.getInstance().usernameProperty()));
        avatarText = new Label();
        // Bind avatar text to initials of username
        avatarText.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String name = LibraryManager.getInstance().getUsername();
            if (name == null || name.isEmpty())
                return "";
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty())
                    sb.append(Character.toUpperCase(part.charAt(0)));
            }
            return sb.toString();
        }, LibraryManager.getInstance().usernameProperty()));
        avatarText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        StackPane avatarStack = new StackPane(avatar, avatarText);
        profileName = new Label();
        // Add avatar and profile name to the bottom profile row
        profileRow.getChildren().addAll(avatarStack, profileName);
        profileName.textProperty().bind(LibraryManager.getInstance().usernameProperty());
        profileName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region profileSpacer = new Region();
        HBox.setHgrow(profileSpacer, Priority.ALWAYS);

        settingsBtn = new Button();
        settingsBtn.setGraphic(SVGIcons.createSettingsIcon(14, Color.WHITE));
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        settingsBtn.setOnMouseEntered(e -> settingsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 4;"));
        settingsBtn.setOnMouseExited(
                e -> settingsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;"));
        settingsBtn.setOnAction(e -> showSettingsView());
        // Add settings button to the profile row
        profileRow.getChildren().add(settingsBtn);
        settingsBtn.visibleProperty().bind(sidebarExpanded);
        settingsBtn.managedProperty().bind(sidebarExpanded);

        statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 10px;");

        Runnable updateStats = () -> {
            int totalSongs = viewModel.getLibrarySongs().size();
            double totalSecs = viewModel.getLibrarySongs().stream().mapToDouble(Song::getDuration).sum();
            int hours = (int) (totalSecs / 3600);
            int mins = (int) ((totalSecs % 3600) / 60);
            statsLabel.setText(String.format("%d songs • %dh %dm", totalSongs, hours, mins));
        };
        updateStats.run();
        viewModel.getLibrarySongs()
                .addListener((javafx.collections.ListChangeListener<Song>) c -> Platform.runLater(updateStats));

        bottomStats.getChildren().addAll(profileRow, statsLabel);

        sidebar.getChildren().addAll(spacer, addFolderBtn, bottomStats);

        // Bind visibility and managed properties to sidebarExpanded
        logo.visibleProperty().bind(sidebarExpanded);
        logo.managedProperty().bind(sidebarExpanded);
        searchContainer.visibleProperty().bind(sidebarExpanded);
        searchContainer.managedProperty().bind(sidebarExpanded);
        addFolderBtn.visibleProperty().bind(sidebarExpanded);
        addFolderBtn.managedProperty().bind(sidebarExpanded);
        profileName.visibleProperty().bind(sidebarExpanded);
        profileName.managedProperty().bind(sidebarExpanded);
        statsLabel.visibleProperty().bind(sidebarExpanded);
        statsLabel.managedProperty().bind(sidebarExpanded);
        navHeader.visibleProperty().bind(sidebarExpanded);
        navHeader.managedProperty().bind(sidebarExpanded);
        libraryHeader.visibleProperty().bind(sidebarExpanded);
        libraryHeader.managedProperty().bind(sidebarExpanded);

        // Setup Collapsing bindings
        sidebarExpanded.addListener((obs, oldVal, newVal) -> {
            for (Button btn : sidebarButtons) {
                if (newVal) {
                    btn.setContentDisplay(ContentDisplay.LEFT);
                    btn.setAlignment(Pos.CENTER_LEFT);
                } else {
                    btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    btn.setAlignment(Pos.CENTER);
                }
            }
            updateBackBtnVisibility();

            if (newVal) {
                sidebar.setPadding(new Insets(20, 15, 20, 15));
            } else {
                sidebar.setPadding(new Insets(20, 8, 20, 8));
            }
        });

        return sidebar;
    }

    private void createInitialImportOverlay() {
        initialImportStatus = new Label();
        initialImportStatus.setStyle("-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 14px;");

        Label title = new Label("Building your music library");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label detail = new Label("First-time setup usually takes 10–20 seconds. You can start exploring as soon as it finishes.");
        detail.setWrapText(true);
        detail.setMaxWidth(390);
        detail.setStyle("-fx-text-fill: rgba(255,255,255,0.58); -fx-font-size: 13px;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(42, 42);
        VBox card = new VBox(14, progress, title, detail, initialImportStatus);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: rgba(26,26,30,0.96); -fx-background-radius: 18; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 18;");

        initialImportOverlay = new StackPane(card);
        initialImportOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.52);");
        initialImportOverlay.setVisible(false);
        initialImportOverlay.setManaged(false);
        getChildren().add(initialImportOverlay);
    }

    private void showInitialImport() {
        showingInitialImport = true;
        initialImportStatus.setText("Preparing your songs…");
        initialImportOverlay.setVisible(true);
        initialImportOverlay.setManaged(true);
    }

    private void updateInitialImportStatus(int songsFound) {
        if (showingInitialImport) {
            initialImportStatus.setText(String.format("Found %,d songs…", songsFound));
        }
    }

    private void finishInitialImport(int songsFound) {
        if (!showingInitialImport) {
            return;
        }
        initialImportStatus.setText(String.format("%,d songs are ready", songsFound));
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.millis(900));
        delay.setOnFinished(e -> {
            showingInitialImport = false;
            initialImportOverlay.setVisible(false);
            initialImportOverlay.setManaged(false);
        });
        delay.play();
    }

    private void toggleSidebar() {
        Stage stage = (Stage) getScene().getWindow();
        boolean isFullScreen = stage != null && (stage.isFullScreen() || stage.isMaximized());
        boolean shouldDockExpanded = isFullScreen || getWidth() > 1350;

        if (shouldDockExpanded) {
            // Keep expanded always in full screen
            return;
        }

        boolean expanded = sidebarExpanded.get();
        sidebarExpanded.set(!expanded);
        double targetWidth = !expanded ? 240 : 72;

        // Effect is now handled permanently via CSS in createSidebar

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(200),
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), targetWidth),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), targetWidth),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), targetWidth)));
        timeline.play();
    }

    private void showPlaylistView() {
        updateActiveSidebarButton("Playlists");
        switchViewWithFade(playlistView);
    }

    private Button createSidebarButton(String text, javafx.scene.Node icon) {
        Button btn = new Button(text);
        btn.setGraphic(icon);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        if (sidebarExpanded.get()) {
            btn.setContentDisplay(ContentDisplay.LEFT);
            btn.setAlignment(Pos.CENTER_LEFT);
        } else {
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btn.setAlignment(Pos.CENTER);
        }
        btn.setStyle(
                "-fx-text-fill: -fx-secondary-text; -fx-font-size: 14px; -fx-graphic-text-gap: 12; -fx-background-color: transparent; -fx-padding: 8 12; -fx-background-radius: 6;");
        return btn;
    }

    private VBox createCenterSection() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(30, 40, 20, 40));
        center.setStyle("-fx-background-color: transparent;");

        // Header (Title & Search/Filter)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));

        sectionTitle = new Label("All Songs");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        Button headerSearchBtn = new Button();
        headerSearchBtn.setGraphic(SVGIcons.createSearchIcon(14, Color.WHITE));
        headerSearchBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        Button headerFilterBtn = new Button();
        headerFilterBtn.setGraphic(SVGIcons.createMenuIcon(14, Color.WHITE));
        headerFilterBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        HBox rightButtons = new HBox(15, headerSearchBtn, headerFilterBtn);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(leftSpacer, sectionTitle, rightSpacer, rightButtons);

        // Song List View
        songListView = new ListView<>(viewModel.getLibrarySongs());
        VBox.setVgrow(songListView, Priority.ALWAYS);
        songListView.setCellFactory(lv -> new SongListCell());

        songListView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Song selected = songListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    viewModel.play(selected);
                }
            }
        });

        // Albums Grid View
        albumsGridPane = new GridPane();
        albumsGridPane.setHgap(24);
        albumsGridPane.setVgap(28);
        albumsGridPane.setStyle("-fx-background-color: transparent;");

        albumsScrollPane = new ScrollPane(albumsGridPane);
        albumsScrollPane.setFitToWidth(true);
        albumsScrollPane.setPannable(true);
        albumsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        albumsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        albumsScrollPane.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(albumsScrollPane, Priority.ALWAYS);
        albumsScrollPane.setVisible(false);

        // Artists Grid View
        artistsGridPane = new GridPane();
        artistsGridPane.setHgap(24);
        artistsGridPane.setVgap(28);
        artistsGridPane.setStyle("-fx-background-color: transparent;");

        artistsScrollPane = new ScrollPane(artistsGridPane);
        artistsScrollPane.setFitToWidth(true);
        artistsScrollPane.setPannable(true);
        artistsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        artistsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        artistsScrollPane.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(artistsScrollPane, Priority.ALWAYS);
        artistsScrollPane.setVisible(false);

        // Genres Grid View
        genresGridPane = new GridPane();
        genresGridPane.setHgap(24);
        genresGridPane.setVgap(28);
        genresGridPane.setStyle("-fx-background-color: transparent;");

        genresScrollPane = new ScrollPane(genresGridPane);
        genresScrollPane.setFitToWidth(true);
        genresScrollPane.setPannable(true);
        genresScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        genresScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        genresScrollPane.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(genresScrollPane, Priority.ALWAYS);
        genresScrollPane.setVisible(false);

        // Albums List View
        albumsListView = new ListView<>();
        VBox.setVgrow(albumsListView, Priority.ALWAYS);
        albumsListView.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        albumsListView.setCellFactory(lv -> new AlbumListCell());
        albumsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = albumsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showAlbumDetail(selected);
                }
            }
        });
        albumsListView.setVisible(false);

        // Artists List View
        artistsListView = new ListView<>();
        VBox.setVgrow(artistsListView, Priority.ALWAYS);
        artistsListView.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        artistsListView.setCellFactory(lv -> new AlbumListCell());
        artistsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = artistsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showArtistDetail(selected);
                }
            }
        });
        artistsListView.setVisible(false);

        // Genres List View
        genresListView = new ListView<>();
        VBox.setVgrow(genresListView, Priority.ALWAYS);
        genresListView.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        genresListView.setCellFactory(lv -> new AlbumListCell());
        genresListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = genresListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectGenre(selected);
                }
            }
        });
        genresListView.setVisible(false);

        // Manage visibility properties so hidden elements don't take up layout space
        songListView.managedProperty().bind(songListView.visibleProperty());
        albumsScrollPane.managedProperty().bind(albumsScrollPane.visibleProperty());
        albumsListView.managedProperty().bind(albumsListView.visibleProperty());
        artistsScrollPane.managedProperty().bind(artistsScrollPane.visibleProperty());
        artistsListView.managedProperty().bind(artistsListView.visibleProperty());
        genresScrollPane.managedProperty().bind(genresScrollPane.visibleProperty());
        genresListView.managedProperty().bind(genresListView.visibleProperty());

        albumsScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            layoutGridResponsively(albumsGridPane, newVal.doubleValue());
        });
        artistsScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            layoutGridResponsively(artistsGridPane, newVal.doubleValue());
        });
        genresScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            layoutGridResponsively(genresGridPane, newVal.doubleValue());
        });
        albumsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> prefetchAlbumCards(newVal.doubleValue()));
        artistsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> prefetchArtistCards(newVal.doubleValue()));
        genresScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> prefetchGenreCards(newVal.doubleValue()));

        center.getChildren().addAll(header, songListView, albumsScrollPane, albumsListView, artistsScrollPane,
                artistsListView, genresScrollPane, genresListView);
        return center;
    }

    private ListView<Song> queueListView;
    private ListView<Song> historyListView;
    private LyricsView lyricsView;
    private VBox queueContainer;

    private VBox createRightPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("glass-panel");
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.88); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 0 1;");

        // Lyrics View initialization
        lyricsView = new LyricsView(viewModel);
        VBox.setVgrow(lyricsView, Priority.ALWAYS);

        // Queue Container initialization
        queueContainer = new VBox(15);
        VBox.setVgrow(queueContainer, Priority.ALWAYS);

        // 1. Tab Selector (Pill Container)
        HBox tabContainer = new HBox(4);
        tabContainer.setPadding(new Insets(2));
        tabContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 20;");
        tabContainer.setAlignment(Pos.CENTER);

        playingNextTabBtn = new Button("Playing Next");
        playingNextTabBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playingNextTabBtn, Priority.ALWAYS);
        playingNextTabBtn.setStyle(
                "-fx-background-color: #121212; -fx-text-fill: white; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

        historyTabBtn = new Button("History");
        historyTabBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(historyTabBtn, Priority.ALWAYS);
        historyTabBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

        tabContainer.getChildren().addAll(playingNextTabBtn, historyTabBtn);

        // 2. Queue Header Row
        queueHeaderRow = new HBox(10);
        queueHeaderRow.setAlignment(Pos.CENTER_LEFT);
        queueHeaderTitle = new Label("Playing Next");
        queueHeaderTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        queueClearBtn = new Button("Clear");
        queueClearBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: 500;");

        queueAutoplayBtn = new Button("∞");
        queueAutoplayBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 18px; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0;");

        // Infinite toggle: bind to toggle repeat mode between ALL and OFF
        queueAutoplayBtn.setOnAction(e -> {
            if (viewModel.repeatModeProperty().get() == MainViewModel.RepeatMode.ALL) {
                viewModel.repeatModeProperty().set(MainViewModel.RepeatMode.OFF);
                queueAutoplayBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 18px; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0;");
            } else {
                viewModel.repeatModeProperty().set(MainViewModel.RepeatMode.ALL);
                queueAutoplayBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: -fx-accent; -fx-font-size: 18px; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0;");
            }
        });

        queueHeaderRow.getChildren().addAll(queueHeaderTitle, headerSpacer, queueClearBtn, queueAutoplayBtn);

        // 3. Queue List View
        queueListView = new ListView<>(filteredQueue);
        setupPremiumQueueListView(queueListView);

        // Queue listener and update handled in ViewModel

        // 4. History List View
        historyListView = new ListView<>(viewModel.getRecentlyPlayed());
        setupPremiumQueueListView(historyListView);
        historyListView.setVisible(false);

        // Tab selection actions
        playingNextTabBtn.setOnAction(e -> {
            playingNextTabBtn.setStyle(
                    "-fx-background-color: #121212; -fx-text-fill: white; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
            historyTabBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

            queueHeaderTitle.setText("Playing Next");
            queueAutoplayBtn.setVisible(true);

            queueListView.setVisible(true);
            historyListView.setVisible(false);
        });

        historyTabBtn.setOnAction(e -> {
            historyTabBtn.setStyle(
                    "-fx-background-color: #121212; -fx-text-fill: white; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
            playingNextTabBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-background-radius: 18; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

            queueHeaderTitle.setText("History");
            queueAutoplayBtn.setVisible(false);

            queueListView.setVisible(false);
            historyListView.setVisible(true);
        });

        // Wire clear button
        queueClearBtn.setOnAction(e -> {
            if (queueListView.isVisible()) {
                viewModel.getQueue().clear();
                viewModel.currentQueueIndexProperty().set(-1);
            } else {
                viewModel.getRecentlyPlayed().clear();
            }
        });

        queueContainer.getChildren().addAll(tabContainer, queueHeaderRow, queueListView, historyListView);

        // Add lyricsView initially
        panel.getChildren().add(lyricsView);
        return panel;
    }

    private Button playingNextTabBtn;
    private Button historyTabBtn;
    private HBox queueHeaderRow;
    private Label queueHeaderTitle;
    private Button queueClearBtn;
    private Button queueAutoplayBtn;

    private void setupPremiumQueueListView(ListView<Song> listView) {
        listView.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.managedProperty().bind(listView.visibleProperty());
        listView.setCellFactory(lv -> new ListCell<Song>() {
            private final HBox cellLayout = new HBox(10);
            private final ImageView artView = new ImageView();
            private final VBox textContainer = new VBox(2);
            private final Label songTitle = new Label();
            private final Label songDetails = new Label();
            private final Label songDuration = new Label();

            {
                cellLayout.setAlignment(Pos.CENTER_LEFT);
                cellLayout.setPadding(new Insets(6, 0, 6, 0));

                artView.setFitWidth(40);
                artView.setFitHeight(40);
                artView.setPreserveRatio(true);
                Rectangle clip = new Rectangle(40, 40);
                clip.setArcWidth(8);
                clip.setArcHeight(8);
                artView.setClip(clip);

                songTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-primary-text; -fx-font-size: 13px;");
                songTitle.setTextOverrun(OverrunStyle.ELLIPSIS);
                songTitle.setMaxWidth(210);

                songDetails.setStyle("-fx-text-fill: -fx-secondary-text; -fx-font-size: 11px;");
                songDetails.setTextOverrun(OverrunStyle.ELLIPSIS);
                songDetails.setMaxWidth(210);

                textContainer.getChildren().addAll(songTitle, songDetails);
                textContainer.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textContainer, Priority.ALWAYS);

                songDuration.setStyle("-fx-text-fill: -fx-secondary-text; -fx-font-size: 12px;");

                cellLayout.getChildren().addAll(artView, textContainer, songDuration);
            }

            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    int minutes = (int) (item.getDuration() / 60);
                    int secs = (int) (item.getDuration() % 60);
                    songDuration.setText(String.format("%d:%02d", minutes, secs));

                    Image cachedImage = queueArtCache.get(item.getPath());
                    if (cachedImage == null) {
                        byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(item.getPath());
                        if (artBytes != null) {
                            cachedImage = new Image(new java.io.ByteArrayInputStream(artBytes), 40, 40, true, true);
                            queueArtCache.put(item.getPath(), cachedImage);
                        }
                    }
                    artView.setImage(cachedImage);

                    textContainer.getChildren().clear();

                    // Highlight currently playing song in the queue with scrolling marquee
                    if (viewModel.currentSongProperty().get() == item) {
                        javafx.scene.Node titleMarquee = aura.music.ui.MarqueeUtils.createMarqueeLabel(item.getTitle(),
                                "-fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-font-size: 13px;", 210);
                        javafx.scene.Node detailsMarquee = aura.music.ui.MarqueeUtils.createMarqueeLabel(
                                item.getArtist() + " — " + item.getAlbum(),
                                "-fx-text-fill: -fx-accent; -fx-opacity: 0.8; -fx-font-size: 11px;", 210);
                        textContainer.getChildren().addAll(titleMarquee, detailsMarquee);
                    } else {
                        songTitle.setText(item.getTitle());
                        songDetails.setText(item.getArtist() + " — " + item.getAlbum());
                        textContainer.getChildren().addAll(songTitle, songDetails);
                    }

                    setGraphic(cellLayout);
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    viewModel.play(selected);
                }
            }
        });
    }

    private HBox createPlayerBar() {
        HBox bar = new HBox(25);
        bar.getStyleClass().add("player-bar");
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(Pos.CENTER);
        bar.setPrefHeight(88);
        bar.setMinHeight(88);

        // --- CENTER SECTION: Controls + LCD Capsule ---
        // 1. Playback Controls Row
        shuffleBtn = new Button();
        shuffleBtn.setGraphic(SVGIcons.createShuffleIcon(14, Color.web("rgba(255,255,255,0.6)")));
        shuffleBtn.getStyleClass().add("icon-button");

        prevBtn = new Button();
        prevBtn.setGraphic(SVGIcons.createPreviousIcon(14, Color.WHITE));
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setOnAction(e -> viewModel.previous());

        miniPlayPause = new Button();
        miniPlayPause.setGraphic(SVGIcons.createPlayIcon(16, Color.WHITE));
        miniPlayPause.getStyleClass().add("icon-button");
        miniPlayPause.setOnAction(e -> viewModel.togglePlayPause());

        nextBtn = new Button();
        nextBtn.setGraphic(SVGIcons.createNextIcon(14, Color.WHITE));
        nextBtn.getStyleClass().add("icon-button");
        nextBtn.setOnAction(e -> viewModel.next());

        repeatBtn = new Button();
        repeatBtn.setGraphic(SVGIcons.createRepeatIcon(14, Color.web("rgba(255,255,255,0.6)")));
        repeatBtn.getStyleClass().add("icon-button");

        shuffleBtn.setOnAction(e -> {
            boolean active = !viewModel.shuffleModeProperty().get();
            viewModel.shuffleModeProperty().set(active);
        });

        repeatBtn.setOnAction(e -> {
            MainViewModel.RepeatMode current = viewModel.repeatModeProperty().get();
            MainViewModel.RepeatMode nextMode;
            if (current == MainViewModel.RepeatMode.OFF) {
                nextMode = MainViewModel.RepeatMode.ALL;
            } else if (current == MainViewModel.RepeatMode.ALL) {
                nextMode = MainViewModel.RepeatMode.ONE;
            } else {
                nextMode = MainViewModel.RepeatMode.OFF;
            }
            viewModel.repeatModeProperty().set(nextMode);
        });

        viewModel.shuffleModeProperty()
                .addListener((obs, oldVal, newVal) -> Platform.runLater(this::updateThemeStyles));
        viewModel.repeatModeProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::updateThemeStyles));
        viewModel.isPlayingProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::updateThemeStyles));

        HBox controlsRow = new HBox(12, shuffleBtn, prevBtn, miniPlayPause, nextBtn, repeatBtn);
        controlsRow.setAlignment(Pos.CENTER);

        // 2. LCD Capsule
        VBox capsuleContainer = new VBox(0);
        capsuleContainer.setPrefWidth(440);
        capsuleContainer.setMinWidth(440);
        capsuleContainer.setMaxWidth(440);
        capsuleContainer.setPrefHeight(46);
        capsuleContainer.setMinHeight(46);
        capsuleContainer.setMaxHeight(46);
        capsuleContainer.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 6; -fx-border-width: 1;");
        capsuleContainer.setAlignment(Pos.CENTER);

        HBox topHalf = new HBox(12);
        topHalf.setAlignment(Pos.CENTER_LEFT);
        topHalf.setPadding(new Insets(6, 12, 4, 12));
        VBox.setVgrow(topHalf, Priority.ALWAYS);

        miniArtwork = new ImageView();
        miniArtwork.setFitWidth(40);
        miniArtwork.setFitHeight(40);
        miniArtwork.setPreserveRatio(true);
        Rectangle clip = new Rectangle(40, 40);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        miniArtwork.setClip(clip);
        miniArtwork.setCursor(javafx.scene.Cursor.HAND);
        miniArtwork.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                enterMiniPlayer();
            }
        });

        Label miniArtPlaceholder = new Label("🎵");
        miniArtPlaceholder.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.3);");
        StackPane miniArtContainer = new StackPane(miniArtPlaceholder, miniArtwork);
        miniArtContainer.setPrefSize(40, 40);
        miniArtContainer.setMinSize(40, 40);
        miniArtContainer.setMaxSize(40, 40);
        miniArtContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#1e1e1e"), new CornerRadii(6), Insets.EMPTY)));

        miniTitleContainer = new javafx.scene.layout.StackPane();
        miniTitleContainer.setAlignment(Pos.CENTER_LEFT);
        miniArtistContainer = new javafx.scene.layout.StackPane();
        miniArtistContainer.setAlignment(Pos.CENTER_LEFT);

        Label defaultTitle = new Label("");
        defaultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.7);");
        miniTitleContainer.getChildren().add(defaultTitle);

        // Wave icon button
        Button waveBtn = new Button();
        waveBtn.setGraphic(SVGIcons.createWaveIcon(12, Color.web("rgba(255,255,255,0.6)")));
        waveBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");

        // Quality details popover
        Popup qualityPopup = new Popup();
        qualityPopup.setAutoHide(true);

        VBox qPopupRoot = new VBox(10);
        qPopupRoot.setPadding(new Insets(15, 20, 15, 20));
        qPopupRoot.setAlignment(Pos.CENTER);
        qPopupRoot.setPrefWidth(240);
        qPopupRoot.setStyle(
                "-fx-background-color: #202020; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);");

        SVGPath largeWave = SVGIcons.createWaveIcon(32, Color.web("rgba(255,255,255,0.75)"));

        Label qHeader = new Label("Lossless");
        qHeader.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label qDetails = new Label("24-bit 48 kHz ALAC");
        qDetails.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");

        qPopupRoot.getChildren().addAll(largeWave, qHeader, qDetails);
        qualityPopup.getContent().add(qPopupRoot);

        // Helper to format quality details dynamically
        java.util.function.BiConsumer<Song, Boolean> formatQuality = (song, isInit) -> {
            if (song != null) {
                String pathLower = song.getPath().toLowerCase();
                if (pathLower.startsWith("youtube:")) {
                    qHeader.setText("YOUTUBE · AUTO QUALITY");
                    qDetails.setText("Official embedded stream · Provider-controlled quality");
                    waveBtn.setVisible(true);
                    return;
                }
                if (pathLower.startsWith("http://") || pathLower.startsWith("https://")) {
                    qHeader.setText("ONLINE PREVIEW · AAC");
                    qDetails.setText("Streaming preview · Provider-controlled quality");
                    waveBtn.setVisible(true);
                    return;
                }
                String codec = "AAC";
                if (pathLower.endsWith(".flac")) {
                    codec = "FLAC";
                } else if (pathLower.endsWith(".wav")) {
                    codec = "WAV";
                } else if (pathLower.endsWith(".mp3")) {
                    codec = "MP3";
                } else if (pathLower.endsWith(".m4a")) {
                    codec = (song.getBitsPerSample() > 16 || song.isHiRes() || song.getBitRate() > 500) ? "ALAC"
                            : "AAC";
                }

                boolean isHiRes = song.isHiRes();
                if (pathLower.endsWith(".mp3") || (pathLower.endsWith(".m4a") && !codec.equals("ALAC"))) {
                    qHeader.setText("AAC High Quality");
                } else {
                    qHeader.setText(isHiRes ? "Hi-Res Lossless" : "Lossless");
                }

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
                qDetails.setText(depth + " " + rate + " " + codec);
                waveBtn.setVisible(true);
            } else {
                waveBtn.setVisible(false);
            }
        };

        // Bind quality popup data
        javafx.beans.value.ChangeListener<Song> qualityListener = (obs, oldSong, newSong) -> {
            formatQuality.accept(newSong, false);
        };
        viewModel.currentSongProperty().addListener(qualityListener);

        // Initial setup
        Song initialSong = viewModel.currentSongProperty().get();
        if (initialSong != null) {
            formatQuality.accept(initialSong, true);
        } else {
            waveBtn.setVisible(false);
        }

        waveBtn.setOnAction(e -> {
            if (qualityPopup.isShowing()) {
                qualityPopup.hide();
            } else {
                javafx.geometry.Bounds bounds = waveBtn.localToScreen(waveBtn.getBoundsInLocal());
                if (bounds != null) {
                    qualityPopup.show(waveBtn, bounds.getMinX() - 100, bounds.getMaxY() + 8);
                }
            }
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button lcdFavBtn = new Button();
        lcdFavBtn.setGraphic(SVGIcons.createStarIcon(12, Color.web("rgba(255,255,255,0.4)"), false));
        lcdFavBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");

        // Bind favorite star
        viewModel.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                lcdFavBtn.setGraphic(SVGIcons.createStarIcon(12,
                        newSong.isFavorite() ? Color.web("#ffcc00") : Color.web("rgba(255,255,255,0.4)"),
                        newSong.isFavorite()));
            } else {
                lcdFavBtn.setGraphic(SVGIcons.createStarIcon(12, Color.web("rgba(255,255,255,0.4)"), false));
            }
        });

        lcdFavBtn.setOnAction(e -> {
            Song song = viewModel.currentSongProperty().get();
            if (song != null) {
                viewModel.toggleFavorite(song);
                lcdFavBtn.setGraphic(SVGIcons.createStarIcon(12,
                        song.isFavorite() ? Color.web("#ffcc00") : Color.web("rgba(255,255,255,0.4)"),
                        song.isFavorite()));
            }
        });

        HBox topRow = new HBox(8, waveBtn, miniTitleContainer, topSpacer, lcdFavBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox metaText = new VBox(2, topRow, miniArtistContainer);
        metaText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(metaText, Priority.ALWAYS);

        topHalf.getChildren().addAll(miniArtContainer, metaText);

        miniProgress = new Slider(0, 100, 0);
        miniProgress.getStyleClass().add("lcd-progress-slider");
        miniProgress.setPrefHeight(2);
        miniProgress.setMinHeight(2);
        miniProgress.setMaxHeight(2);
        miniProgress.setPadding(new Insets(0));

        capsuleContainer.getChildren().addAll(topHalf, miniProgress);

        HBox centerSec = new HBox(35, controlsRow, capsuleContainer);
        centerSec.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerSec, Priority.ALWAYS);

        // --- RIGHT SECTION: Volume, Lyrics, Queue, WinControls
        // ---
        lyricsBtn = new Button();
        lyricsBtn.setGraphic(SVGIcons.createLyricsIcon(14, Color.WHITE));
        lyricsBtn.getStyleClass().add("icon-button");
        lyricsBtn.setOnAction(e -> showRightPanel("lyrics"));

        queueBtn = new Button();
        queueBtn.setGraphic(SVGIcons.createQueueIcon(14, Color.WHITE));
        queueBtn.getStyleClass().add("icon-button");
        queueBtn.setOnAction(e -> showRightPanel("queue"));

        speakerBtn = new Button();
        speakerBtn.setGraphic(SVGIcons.createVolumeIcon(14, Color.WHITE));
        speakerBtn.getStyleClass().add("icon-button");

        Popup volumePopup = new Popup();
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

        speakerBtn.setOnAction(e -> {
            if (volumePopup.isShowing()) {
                volumePopup.hide();
            } else {
                javafx.geometry.Bounds bounds = speakerBtn.localToScreen(speakerBtn.getBoundsInLocal());
                if (bounds != null) {
                    volumePopup.show(speakerBtn, bounds.getMinX() - 80, bounds.getMaxY() + 8);
                }
            }
        });

        // Windows Control Buttons
        Button minBtn = new Button();
        minBtn.getStyleClass().addAll("window-control", "window-control-minimize");
        minBtn.setOnAction(e -> ((Stage) getScene().getWindow()).setIconified(true));

        Button maxBtn = new Button();
        maxBtn.getStyleClass().addAll("window-control", "window-control-maximize");
        maxBtn.setOnAction(e -> {
            Stage stage = (Stage) getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });

        Button closeBtn = new Button();
        closeBtn.getStyleClass().addAll("window-control", "window-control-close");
        closeBtn.setOnAction(e -> {
            viewModel.savePlaybackState();
            LibraryManager.getInstance().shutdown();
            System.exit(0);
        });

        HBox winControls = new HBox(8, minBtn, maxBtn, closeBtn);
        winControls.setAlignment(Pos.CENTER);
        winControls.setPadding(new Insets(0, 0, 0, 15));

        HBox rightSec = new HBox(12, speakerBtn, lyricsBtn, queueBtn, winControls);
        rightSec.setAlignment(Pos.CENTER_RIGHT);
        rightSec.setPrefWidth(240);
        rightSec.setMinWidth(Region.USE_PREF_SIZE); // Prevent squeezing

        StackPane backBtnContainer = new StackPane(backBtn);
        backBtnContainer.setPrefWidth(40);
        backBtnContainer.setMinWidth(40);
        backBtnContainer.setMaxWidth(40);
        backBtnContainer.setAlignment(Pos.CENTER_LEFT);

        bar.getChildren().addAll(backBtnContainer, centerSec, rightSec);
        return bar;
    }

    private void enterMiniPlayer() {
        Stage mainStage = (Stage) getScene().getWindow();
        if (mainStage != null) {
            mainStage.hide();
            MiniPlayerWindow miniPlayer = new MiniPlayerWindow(viewModel, mainStage);
            miniPlayer.show();
        }
    }

    void showRightPanel(String type) {
        boolean isLyrics = type.equalsIgnoreCase("lyrics");

        if (isLyrics) {
            if (!rightPanel.getChildren().contains(lyricsView)) {
                rightPanel.getChildren().remove(queueContainer);
                rightPanel.getChildren().add(lyricsView);
            }
        } else {
            if (!rightPanel.getChildren().contains(queueContainer)) {
                rightPanel.getChildren().remove(lyricsView);
                rightPanel.getChildren().add(queueContainer);
            }
        }

        // If clicking the currently active panel button, toggle it off. Otherwise,
        // switch content.
        if (rightPanel.isVisible()) {
            if ((isLyrics && rightPanel.getChildren().contains(lyricsView))
                    || (!isLyrics && rightPanel.getChildren().contains(queueContainer))) {
                // Toggle off
                TranslateTransition transition = new TranslateTransition(Duration.millis(250), rightPanel);
                transition.setFromX(0);
                transition.setToX(400);
                transition.setOnFinished(e -> rightPanel.setVisible(false));
                transition.play();
            }
        } else {
            // Toggle on
            rightPanel.setVisible(true);
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), rightPanel);
            transition.setFromX(400);
            transition.setToX(0);
            transition.play();
        }
    }

    private void enterFullScreen() {
        if (viewModel.currentSongProperty().get() == null)
            return;

        fullScreenPlayer = new FullScreenPlayer(viewModel, () -> {
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null) {
                stage.setFullScreen(false);
            }
            getChildren().remove(fullScreenPlayer);
            aura.music.ui.views.YoutubePlayerWindow.hideVideo();
            fullScreenPlayer = null;
        }, () -> {
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null) {
                stage.setFullScreen(false);
                stage.hide();
                MiniPlayerWindow miniPlayer = new MiniPlayerWindow(viewModel, stage);
                miniPlayer.show();
            }
            getChildren().remove(fullScreenPlayer);
            aura.music.ui.views.YoutubePlayerWindow.hideVideo();
            fullScreenPlayer = null;
        });

        getChildren().add(fullScreenPlayer);

        Platform.runLater(() -> {
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null) {
                stage.setFullScreenExitHint("");
                stage.setFullScreen(true);
            }
        });
    }

    public void enterFullScreenFromMiniPlayer() {
        Platform.runLater(this::enterFullScreen);
    }

    private void setupBindings() {
        viewModel.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            updateFilteredQueue();
            if (newSong != null) {
                miniTitleContainer.getChildren().setAll(
                        aura.music.ui.MarqueeUtils.createMarqueeLabel(newSong.getTitle(),
                                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.8);",
                                260));
                miniArtistContainer.getChildren().setAll(
                        aura.music.ui.MarqueeUtils.createMarqueeLabel(newSong.getArtist(),
                                "-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;", 280));

                Image onlineArtwork = newSong.getArtworkUrl() == null || newSong.getArtworkUrl().isBlank()
                        ? null : new Image(newSong.getArtworkUrl(), true);
                byte[] artBytes = onlineArtwork == null ? aura.music.library.MetadataExtractor.extractArtworkBytes(newSong.getPath()) : null;
                if (onlineArtwork != null || artBytes != null) {
                    Image image = onlineArtwork != null ? onlineArtwork : new Image(new ByteArrayInputStream(artBytes));
                    miniArtwork.setImage(image);
                    bgImageView.setImage(image);
                } else {
                    miniArtwork.setImage(null);
                    bgImageView.setImage(null);
                }
            } else {
                Label defaultTitle = new Label("");
                defaultTitle
                        .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.7);");
                miniTitleContainer.getChildren().setAll(defaultTitle);
                miniArtistContainer.getChildren().clear();
                miniArtwork.setImage(null);
                bgImageView.setImage(null);
            }
        });

        viewModel.isPlayingProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                Color iconCol = themeEngine.primaryColorProperty().get();
                if (newVal) {
                    miniPlayPause.setGraphic(SVGIcons.createPauseIcon(18, iconCol));
                } else {
                    miniPlayPause.setGraphic(SVGIcons.createPlayIcon(18, iconCol));
                }
            });
        });

        viewModel.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!miniProgress.isValueChanging()) {
                miniProgress.setValue(newVal.doubleValue());
            }
            if (miniTime != null) {
                miniTime.setText(formatTime(newVal.doubleValue()));
            }
        });

        viewModel.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
            miniProgress.setMax(newVal.doubleValue());
            if (miniDuration != null) {
                miniDuration.setText(formatTime(newVal.doubleValue()));
            }
        });

        miniProgress.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (miniProgress.isValueChanging()) {
                viewModel.seek(newVal.doubleValue());
            }
        });

        if (volumeSlider != null) {
            volumeSlider.valueProperty().bindBidirectional(viewModel.volumeProperty());
        }
    }

    private String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", minutes, secs);
    }

    private String toHexString(Color val) {
        if (val == null)
            return "#121212";
        return String.format("#%02x%02x%02x",
                (int) (val.getRed() * 255),
                (int) (val.getGreen() * 255),
                (int) (val.getBlue() * 255));
    }

    private void showSongInfoDialog(Song song) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Song Information");
        alert.setHeaderText(null);

        VBox dialogContent = new VBox(12);
        dialogContent.setPadding(new Insets(15));
        dialogContent.setPrefWidth(450);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label artistLabel = new Label("by " + song.getArtist());
        artistLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px;");

        VBox metaBox = new VBox(6);
        metaBox.setPadding(new Insets(10));
        metaBox.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(8), Insets.EMPTY)));

        Label album = new Label("Album: " + song.getAlbum());
        Label genre = new Label("Genre: " + song.getGenre());
        Label year = new Label("Year: " + (song.getYear() != null ? song.getYear() : "Unknown"));
        Label path = new Label("File: " + song.getPath());
        path.setWrapText(true);

        for (Label l : new Label[] { album, genre, year, path }) {
            l.setStyle("-fx-text-fill: #e5e5e5; -fx-font-size: 12px;");
        }
        metaBox.getChildren().addAll(album, genre, year, path);

        VBox techBox = new VBox(6);
        techBox.setPadding(new Insets(10));
        techBox.setBackground(new Background(
                new BackgroundFill(Color.web("rgba(0, 120, 212, 0.1)"), new CornerRadii(8), Insets.EMPTY)));
        techBox.setStyle("-fx-border-color: rgba(0, 120, 212, 0.3); -fx-border-radius: 8; -fx-border-width: 1;");

        Label techHeader = new Label("AUDIO QUALITY DETAILS");
        techHeader.setStyle("-fx-text-fill: #0078d4; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label codec = new Label("Format: " + (song.getPath().toLowerCase().endsWith(".flac") ? "FLAC" : "AAC"));
        Label bitrate = new Label("Bitrate: " + song.getBitRate() + " kbps");
        Label sampleRate = new Label("Sample Rate: " + song.getSampleRate() + " Hz");
        Label bitDepth = new Label("Bit Depth: " + song.getBitsPerSample() + "-bit");
        Label quality = new Label("Quality: " + (song.isHiRes() ? "Hi-Res Lossless (Studio Quality)" : "Lossless"));

        for (Label l : new Label[] { codec, bitrate, sampleRate, bitDepth, quality }) {
            l.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        }
        quality.setStyle("-fx-text-fill: #38ef7d; -fx-font-size: 12px; -fx-font-weight: bold;");

        techBox.getChildren().addAll(techHeader, codec, bitrate, sampleRate, bitDepth, quality);

        dialogContent.getChildren().addAll(titleLabel, artistLabel, metaBox, techBox);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(dialogContent);
        dialogPane.setStyle("-fx-background-color: #202020; -fx-text-fill: white;");
        dialogPane.getButtonTypes().setAll(ButtonType.CLOSE);

        Button closeBtn = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        closeBtn.setStyle(
                "-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 15; -fx-cursor: hand;");

        alert.showAndWait();
    }

    private class SongListCell extends ListCell<Song> {
        private final HBox layout;
        private final Label titleLabel;
        private final Label artistLabel;
        private final Label albumLabel;
        private final Label durationLabel;
        private final Label hiResBadge;

        public SongListCell() {
            layout = new HBox(15);
            layout.setAlignment(Pos.CENTER_LEFT);
            layout.setPadding(new Insets(8, 12, 8, 12));

            titleLabel = new Label();
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
            titleLabel.setPrefWidth(250);

            hiResBadge = new Label("HR");
            hiResBadge.setStyle(
                    "-fx-background-color: rgba(0, 120, 212, 0.2); -fx-text-fill: #0078d4; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1 4; -fx-background-radius: 2; -fx-border-color: #0078d4; -fx-border-width: 0.5;");
            hiResBadge.setVisible(false);

            artistLabel = new Label();
            artistLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 13px;");
            artistLabel.setPrefWidth(200);

            albumLabel = new Label();
            albumLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 13px;");
            HBox.setHgrow(albumLabel, Priority.ALWAYS);

            durationLabel = new Label();
            durationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 13px;");

            HBox titleContainer = new HBox(8, titleLabel, hiResBadge);
            titleContainer.setAlignment(Pos.CENTER_LEFT);
            titleContainer.setPrefWidth(280);

            layout.getChildren().addAll(titleContainer, artistLabel, albumLabel, durationLabel);

            // Context Menu Setup
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.setStyle(
                    "-fx-background-color: #202020; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.05);");

            MenuItem playNext = new MenuItem("Play Next");
            playNext.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    int currentIndex = viewModel.currentQueueIndexProperty().get();
                    if (currentIndex != -1 && currentIndex + 1 <= viewModel.getQueue().size()) {
                        viewModel.getQueue().add(currentIndex + 1, item);
                    } else {
                        viewModel.getQueue().add(item);
                    }
                }
            });

            MenuItem addToQueue = new MenuItem("Add to Queue");
            addToQueue.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    viewModel.getQueue().add(item);
                }
            });

            MenuItem toggleFav = new MenuItem("Favorite");
            toggleFav.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    viewModel.toggleFavorite(item);
                }
            });

            MenuItem showAlbum = new MenuItem("Go to Album");
            showAlbum.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    showAlbumDetail(item.getAlbum());
                }
            });

            MenuItem showArtist = new MenuItem("Go to Artist");
            showArtist.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    showArtistDetail(item.getArtist());
                }
            });

            MenuItem infoItem = new MenuItem("Song Info");
            infoItem.setOnAction(e -> {
                Song item = getItem();
                if (item != null) {
                    showSongInfoDialog(item);
                }
            });

            contextMenu.getItems().addAll(playNext, addToQueue, new SeparatorMenuItem(), toggleFav,
                    new SeparatorMenuItem(), showAlbum, showArtist, new SeparatorMenuItem(), infoItem);
            setContextMenu(contextMenu);
        }

        @Override
        protected void updateItem(Song item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(item.getTitle());
                artistLabel.setText(item.getArtist());
                albumLabel.setText(item.getAlbum());
                hiResBadge.setVisible(item.isHiRes());

                int minutes = (int) (item.getDuration() / 60);
                int secs = (int) (item.getDuration() % 60);
                durationLabel.setText(String.format("%d:%02d", minutes, secs));

                setGraphic(layout);
            }
        }
    }

    private void showSettingsView() {
        switchViewWithFade(settingsView);
        for (Button b : sidebarButtons) {
            b.getStyleClass().remove("sidebar-item-active");
        }
    }

    private void updateThemeStyles() {
        Color bgCol = themeEngine.backgroundColorProperty().get();
        Color accentCol = themeEngine.accentColorProperty().get();
        Color primaryCol = themeEngine.primaryColorProperty().get();
        Color secondaryCol = themeEngine.secondaryColorProperty().get();
        Color sbCol = themeEngine.sidebarColorProperty().get();

        String bgHex = toHexString(bgCol);
        String accentHex = toHexString(accentCol);
        String primaryHex = toHexString(primaryCol);
        String secondaryHex = toHexString(secondaryCol);
        boolean light = themeEngine.lightModeProperty().get();

        String playerBarBg = light ? "rgba(255, 255, 255, 0.85)" : "rgba(24, 24, 24, 0.85)";
        String sidebarBg = String.format("rgba(%d, %d, %d, 0.45)", (int) (sbCol.getRed() * 255),
                (int) (sbCol.getGreen() * 255), (int) (sbCol.getBlue() * 255));
        String borderCol = light ? "rgba(0, 0, 0, 0.08)" : "rgba(255, 255, 255, 0.06)";
        String cardBg = light ? "rgba(255, 255, 255, 0.6)" : "rgba(35, 35, 35, 0.4)";

        setStyle(String.format(
                "-fx-primary-text: %s; " +
                        "-fx-secondary-text: %s; " +
                        "-fx-bg: %s; " +
                        "-fx-accent-color: %s; " +
                        "-fx-background-color: %s; " +
                        "--accent-color: %s; " +
                        "-fx-player-bar-bg: %s; " +
                        "-fx-custom-border-color: %s; " +
                        "-fx-card-bg: %s;",
                primaryHex, secondaryHex, bgHex, accentHex, bgHex, accentHex, playerBarBg, borderCol, cardBg));

        if (sidebar != null) {
            sidebar.setStyle(String.format(
                    "-fx-background-color: %s; " +
                            "-fx-background-radius: 20; " +
                            "-fx-border-radius: 20; " +
                            "-fx-border-color: %s; " +
                            "-fx-border-width: 1;",
                    sidebarBg, borderCol));
        }

        if (bgDarkening != null) {
            bgDarkening.setStyle(String.format("-fx-background-color: rgba(%d, %d, %d, 0.85);",
                    (int) (bgCol.getRed() * 255), (int) (bgCol.getGreen() * 255), (int) (bgCol.getBlue() * 255)));
        }

        if (bgImageView != null) {
            bgImageView.setVisible(themeEngine.dynamicColoringProperty().get());
        }

        // Determine primary and secondary colors for icons
        Color primaryIconCol = themeEngine.primaryColorProperty().get();
        Color secondaryIconCol = themeEngine.secondaryColorProperty().get();

        // If shuffle is active, use accent color, otherwise secondary color
        Color shuffleCol = viewModel.shuffleModeProperty().get() ? accentCol : secondaryIconCol;
        // If repeat is active, use accent color (or gold for ONE), otherwise secondary
        // color
        Color repeatCol = secondaryIconCol;
        if (viewModel.repeatModeProperty().get() == MainViewModel.RepeatMode.ALL) {
            repeatCol = accentCol;
        } else if (viewModel.repeatModeProperty().get() == MainViewModel.RepeatMode.ONE) {
            repeatCol = Color.web("#ffcc00");
        }

        if (shuffleBtn != null) {
            shuffleBtn.setGraphic(SVGIcons.createShuffleIcon(14, shuffleCol));
        }
        if (prevBtn != null) {
            prevBtn.setGraphic(SVGIcons.createPreviousIcon(14, primaryIconCol));
        }
        if (miniPlayPause != null) {
            boolean isPlaying = viewModel.isPlayingProperty().get();
            miniPlayPause.setGraphic(isPlaying ? SVGIcons.createPauseIcon(16, primaryIconCol)
                    : SVGIcons.createPlayIcon(16, primaryIconCol));
        }
        if (nextBtn != null) {
            nextBtn.setGraphic(SVGIcons.createNextIcon(14, primaryIconCol));
        }
        if (repeatBtn != null) {
            repeatBtn.setGraphic(SVGIcons.createRepeatIcon(14, repeatCol));
        }
        if (speakerBtn != null) {
            speakerBtn.setGraphic(SVGIcons.createVolumeIcon(14, primaryIconCol));
        }
        if (lyricsBtn != null) {
            lyricsBtn.setGraphic(SVGIcons.createLyricsIcon(14, primaryIconCol));
        }
        if (queueBtn != null) {
            queueBtn.setGraphic(SVGIcons.createQueueIcon(14, primaryIconCol));
        }

        // Sidebar icons & text dynamic updates
        if (homeBtn != null)
            homeBtn.setGraphic(SVGIcons.createHomeIcon(16, secondaryIconCol));
        if (browseBtn != null)
            browseBtn.setGraphic(SVGIcons.createCompassIcon(16, secondaryIconCol));
        if (albumsBtn != null)
            albumsBtn.setGraphic(SVGIcons.createAlbumIcon(14, secondaryIconCol));
        if (artistsBtn != null)
            artistsBtn.setGraphic(SVGIcons.createArtistIcon(14, secondaryIconCol));
        if (genresBtn != null)
            genresBtn.setGraphic(SVGIcons.createGenreIcon(14, secondaryIconCol));
        if (songsBtn != null)
            songsBtn.setGraphic(SVGIcons.createMusicIcon(14, secondaryIconCol));
        if (favoritesBtn != null)
            favoritesBtn.setGraphic(SVGIcons.createHeartIcon(14, secondaryIconCol, true));
        if (playlistsBtn != null)
            playlistsBtn.setGraphic(SVGIcons.createPlaylistIcon(14, secondaryIconCol));
        if (settingsBtn != null)
            settingsBtn.setGraphic(SVGIcons.createSettingsIcon(14, secondaryIconCol));
        if (toggleSidebarBtn != null)
            toggleSidebarBtn.setGraphic(SVGIcons.createMenuIcon(16, primaryIconCol));

        if (searchIcon != null) {
            searchIcon.setFill(secondaryIconCol);
        }

        if (logo != null) {
            logo.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: " + primaryHex + ";");
        }

        if (backBtn != null) {
            backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + primaryHex
                    + "; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 5 0 0;");
        }

        if (navHeader != null) {
            navHeader.setStyle("-fx-text-fill: " + secondaryHex
                    + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 10 0 5 10;");
        }

        if (libraryHeader != null) {
            libraryHeader.setStyle("-fx-text-fill: " + secondaryHex
                    + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 15 0 5 10;");
        }

        if (searchField != null) {
            String searchBg = light ? "rgba(0,0,0,0.05)" : "rgba(255,255,255,0.06)";
            searchField.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 8; -fx-padding: 0 10 0 35; -fx-prompt-text-fill: %s; -fx-font-size: 13px;",
                    searchBg, primaryHex, secondaryHex));
        }

        if (addFolderBtn != null) {
            String btnBg = light ? "rgba(0,0,0,0.05)" : "rgba(255,255,255,0.06)";
            addFolderBtn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;",
                    btnBg, primaryHex));
        }

        if (profileName != null) {
            profileName.setStyle("-fx-text-fill: " + primaryHex + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        }
        if (statsLabel != null) {
            statsLabel.setStyle("-fx-text-fill: " + secondaryHex + "; -fx-font-size: 10px;");
        }
    }

    private void populateAlbumsList() {
        if (albumCatalog.isEmpty())
            albumCatalog = buildBrowseCatalog("albums", Song::getAlbum);
        albumsListView.getItems().setAll(extractBrowseNames(albumCatalog, Song::getAlbum));
    }

    private void populateArtistsList() {
        if (artistCatalog.isEmpty())
            artistCatalog = buildBrowseCatalog("artists", Song::getArtist);
        artistsListView.getItems().setAll(extractBrowseNames(artistCatalog, Song::getArtist));
    }

    private void populateGenresList() {
        if (genreCatalog.isEmpty())
            genreCatalog = buildBrowseCatalog("genres", Song::getGenre);
        genresListView.getItems().setAll(extractBrowseNames(genreCatalog, Song::getGenre));
    }

    private java.util.List<String> extractBrowseNames(java.util.List<Song> catalog,
            java.util.function.Function<Song, String> nameExtractor) {
        return catalog.stream().map(nameExtractor).toList();
    }

    private class AlbumListCell extends ListCell<String> {
        private final HBox layout;
        private final ImageView artView;
        private final Label titleLabel;
        private final Label subtitleLabel;

        public AlbumListCell() {
            layout = new HBox(15);
            layout.setAlignment(Pos.CENTER_LEFT);
            layout.setPadding(new Insets(6, 12, 6, 12));

            artView = new ImageView();
            artView.setFitWidth(40);
            artView.setFitHeight(40);
            artView.setPreserveRatio(true);
            Rectangle clip = new Rectangle(40, 40);
            clip.setArcWidth(8);
            clip.setArcHeight(8);
            artView.setClip(clip);

            titleLabel = new Label();
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-primary-text; -fx-font-size: 14px;");

            subtitleLabel = new Label();
            subtitleLabel.setStyle("-fx-text-fill: -fx-secondary-text; -fx-font-size: 12px;");

            layout.getChildren().addAll(artView, titleLabel, subtitleLabel);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(item);
                Song sample = findSampleSong(item);
                if (sample != null) {
                    if (albumsListView.getItems().contains(item)) {
                        subtitleLabel.setText("by " + sample.getArtist());
                    } else if (artistsListView.getItems().contains(item)) {
                        subtitleLabel.setText("Artist");
                    } else {
                        subtitleLabel.setText("Genre");
                    }
                    Image cachedImage = aura.music.utils.ImageCache.getCachedImage(sample.getPath());
                    if (cachedImage != null) {
                        artView.setImage(cachedImage);
                    } else {
                        artView.setImage(null);
                        String expectedItem = item;
                        java.util.concurrent.CompletableFuture
                                .supplyAsync(() -> aura.music.library.MetadataExtractor.extractArtworkBytes(sample.getPath()))
                                .thenAcceptAsync(artBytes -> {
                                    if (!isEmpty() && expectedItem.equals(getItem()) && artBytes != null) {
                                        artView.setImage(aura.music.utils.ImageCache.getImage(sample.getPath(), artBytes,
                                                40, 40));
                                    }
                                }, Platform::runLater);
                    }
                } else {
                    subtitleLabel.setText("");
                    artView.setImage(null);
                }
                setGraphic(layout);
            }
        }
    }

    private void updateFilteredQueue() {
        Platform.runLater(() -> {
            filteredQueue.clear();
            Song current = viewModel.currentSongProperty().get();
            if (current != null) {
                filteredQueue.add(current);
                int idx = viewModel.getQueue().indexOf(current);
                if (idx != -1) {
                    for (int i = idx + 1; i < viewModel.getQueue().size(); i++) {
                        filteredQueue.add(viewModel.getQueue().get(i));
                    }
                }
            } else {
                filteredQueue.addAll(viewModel.getQueue());
            }
        });
    }

    private Song findSampleSong(String key) {
        for (Song s : viewModel.getLibrarySongs()) {
            if (key.equalsIgnoreCase(s.getAlbum()) || key.equalsIgnoreCase(s.getArtist())
                    || key.equalsIgnoreCase(s.getGenre())) {
                return s;
            }
        }
        return null;
    }
}
