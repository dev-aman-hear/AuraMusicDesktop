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

        // 2. Two-Column Layout (Random Song in 1st column, Favorite Songs in 2nd
        // column)
        GridPane twoColumnGrid = createTwoColumnLayout();
        content.getChildren().add(twoColumnGrid);

        // 3. Recently Played Section
        VBox recentlyPlayed = createRecentlyPlayedSection();
        content.getChildren().add(recentlyPlayed);

        // 4. Top Albums Section
        VBox topAlbums = createTopAlbumsSection(onAlbumSelect);
        content.getChildren().add(topAlbums);

        setContent(content);
    }

    private GridPane createTwoColumnLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(35);
        grid.setVgap(20);
        grid.setStyle("-fx-background-color: transparent;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        VBox randomSongCol = createRandomSongColumn();
        VBox favoritesCol = createFavoritesColumn();

        grid.add(randomSongCol, 0, 0);
        grid.add(favoritesCol, 1, 0);

        return grid;
    }

    private VBox createRandomSongColumn() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Daily Special");
        sectionTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button rollBtn = new Button("🎲");
        rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;");
        rollBtn.setOnMouseEntered(e -> rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;"));
        rollBtn.setOnMouseExited(e -> rollBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;"));
        rollBtn.setOnMouseClicked(e -> pickNewRandomSong());

        headerRow.getChildren().addAll(sectionTitle, spacer, rollBtn);

        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        card.setPrefHeight(390);
        card.setMinHeight(390);
        card.setMaxHeight(390);
        card.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("rgba(255, 255, 255, 0.08)")),
                        new Stop(1, Color.web("rgba(255, 255, 255, 0.02)"))),
                new CornerRadii(16), Insets.EMPTY)));
        card.setStyle("-fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 16; -fx-border-width: 1;");

        DropShadow cardShadow = new DropShadow(20, Color.rgb(0, 0, 0, 0.4));
        card.setEffect(cardShadow);

        randomArtContainer = new StackPane();
        randomArtContainer.setPrefSize(240, 240);
        randomArtContainer.setMinSize(240, 240);
        randomArtContainer.setMaxSize(240, 240);
        randomArtContainer.setCursor(javafx.scene.Cursor.HAND);
        randomArtContainer.setBackground(
                new Background(new BackgroundFill(Color.web("#2c2c2c"), new CornerRadii(12), Insets.EMPTY)));
        randomArtContainer.setOnMouseClicked(e -> {
            if (currentRandomSong != null) {
                viewModel.play(currentRandomSong);
            }
        });

        randomArtView = new ImageView();
        randomArtView.setFitWidth(240);
        randomArtView.setFitHeight(240);
        randomArtView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(240, 240);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        randomArtView.setClip(clip);

        DropShadow artShadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.5));
        randomArtContainer.setEffect(artShadow);

        VBox details = new VBox(6);
        details.setAlignment(Pos.CENTER);

        randomTitleContainer = new StackPane();
        randomTitleContainer.setAlignment(Pos.CENTER);
        randomArtistContainer = new StackPane();
        randomArtistContainer.setAlignment(Pos.CENTER);

        details.getChildren().addAll(randomTitleContainer, randomArtistContainer);

        card.getChildren().addAll(randomArtContainer, details);
        section.getChildren().addAll(headerRow, card);

        pickNewRandomSong();

        return section;
    }

    private void pickNewRandomSong() {
        List<Song> songs = viewModel.getLibrarySongs();
        if (songs.isEmpty()) {
            setRandomSong(null);
        } else {
            java.util.Random rand = new java.util.Random();
            Song song = songs.get(rand.nextInt(songs.size()));
            setRandomSong(song);
        }
    }

    private void setRandomSong(Song song) {
        this.currentRandomSong = song;
        if (song == null) {
            Label noSongsLabel = new Label("No Songs Available");
            noSongsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            randomTitleContainer.getChildren().setAll(noSongsLabel);

            Label addMusicLabel = new Label("Add music to your library");
            addMusicLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");
            randomArtistContainer.getChildren().setAll(addMusicLabel);

            randomArtView.setImage(null);
            randomArtContainer.getChildren().clear();
            Label placeholder = new Label("♫");
            placeholder.setStyle("-fx-font-size: 64px; -fx-text-fill: rgba(255,255,255,0.15);");
            randomArtContainer.getChildren().add(placeholder);
            return;
        }

        randomTitleContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getTitle(),
                        "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;", 240));
        randomArtistContainer.getChildren().setAll(
                aura.music.ui.MarqueeUtils.createMarqueeLabel(song.getArtist(),
                        "-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;", 240));

        byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
        randomArtContainer.getChildren().clear();
        if (artBytes != null) {
            Image img = new Image(new ByteArrayInputStream(artBytes));
            randomArtView.setImage(img);
            randomArtContainer.getChildren().add(randomArtView);
        } else {
            Label placeholder = new Label("♫");
            placeholder.setStyle("-fx-font-size: 64px; -fx-text-fill: rgba(255,255,255,0.15);");
            randomArtContainer.getChildren().add(placeholder);
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

    private VBox createFavoritesColumn() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Favorite Songs");
        sectionTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox listContainer = new VBox(12);
        listContainer.setStyle("-fx-background-color: transparent;");

        rebuildFavorites(listContainer);

        viewModel.getLibrarySongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            Platform.runLater(() -> rebuildFavorites(listContainer));
        });

        viewModel.favoritesVersionProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> rebuildFavorites(listContainer));
        });

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefHeight(390);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        section.getChildren().addAll(sectionTitle, scroll);
        return section;
    }

    private void rebuildFavorites(VBox listContainer) {
        listContainer.getChildren().clear();
        List<Song> favs = new java.util.ArrayList<>();
        for (Song s : viewModel.getLibrarySongs()) {
            if (s.isFavorite()) {
                favs.add(s);
            }
        }

        if (favs.isEmpty()) {
            Label emptyLabel = new Label("No favorites yet");
            emptyLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 13px; -fx-font-style: italic;");
            listContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Song song : favs) {
            byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
            listContainer.getChildren().add(createSongGridCell(song, art));
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
}