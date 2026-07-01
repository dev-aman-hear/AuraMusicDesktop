package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.viewmodel.MainViewModel;
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
import javafx.scene.shape.Rectangle;

import java.io.ByteArrayInputStream;
import java.util.List;

public class NewView extends ScrollPane {

    private final MainViewModel viewModel;
    private final VBox content;

    public NewView(MainViewModel viewModel, java.util.function.Consumer<String> onGenreSelect,
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

        // 1. Top Artists Section
        VBox topArtists = createTopArtistsSection(onArtistSelect);
        content.getChildren().add(topArtists);

        // 2. Best New Songs Section
        VBox bestNewSongs = createBestNewSongsSection();
        content.getChildren().add(bestNewSongs);

        // 3. Trendy Gener Section
        VBox TrendyGener = createTrendyGenerSection(onGenreSelect);
        content.getChildren().add(TrendyGener);

        setContent(content);
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

        // Column constraints for 3 columns
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }

        List<Song> songs = viewModel.getLibrarySongs();
        int index = 0;
        for (int i = 0; i < Math.min(15, songs.size()); i++) {
            Song song = songs.get(i);
            Pane songRow = createSongRowItem(song);

            int col = index % 3;
            int row = index / 3;
            grid.add(songRow, col, row);
            index++;
        }

        section.getChildren().addAll(headerRow, grid);
        return section;
    }

    private Pane createSongRowItem(Song song) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 6 0; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        // Artwork
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

        byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(song.getPath());
        if (artBytes != null) {
            artView.setImage(new Image(new ByteArrayInputStream(artBytes), 44, 44, true, true));
        } else {
            Label placeholder = new Label("🎵");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.2);");
            artContainer.getChildren().add(placeholder);
        }
        artContainer.getChildren().add(artView);

        // Metadata
        VBox textCol = new VBox(2);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");

        textCol.getChildren().addAll(titleLabel, artistLabel);

        // Options Button
        Button optionsBtn = new Button("•••");
        optionsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ff2d55; -fx-font-size: 11px; -fx-cursor: hand;");

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

        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> artistMap = new java.util.HashMap<>();
        for (Song s : songs) {
            String artist = s.getArtist();
            if (artist != null && !artist.isEmpty()) {
                artistMap.computeIfAbsent(artist, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (artistMap.isEmpty()) {
            cardsRow.getChildren().addAll(
                    createSquareCard("Lata Mangeshkar", "Artist", null, null),
                    createSquareCard("Arijit Singh", "Artist", null, null),
                    createSquareCard("Ed Sheeran", "Artist", null, null),
                    createSquareCard("Coldplay", "Artist", null, null),
                    createSquareCard("Taylor Swift", "Artist", null, null));
        } else {
            List<String> artists = new java.util.ArrayList<>(artistMap.keySet());
            java.util.Collections.shuffle(artists);
            int count = Math.min(15, artists.size());
            for (int i = 0; i < count; i++) {
                String artistName = artists.get(i);
                List<Song> artistSongs = artistMap.get(artistName);
                Song representative = artistSongs.get(0);
                byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(representative.getPath());

                cardsRow.getChildren().add(createSquareCard(artistName, "Artist", art, () -> {
                    if (onArtistSelect != null) {
                        onArtistSelect.accept(artistName);
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

    private VBox createTrendyGenerSection(java.util.function.Consumer<String> onGenreSelect) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: transparent;");

        Label sectionTitle = new Label("Trendy Genres");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox cardsRow = new HBox(20);
        cardsRow.setStyle("-fx-background-color: transparent;");

        List<Song> songs = viewModel.getLibrarySongs();
        java.util.Map<String, List<Song>> genreMap = new java.util.HashMap<>();
        for (Song s : songs) {
            String genre = s.getGenre();
            if (genre != null && !genre.isEmpty()) {
                genreMap.computeIfAbsent(genre, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        if (genreMap.isEmpty()) {
            cardsRow.getChildren().addAll(
                    createSquareCard("Pop", "Genre", null, null),
                    createSquareCard("Rock", "Genre", null, null),
                    createSquareCard("Hip-Hop", "Genre", null, null),
                    createSquareCard("Classical", "Genre", null, null),
                    createSquareCard("Jazz", "Genre", null, null));
        } else {
            List<String> genres = new java.util.ArrayList<>(genreMap.keySet());
            java.util.Collections.shuffle(genres);
            int count = Math.min(15, genres.size());
            for (int i = 0; i < count; i++) {
                String genreName = genres.get(i);
                List<Song> genreSongs = genreMap.get(genreName);
                Song representative = genreSongs.get(0);
                byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(representative.getPath());

                cardsRow.getChildren().add(createSquareCard(genreName, "Genre", art, () -> {
                    if (onGenreSelect != null) {
                        onGenreSelect.accept(genreName);
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

    private Pane createSquareCard(Object genername2, String subtitle, byte[] artBytes, Runnable onClick) {
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
}
