package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.List;

@SuppressWarnings("unused")
public class HomeView extends ScrollPane {

    private final MainViewModel viewModel;
    private final VBox content;

    // Random Song UI Components
    private Song currentRandomSong;

    public HomeView(MainViewModel viewModel, java.util.function.Consumer<String> onSectionChange,
            java.util.function.Consumer<String> onAlbumSelect,
            java.util.function.Consumer<String> onGenreSelect,
            java.util.function.Consumer<String> onArtistSelect) {
        this.viewModel = viewModel;

        // Configure ScrollPane
        setFitToWidth(true);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        content = new VBox(35);
        content.setPadding(new Insets(30, 40, 40, 40));
        content.setStyle("-fx-background-color: transparent;");

        // 1. Dynamic Greeting Header + Right-Aligned Quick Actions Bar
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label greetingLabel = new Label();
        greetingLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        updateGreeting(greetingLabel);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox quickActions = createQuickActionsBar(onSectionChange);

        headerBox.getChildren().addAll(greetingLabel, headerSpacer, quickActions);
        content.getChildren().add(headerBox);

        // 2. Two-Column Layout (Daily Special & Up Next)
        HBox twoColumnGrid = createTwoColumnLayout();
        content.getChildren().add(twoColumnGrid);

        // 4. Recently Played Section
        VBox recentlyPlayed = createRecentlyPlayedSection();
        content.getChildren().add(recentlyPlayed);

        // 5. Best New Songs Section
        VBox bestNewSongs = createBestNewSongsSection();
        content.getChildren().add(bestNewSongs);

        // 6. Top Albums Section
        VBox topAlbums = createTopAlbumsSection(onAlbumSelect);
        content.getChildren().add(topAlbums);

        // 7. Top Artists Section
        VBox topArtists = createTopArtistsSection(onArtistSelect);
        content.getChildren().add(topArtists);

        // 8. Library Insights / Statistics
        VBox statsSection = createLibraryStatsSection();
        content.getChildren().add(statsSection);

        setContent(content);
    }

    private void updateGreeting(Label greetingLabel) {
        java.time.LocalTime time = java.time.LocalTime.now();
        int hour = time.getHour();
        String greeting = "Good Night";
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening";
        }

