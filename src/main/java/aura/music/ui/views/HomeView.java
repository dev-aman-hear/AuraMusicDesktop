package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.ui.components.MenuUtils;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import java.io.ByteArrayInputStream;
import java.util.List;

public class HomeView extends ScrollPane {

    private final MainViewModel viewModel;
    private final VBox content;

    // Random Song UI Components
    private Song currentRandomSong;
    private ImageView randomArtView;
    private StackPane randomTitleContainer;
    private StackPane randomArtistContainer;
    private StackPane randomArtContainer;

    public HomeView(MainViewModel viewModel, java.util.function.Consumer<String> onSectionChange,
            java.util.function.Consumer<String> onAlbumSelect) {
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

        // 1. Dynamic Greeting Header
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label greetingLabel = new Label();
        greetingLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        updateGreeting(greetingLabel);
        headerBox.getChildren().add(greetingLabel);
        content.getChildren().add(headerBox);

        // Removed Queue and Audio Quality Section from here

        // 2. Two-Column Layout (Daily Special in 1st column, Next in Queue in 2nd
        // column)
        HBox twoColumnGrid = createTwoColumnLayout();
        content.getChildren().add(twoColumnGrid);

        // 3. Recently Played Section
        VBox recentlyPlayed = createRecentlyPlayedSection();
        content.getChildren().add(recentlyPlayed);

        // 4. Favorites Section
        VBox favoritesSection = createFavoritesSection();
        content.getChildren().add(favoritesSection);

        // 5. Top Albums Section
        VBox topAlbums = createTopAlbumsSection(onAlbumSelect);
        content.getChildren().add(topAlbums);

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

        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> albumMap = new java.util.HashMap<>();
        for (Song s : songs) {
            String album = s.getAlbum();
            if (album != null && !album.isEmpty()) {
                albumMap.computeIfAbsent(album, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (albumMap.isEmpty()) {
            cardsRow.getChildren().addAll(
                    createPlaceholderAlbumCard("Abbey Road", "The Beatles"),
                    createPlaceholderAlbumCard("Dark Side of the Moon", "Pink Floyd"),
                    createPlaceholderAlbumCard("Thriller", "Michael Jackson"),
                    createPlaceholderAlbumCard("Back in Black", "AC/DC"),
                    createPlaceholderAlbumCard("Random Access Memories", "Daft Punk"));
        } else {
            List<String> albums = new java.util.ArrayList<>(albumMap.keySet());
            java.util.Collections.shuffle(albums);
            int count = Math.min(15, albums.size());
            for (int i = 0; i < count; i++) {
                String albumName = albums.get(i);
                List<Song> albumSongs = albumMap.get(albumName);
                Song representative = albumSongs.get(0);
                byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(representative.getPath());

                cardsRow.getChildren().add(createAlbumCard(albumName, representative.getArtist(), art, () -> {
                    if (onAlbumSelect != null) {
                        onAlbumSelect.accept(albumName);
                    }
                }));
            }
        }

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

    private Pane createAlbumCard(String title, String artist, byte[] artBytes, Runnable onClick) {
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

        if (artBytes != null) {
            artView.setImage(new Image(new ByteArrayInputStream(artBytes), 150, 150, true, true));
        } else {
            Label placeholder = new Label("💿");
            placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
            artContainer.getChildren().add(placeholder);
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
            byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            container.getChildren().add(createRecentSongCard(song, art));
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
            byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            container.getChildren().add(createRecentSongCard(song, art));
        }
    }

    private Pane createRecentSongCard(Song song, byte[] artBytes) {
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

        if (artBytes != null) {
            artView.setImage(new Image(new ByteArrayInputStream(artBytes), 150, 150, true, true));
        } else {
            Label placeholder = new Label("🎵");
            placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.15);");
            artContainer.getChildren().add(placeholder);
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

    private Pane createSongGridCell(Song song, byte[] artBytes) {
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10, 14, 10, 14));
        cell.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8; -fx-cursor: hand;");

        ImageView artView = new ImageView();
        artView.setFitWidth(56);
        artView.setFitHeight(56);
        artView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(56, 56);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        artView.setClip(clip);

        if (artBytes != null) {
            artView.setImage(new Image(new ByteArrayInputStream(artBytes)));
        } else {
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(56, 56);
            placeholder.setBackground(
                    new Background(new BackgroundFill(Color.web("#2c2c2c"), new CornerRadii(8), Insets.EMPTY)));
            Label musicSign = new Label("♫");
            musicSign.setStyle("-fx-text-fill: rgba(255,255,255,0.2); -fx-font-size: 24px;");
            placeholder.getChildren().add(musicSign);
            cell.getChildren().add(placeholder);
        }
        if (artBytes != null) {
            cell.getChildren().add(artView);
        }

        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(260);

        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 12px;");
        artistLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        artistLabel.setMaxWidth(260);

        textBox.getChildren().addAll(titleLabel, artistLabel);

        Button menuBtn = new Button("•••");
        menuBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 5 0 5;");
        menuBtn.setOnAction(e -> {
            e.consume();
            MenuUtils.showSongContextMenu(menuBtn, song, viewModel);
        });

        cell.getChildren().addAll(textBox, menuBtn);

        cell.setOnMouseEntered(e -> cell
                .setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8; -fx-cursor: hand;"));
        cell.setOnMouseExited(e -> cell
                .setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8; -fx-cursor: hand;"));
        cell.setOnMouseClicked(e -> viewModel.play(song));

        return cell;
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

        byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
        artContainer.getChildren().clear();
        if (artBytes != null) {
            ImageView artView = new ImageView(new Image(new ByteArrayInputStream(artBytes), 300, 300, true, true));
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 300);
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            artView.setClip(clip);
            artContainer.getChildren().add(artView);
        } else {
            Label placeholder = new Label("🎵");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 40px;");
            artContainer.getChildren().add(placeholder);
        }

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
}