        String username = System.getProperty("user.name");
        if (username == null || username.trim().isEmpty()) {
            username = "User";
        }
        greetingLabel.setText(greeting + ", " + username);
    }

    private HBox createTwoColumnLayout() {
        HBox container = new HBox(40);

        VBox currentCard = createSongCard("Daily Special");
        VBox nextCard = createSongCard("Up Next");

        // Add random roll button to Daily Special
        Button rollBtn = new Button("🎲");
        rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;");
        rollBtn.setOnMouseEntered(e -> rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;"));
        rollBtn.setOnMouseExited(e -> rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;"));
        rollBtn.setOnMouseClicked(e -> {
            e.consume(); // Prevent card click
            pickNewRandomSong();
            updateSongCardUI(currentCard, currentRandomSong);
        });

        currentCard.setCursor(javafx.scene.Cursor.HAND);
        currentCard.setOnMouseClicked(e -> {
            if (currentRandomSong != null) {
                viewModel.play(currentRandomSong);
            }
        });

        HBox titleBox = (HBox) currentCard.getChildren().get(0);
        titleBox.getChildren().add(rollBtn);

        nextCard.setCursor(javafx.scene.Cursor.HAND);
        nextCard.setOnMouseClicked(e -> {
            int nextIndex = viewModel.currentQueueIndexProperty().get() + 1;
            if (nextIndex > 0 && nextIndex < viewModel.getQueue().size()) {
                Song nSong = viewModel.getQueue().get(nextIndex);
                viewModel.play(nSong);
            }
        });

        container.getChildren().addAll(currentCard, nextCard);

        Runnable updateUI = () -> {
            Platform.runLater(() -> {
                updateSongCardUI(currentCard, currentRandomSong);

                int nextIndex = viewModel.currentQueueIndexProperty().get() + 1;
                Song nextSong = null;
                if (nextIndex > 0 && nextIndex < viewModel.getQueue().size()) {
                    nextSong = viewModel.getQueue().get(nextIndex);
                }
                updateSongCardUI(nextCard, nextSong);
            });
        };

        viewModel.getQueue().addListener((javafx.collections.ListChangeListener<Song>) c -> updateUI.run());
        viewModel.currentQueueIndexProperty().addListener((obs, o, n) -> updateUI.run());
        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            if (currentRandomSong == null) {
                pickNewRandomSong();
            }
            updateUI.run();
        });

        // Initialize random song
        pickNewRandomSong();
        updateUI.run();

        return container;
    }

    private void pickNewRandomSong() {
        List<Song> songs = viewModel.getLibrarySongs();
        if (songs.isEmpty()) {
            this.currentRandomSong = null;
        } else {
            java.util.Random rand = new java.util.Random();
            this.currentRandomSong = songs.get(rand.nextInt(songs.size()));
        }
    }

    private VBox createTopAlbumsSection(java.util.function.Consumer<String> onAlbumSelect) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Top Albums");
        sectionTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        rebuildTopAlbums(cardsRow, onAlbumSelect);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildTopAlbums(cardsRow, onAlbumSelect));
        });

        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildTopAlbums(HBox container, java.util.function.Consumer<String> onAlbumSelect) {
        container.getChildren().clear();
        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> albumMap = new java.util.LinkedHashMap<>();
        for (Song s : songs) {
            String album = s.getAlbum();
            if (album != null && !album.isEmpty()) {
                albumMap.computeIfAbsent(album, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (albumMap.isEmpty()) {
            Song firstSong = songs.isEmpty() ? null : songs.get(0);
            container.getChildren().addAll(
                    createAlbumCard("Abbey Road", "The Beatles", firstSong, null),
                    createAlbumCard("Dark Side of the Moon", "Pink Floyd", firstSong, null),
                    createAlbumCard("Thriller", "Michael Jackson", firstSong, null),
                    createAlbumCard("Back in Black", "AC/DC", firstSong, null),
                    createAlbumCard("Random Access Memories", "Daft Punk", firstSong, null));
        } else {
            List<String> albums = new java.util.ArrayList<>(albumMap.keySet());
            int count = Math.min(15, albums.size());
            for (int i = 0; i < count; i++) {
                String albumName = albums.get(i);
                List<Song> albumSongs = albumMap.get(albumName);
                Song representative = albumSongs.get(0);

                container.getChildren()
                        .add(createAlbumCard(albumName, representative.getArtist(), representative, () -> {
                            if (onAlbumSelect != null) {
                                onAlbumSelect.accept(albumName);
                            }
                        }));
            }
        }
    }

    private Pane createAlbumCard(String title, String artist, Song representative, Runnable onClick) {
        VBox card = new VBox(8);
        card.setPrefWidth(150);
        card.setMinWidth(150);
        card.setMaxWidth(150);
        card.setStyle("-fx-cursor: hand;");

        StackPane artContainer = new StackPane();
        artContainer.setPrefSize(150, 150);
        artContainer.setMinSize(150, 150);
        artContainer.setMaxSize(150, 150);
        artContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(12), Insets.EMPTY)));

        ImageView artView = new ImageView();
        artView.setFitWidth(150);
        artView.setFitHeight(150);
        artView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        artView.setClip(clip);

        Label placeholder = new Label("💿");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
        artContainer.getChildren().add(placeholder);

        if (representative != null) {
            aura.music.library.ArtworkCache.getInstance().getArtworkAsync(representative, img -> {
                if (img != null)
                    artView.setImage(img);
            });
        }
        artContainer.getChildren().add(artView);

        DropShadow shadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.3));
        artContainer.setEffect(shadow);

        javafx.scene.Node titleLabel = aura.music.ui.MarqueeUtils.createMarqueeLabel(title,
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;", 140);
        javafx.scene.Node artistLabel = aura.music.ui.MarqueeUtils.createMarqueeLabel(artist,
                "-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;", 140);

        card.getChildren().addAll(artContainer, titleLabel, artistLabel);

        card.setOnMouseEntered(e -> {
            artContainer.setScaleX(1.04);
            artContainer.setScaleY(1.04);
        });
        card.setOnMouseExited(e -> {
            artContainer.setScaleX(1.0);
            artContainer.setScaleY(1.0);
        });

        if (onClick != null) {
            card.setOnMouseClicked(e -> onClick.run());
        }

        return card;
    }

    private Pane createPlaceholderAlbumCard(String title, String artist) {
        return createAlbumCard(title, artist, null, null);
    }

    private VBox createFavoritesSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Favorite Songs");
        sectionTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        rebuildFavorites(cardsRow);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildFavorites(cardsRow));
        });

        viewModel.favoritesVersionProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> rebuildFavorites(cardsRow));
        });

        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildFavorites(HBox container) {
        container.getChildren().clear();
        List<Song> favs = new java.util.ArrayList<>();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.isFavorite()) {
                favs.add(s);
            }
        }

        if (favs.isEmpty()) {
            Label emptyLabel = new Label("No favorites yet");
            emptyLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 13px; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return;
        }

        for (Song song : favs) {
            container.getChildren().add(createRecentSongCard(song));
        }
    }

    private VBox createRecentlyPlayedSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Recently Played");
        sectionTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        rebuildRecentlyPlayedHorizontal(cardsRow);

        viewModel.getRecentlyPlayed().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildRecentlyPlayedHorizontal(cardsRow));
        });

        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildRecentlyPlayedHorizontal(HBox container) {
        container.getChildren().clear();
        List<Song> recent = viewModel.getRecentlyPlayed();
        if (recent.isEmpty()) {
            Label emptyLabel = new Label("No recently played tracks yet");
            emptyLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 13px; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return;
        }

        int count = Math.min(15, recent.size());
        for (int i = 0; i < count; i++) {
            Song song = recent.get(i);
            container.getChildren().add(createRecentSongCard(song));
        }
    }

    private Pane createRecentSongCard(Song song) {
        VBox card = new VBox(8);
        card.setPrefWidth(150);
        card.setMinWidth(150);
        card.setMaxWidth(150);
        card.setStyle("-fx-cursor: hand;");

        StackPane artContainer = new StackPane();
        artContainer.setPrefSize(150, 150);
        artContainer.setMinSize(150, 150);
        artContainer.setMaxSize(150, 150);
        artContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(12), Insets.EMPTY)));

        ImageView artView = new ImageView();
        artView.setFitWidth(150);
        artView.setFitHeight(150);
        artView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        artView.setClip(clip);

        Label placeholder = new Label("🎵");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
        artContainer.getChildren().add(placeholder);

        if (song != null) {
            aura.music.library.ArtworkCache.getInstance().getArtworkAsync(song, img -> {
                if (img != null)
                    artView.setImage(img);
            });
        }
        artContainer.getChildren().add(artView);

        DropShadow shadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.3));
        artContainer.setEffect(shadow);

        javafx.scene.Node titleLabel = aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;", 140);
        javafx.scene.Node artistLabel = aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                "-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;", 140);

        card.getChildren().addAll(artContainer, titleLabel, artistLabel);

        card.setOnMouseEntered(e -> {
            artContainer.setScaleX(1.04);
            artContainer.setScaleY(1.04);
        });
        card.setOnMouseExited(e -> {
            artContainer.setScaleX(1.0);
            artContainer.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> viewModel.play(song));

        return card;
    }

    private VBox createSongCard(String title) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setPrefHeight(480);
        card.setMinHeight(480);
        card.setMaxHeight(480);
        card.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setBackground(new Background(
                new BackgroundFill(Color.web("rgba(255,255,255,0.05)"), new CornerRadii(16), Insets.EMPTY)));

        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleBox.getChildren().addAll(titleLabel, spacer);

        // Artwork
        StackPane artContainer = new StackPane();
        artContainer.setPrefSize(300, 300);
        artContainer.setMinSize(300, 300);
        artContainer.setMaxSize(300, 300);
        artContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#333"), new CornerRadii(12), Insets.EMPTY)));

        // Song Name + Badge
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setMaxWidth(300);
        StackPane songContainer = new StackPane();
        songContainer.setAlignment(Pos.CENTER_LEFT);
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        Label badgeLabel = new Label("Lossless");
        badgeLabel.setMinWidth(Region.USE_PREF_SIZE);
        badgeLabel.setStyle(
                "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
        nameBox.getChildren().addAll(songContainer, badgeSpacer, badgeLabel);

        // Artist Name + Specs
        HBox artistBox = new HBox(10);
        artistBox.setAlignment(Pos.CENTER_LEFT);
        artistBox.setMaxWidth(300);
        StackPane artistContainer = new StackPane();
        artistContainer.setAlignment(Pos.CENTER_LEFT);
        Region specsSpacer = new Region();
        HBox.setHgrow(specsSpacer, Priority.ALWAYS);
        Label specsLabel = new Label("16-bit / 44.1 kHz");
        specsLabel.setMinWidth(Region.USE_PREF_SIZE);
        specsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");
        artistBox.getChildren().addAll(artistContainer, specsSpacer, specsLabel);

        card.getChildren().addAll(titleBox, artContainer, nameBox, artistBox);

        card.getProperties().put("artContainer", artContainer);
        card.getProperties().put("songContainer", songContainer);
        card.getProperties().put("badgeLabel", badgeLabel);
        card.getProperties().put("artistContainer", artistContainer);
        card.getProperties().put("specsLabel", specsLabel);

        return card;
    }

    private void updateSongCardUI(VBox card, Song song) {
        StackPane artContainer = (StackPane) card.getProperties().get("artContainer");
        StackPane songContainer = (StackPane) card.getProperties().get("songContainer");
        Label badgeLabel = (Label) card.getProperties().get("badgeLabel");
        StackPane artistContainer = (StackPane) card.getProperties().get("artistContainer");
        Label specsLabel = (Label) card.getProperties().get("specsLabel");

        if (song == null) {
            Label noSongLabel = new Label("No song");
            noSongLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            songContainer.getChildren().setAll(noSongLabel);

            Label noArtistLabel = new Label("-");
            noArtistLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 14px;");
            artistContainer.getChildren().setAll(noArtistLabel);

            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
            specsLabel.setText("");
            artContainer.getChildren().clear();
            Label placeholder = new Label("🎵");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 40px;");
            artContainer.getChildren().add(placeholder);
            card.setOpacity(0.5);
            return;
        }

        card.setOpacity(1.0);

        songContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                        "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;", 180));
        artistContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                        "-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 14px;", 180));

        artContainer.getChildren().clear();
        Label placeholder = new Label("🎵");
        placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 40px;");
        artContainer.getChildren().add(placeholder);

        aura.music.library.ArtworkCache.getInstance().getArtworkAsync(song, img -> {
            if (img != null) {
                artContainer.getChildren().clear();
                ImageView artView = new ImageView(img);
                artView.setFitWidth(300);
                artView.setFitHeight(300);
                artView.setPreserveRatio(true);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 300);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                artView.setClip(clip);
                artContainer.getChildren().add(artView);
            }
        });

        badgeLabel.setVisible(true);
        badgeLabel.setManaged(true);
        String format = "Unknown";
        String path = song.getPath().toLowerCase();
        if (path.endsWith(".flac"))
            format = "FLAC";
        else if (path.endsWith(".wav"))
            format = "WAV";
        else if (path.endsWith(".mp3"))
            format = "MP3";
        else if (path.endsWith(".aac") || path.endsWith(".m4a"))
            format = "AAC";
        else if (path.endsWith(".ogg"))
            format = "OGG";

        int bitRate = song.getBitRate();
        int sampleRate = song.getSampleRate();
        int bits = song.getBitsPerSample();

        String desc = format;
        if (bits > 0 || sampleRate > 0) {
            desc += " • ";
            if (bits > 0)
                desc += bits + "-bit / ";
            if (sampleRate > 0)
                desc += (sampleRate / 1000.0) + " kHz";
        } else if (bitRate > 0) {
            desc += " • " + bitRate + " kbps";
        }
        specsLabel.setText(desc);

        if (song.isHiRes()) {
            badgeLabel.setText("Hi-Res Lossless");
            badgeLabel.setStyle(
                    "-fx-background-color: #E2B714; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
        } else if (format.equals("FLAC") || format.equals("WAV")) {
            badgeLabel.setText("Lossless");
            badgeLabel.setStyle(
                    "-fx-background-color: #ffffff; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
        } else {
            badgeLabel.setText("Lossless");
            badgeLabel.setStyle(
                    "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
        }
    }

    private VBox createBestNewSongsSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        HBox headerRow = new HBox(5);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Best New Songs");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        headerRow.getChildren().addAll(sectionTitle);

        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(16);
        grid.setStyle("-fx-background-color: transparent;");

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }

        rebuildBestNewSongs(grid);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildBestNewSongs(grid));
        });

        section.getChildren().addAll(headerRow, grid);
        return section;
    }

    private void rebuildBestNewSongs(GridPane grid) {
        grid.getChildren().clear();
        List<Song> songs = viewModel.getLibrarySongs();
        if (songs.isEmpty()) {
            Label emptyLabel = new Label("No new songs found in library");
            emptyLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 13px; -fx-font-style: italic;");
            grid.add(emptyLabel, 0, 0, 3, 1);
            return;
        }

        int count = Math.min(15, songs.size());
        for (int i = 0; i < count; i++) {
            Song song = songs.get(i);
            Pane songRow = createSongRowItem(song);

            int col = i % 3;
            int row = i / 3;
            grid.add(songRow, col, row);
        }
    }

    private Pane createSongRowItem(Song song) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 6 0; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        StackPane artContainer = new StackPane();
        artContainer.setPrefSize(44, 44);
        artContainer.setMinSize(44, 44);
        artContainer.setMaxSize(44, 44);

        ImageView artView = new ImageView();
        artView.setFitWidth(44);
        artView.setFitHeight(44);
        artView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        artView.setClip(clip);

        Label placeholder = new Label("🎵");
        placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.2);");
        artContainer.getChildren().add(placeholder);

        aura.music.library.ArtworkCache.getInstance().getArtworkAsync(song, img -> {
            if (img != null)
                artView.setImage(img);
        });
        artContainer.getChildren().add(artView);

        VBox textCol = new VBox(2);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");

        textCol.getChildren().addAll(titleLabel, artistLabel);

        Button optionsBtn = new Button("•••");
        optionsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ff2d55; -fx-font-size: 11px; -fx-cursor: hand;");
        optionsBtn.setOnAction(e -> {
            e.consume();
            aura.music.ui.components.MenuUtils.showSongContextMenu(optionsBtn, song, viewModel);
        });

        row.getChildren().addAll(artContainer, textCol, optionsBtn);

        row.setOnMouseClicked(e -> {
            viewModel.play(song);
        });

        return row;
    }

    private VBox createTopArtistsSection(java.util.function.Consumer<String> onArtistSelect) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Top Artists");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        rebuildTopArtists(cardsRow, onArtistSelect);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildTopArtists(cardsRow, onArtistSelect));
        });

        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildTopArtists(HBox container, java.util.function.Consumer<String> onArtistSelect) {
        container.getChildren().clear();
        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> artistMap = new java.util.LinkedHashMap<>();
        for (Song s : songs) {
            String artist = s.getArtist();
            if (artist != null && !artist.isEmpty()) {
                artistMap.computeIfAbsent(artist, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (artistMap.isEmpty()) {
            Song firstSong = songs.isEmpty() ? null : songs.get(0);
            container.getChildren().addAll(
                    createSquareCard("Lata Mangeshkar", "Artist", firstSong, null),
                    createSquareCard("Arijit Singh", "Artist", firstSong, null),
                    createSquareCard("Ed Sheeran", "Artist", firstSong, null),
                    createSquareCard("Coldplay", "Artist", firstSong, null),
                    createSquareCard("Taylor Swift", "Artist", firstSong, null));
        } else {
            List<String> artists = new java.util.ArrayList<>(artistMap.keySet());
            int count = Math.min(15, artists.size());
            for (int i = 0; i < count; i++) {
                String artistName = artists.get(i);
                List<Song> artistSongs = artistMap.get(artistName);
                Song representative = artistSongs.get(0);

                container.getChildren().add(createSquareCard(artistName, "Artist", representative, () -> {
                    if (onArtistSelect != null) {
                        onArtistSelect.accept(artistName);
                    }
                }));
            }
        }
    }

    private VBox createTrendyGenerSection(java.util.function.Consumer<String> onGenreSelect) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Trendy Genres");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        rebuildTrendyGenres(cardsRow, onGenreSelect);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildTrendyGenres(cardsRow, onGenreSelect));
        });

        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildTrendyGenres(HBox container, java.util.function.Consumer<String> onGenreSelect) {
        container.getChildren().clear();
        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> genreMap = new java.util.LinkedHashMap<>();
        for (Song s : songs) {
            String genre = s.getGenre();
            if (genre != null && !genre.isEmpty()) {
                genreMap.computeIfAbsent(genre, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (genreMap.isEmpty()) {
            Song firstSong = songs.isEmpty() ? null : songs.get(0);
            container.getChildren().addAll(
                    createSquareCard("Pop", "Genre", firstSong, null),
                    createSquareCard("Rock", "Genre", firstSong, null),
                    createSquareCard("Hip-Hop", "Genre", firstSong, null),
                    createSquareCard("Classical", "Genre", firstSong, null),
                    createSquareCard("Jazz", "Genre", firstSong, null));
        } else {
            List<String> genres = new java.util.ArrayList<>(genreMap.keySet());
            int count = Math.min(15, genres.size());
            for (int i = 0; i < count; i++) {
                String genreName = genres.get(i);
                List<Song> genreSongs = genreMap.get(genreName);
                Song representative = genreSongs.get(0);

                container.getChildren().add(createSquareCard(genreName, "Genre", representative, () -> {
                    if (onGenreSelect != null) {
                        onGenreSelect.accept(genreName);
                    }
                }));
            }
        }
    }

    private Pane createSquareCard(Object genername2, String subtitle, Song representative, Runnable onClick) {
        VBox card = new VBox(8);
        card.setPrefWidth(150);
        card.setMinWidth(150);
        card.setMaxWidth(150);
        card.setStyle("-fx-cursor: hand;");

        StackPane artContainer = new StackPane();
        artContainer.setPrefSize(150, 150);
        artContainer.setMinSize(150, 150);
        artContainer.setMaxSize(150, 150);
        artContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#2a2a2a"), new CornerRadii(12), Insets.EMPTY)));

        ImageView artView = new ImageView();
        artView.setFitWidth(150);
        artView.setFitHeight(150);
        artView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        artView.setClip(clip);

        Label placeholder = new Label("🎵");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
        artContainer.getChildren().add(placeholder);

        if (representative != null) {
            aura.music.library.ArtworkCache.getInstance().getArtworkAsync(representative, img -> {
                if (img != null)
                    artView.setImage(img);
            });
        }
        artContainer.getChildren().add(artView);

        DropShadow shadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.3));
        artContainer.setEffect(shadow);

        Label titleLabel = new Label(genername2 != null ? genername2.toString() : "");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        titleLabel.setWrapText(false);

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");
        subLabel.setWrapText(false);

        card.getChildren().addAll(artContainer, titleLabel, subLabel);

        card.setOnMouseEntered(e -> {
            artContainer.setScaleX(1.04);
            artContainer.setScaleY(1.04);
        });
        card.setOnMouseExited(e -> {
            artContainer.setScaleX(1.0);
            artContainer.setScaleY(1.0);
        });

        if (onClick != null) {
            card.setOnMouseClicked(e -> onClick.run());
        }

        return card;
    }

    private HBox createQuickActionsBar(java.util.function.Consumer<String> onSectionChange) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);

        Button favsBtn = createQuickActionButton("Favorites", "rgba(255,255,255,0.08)");
        favsBtn.setOnAction(e -> {
            if (onSectionChange != null) {
                onSectionChange.accept("Favorites");
            }
        });

        Button importBtn = createQuickActionButton("📁 Import Folder", "rgba(255,255,255,0.08)");
        importBtn.setOnAction(e -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Select Music Folder");
            java.io.File selected = chooser.showDialog(getScene().getWindow());
            if (selected != null) {
                aura.music.library.LibraryManager.getInstance().addWatchedFolder(selected.getAbsolutePath());
                viewModel.getLibrarySongs().setAll(aura.music.library.LibraryManager.getInstance().getSongs());
            }
        });

        row.getChildren().addAll(favsBtn, importBtn);
        return row;
    }

    private Button createQuickActionButton(String text, String bg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private VBox createLibraryStatsSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Library Insights");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox statsGrid = new HBox(20);
        statsGrid.setAlignment(Pos.CENTER_LEFT);

        rebuildLibraryStats(statsGrid);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildLibraryStats(statsGrid));
        });

        section.getChildren().addAll(sectionTitle, statsGrid);
        return section;
    }

    private void rebuildLibraryStats(HBox container) {
        container.getChildren().clear();
        List<Song> songs = viewModel.getLibrarySongs();

        java.util.Set<String> artists = new java.util.HashSet<>();
        java.util.Set<String> albums = new java.util.HashSet<>();
        int favCount = 0;

        for (Song s : songs) {
            if (s.getArtist() != null && !s.getArtist().isEmpty())
                artists.add(s.getArtist());
            if (s.getAlbum() != null && !s.getAlbum().isEmpty())
                albums.add(s.getAlbum());
            if (s.isFavorite())
                favCount++;
        }

        container.getChildren().addAll(
                createStatCard("Total Songs", String.format("%,d", songs.size()), "🎵"),
                createStatCard("Artists", String.format("%,d", artists.size()), "🎤"),
                createStatCard("Albums", String.format("%,d", albums.size()), "💿"),
                createStatCard("Favorites", String.format("%,d", favCount), "❤️"));
    }

    private VBox createStatCard(String title, String value, String icon) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 22, 16, 22));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 14;");
        card.setMinWidth(140);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.5);");

        card.getChildren().addAll(iconLabel, valLabel, titleLabel);
        return card;
    }
}