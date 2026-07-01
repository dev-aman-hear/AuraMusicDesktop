package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.ByteArrayInputStream;
import java.util.List;

public class AlbumDetailView extends ScrollPane {

    private final MainViewModel viewModel;
    private final ImageView artworkView;
    private final Label titleLabel;
    private final Label artistLabel;
    private final Label metadataLabel;
    private final Label descriptionLabel;
    private final VBox tracksContainer;
    private final Label footerLabel;
    private List<Song> albumSongs;

    public AlbumDetailView(MainViewModel viewModel) {
        this.viewModel = viewModel;

        setFitToWidth(true);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        VBox mainLayout = new VBox(35);
        mainLayout.setPadding(new Insets(30, 40, 40, 40));
        mainLayout.setStyle("-fx-background-color: transparent;");

        // --- TOP SECTION (Artwork + Meta) ---
        HBox headerBox = new HBox(40);
        headerBox.setAlignment(Pos.TOP_LEFT);

        // Left: Artwork
        artworkView = new ImageView();
        artworkView.setFitWidth(260);
        artworkView.setFitHeight(260);
        artworkView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(260, 260);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        artworkView.setClip(clip);

        StackPane artContainer = new StackPane(artworkView);
        artContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16px; -fx-border-width: 1px;");
        artContainer.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.4)));

        // Right: Metadata details
        VBox metaColumn = new VBox(12);
        metaColumn.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(metaColumn, Priority.ALWAYS);

        titleLabel = new Label("Album Title");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: white;");
        titleLabel.setWrapText(true);

        artistLabel = new Label("Artist Name");
        artistLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #ff2d55;"); // Apple Music pink

        metadataLabel = new Label("Genre • Year • Quality");
        metadataLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4);");

        descriptionLabel = new Label("A premium curated set for this music album.");
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.6);");
        descriptionLabel.setWrapText(true);

        // Play / Shuffle Buttons + Options Row
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(10, 0, 0, 0));

        Button playBtn = new Button("Play");
        playBtn.setGraphic(SVGIcons.createPlayIcon(12, Color.WHITE));
        playBtn.setStyle(
                "-fx-background-color: #ff2d55; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22; -fx-cursor: hand; -fx-font-size: 13px;");
        playBtn.setOnAction(e -> {
            if (albumSongs != null && !albumSongs.isEmpty()) {
                viewModel.play(albumSongs.get(0));
            }
        });

        Button shuffleBtn = new Button("Shuffle");
        shuffleBtn.setGraphic(SVGIcons.createShuffleIcon(12, Color.web("#ff2d55")));
        shuffleBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #ff2d55; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 6;");
        shuffleBtn.setOnAction(e -> {
            if (albumSongs != null && !albumSongs.isEmpty()) {
                viewModel.getQueue().setAll(albumSongs);
                viewModel.shuffleModeProperty().set(true);
                int randomIdx = (int) (Math.random() * albumSongs.size());
                viewModel.playQueueIndex(randomIdx);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button downloadBtn = new Button("↓");
        downloadBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ff2d55; -fx-font-size: 20px; -fx-cursor: hand; -fx-font-weight: bold;");

        Button moreBtn = new Button("•••");
        moreBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ff2d55; -fx-font-size: 14px; -fx-cursor: hand;");

        actionRow.getChildren().addAll(playBtn, shuffleBtn, spacer, downloadBtn, moreBtn);

        metaColumn.getChildren().addAll(titleLabel, artistLabel, metadataLabel, descriptionLabel, actionRow);
        headerBox.getChildren().addAll(artContainer, metaColumn);

        // --- BOTTOM SECTION (Track List) ---
        tracksContainer = new VBox(2);
        tracksContainer.setStyle("-fx-background-color: transparent;");

        footerLabel = new Label("0 Items, 0 Minutes");
        footerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4); -fx-padding: 20 0 0 10;");

        mainLayout.getChildren().addAll(headerBox, tracksContainer, footerLabel);
        setContent(mainLayout);
    }

    /**
     * @param albumName
     * @param songs
     */
    public void setAlbum(String albumName, List<Song> songs) {
        this.albumSongs = songs;
        if (songs.isEmpty())
            return;

        Song firstSong = songs.get(0);
        titleLabel.setText(albumName);
        artistLabel.setText(firstSong.getArtist());
        metadataLabel.setText("Bollywood • 2026 • Hi-Res Lossless");
        descriptionLabel.setText("A transportive set for this music album.");

        byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(firstSong.getPath());
        if (artBytes != null) {
            artworkView.setImage(new Image(new ByteArrayInputStream(artBytes), 260, 260, true, true));
        } else {
            artworkView.setImage(null);
        }

        // Build Tracks List
        tracksContainer.getChildren().clear();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            HBox row = createTrackRow(i + 1, song);
            tracksContainer.getChildren().add(row);
        }

        // Footer info
        int totalSecs = 0;
        for (Song s : songs) {
            totalSecs += s.getDuration();
        }
        int mins = totalSecs / 60;
        // Calculate total size
        double totalSizeMB = 0;

        for (Song song : songs) {
            java.io.File file = new java.io.File(song.getPath());
            if (file.exists()) {
                totalSizeMB += (double) file.length() / (1024 * 1024);
            }
        }
        footerLabel
                .setText(songs.size() + " items, " + mins + " minutes, " + String.format("%.1f", totalSizeMB) + " MB");
    }

    public void setArtist(String artistName, List<Song> songs) {
        this.albumSongs = songs;
        titleLabel.setText(artistName);
        artistLabel.setText("Artist");
        metadataLabel.setText("Featured Artist • " + songs.size() + " tracks");
        descriptionLabel.setText("An immersive collection of tracks by " + artistName + ".");

        if (!songs.isEmpty()) {
            byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(songs.get(0).getPath());
            if (artBytes != null) {
                artworkView.setImage(new Image(new ByteArrayInputStream(artBytes), 260, 260, true, true));
            } else {
                artworkView.setImage(null);
            }
        } else {
            artworkView.setImage(null);
        }

        // Build Tracks List
        tracksContainer.getChildren().clear();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            HBox row = createTrackRow(i + 1, song);
            tracksContainer.getChildren().add(row);
        }

        // Footer info
        int totalSecs = 0;
        for (Song s : songs) {
            totalSecs += s.getDuration();
        }
        int mins = totalSecs / 60;
        footerLabel.setText(songs.size() + " items, " + mins + " minutes");
    }

    public void setPlaylist(String playlistName, List<Song> songs) {
        this.albumSongs = songs;
        titleLabel.setText(playlistName);
        artistLabel.setText("Playlist");
        metadataLabel.setText("Personal Playlist • " + songs.size() + " tracks");
        descriptionLabel.setText("Your custom playlist of favorite tunes.");

        if (!songs.isEmpty()) {
            byte[] artBytes = aura.music.library.MetadataExtractor.extractArtworkBytes(songs.get(0).getPath());
            if (artBytes != null) {
                artworkView.setImage(new Image(new ByteArrayInputStream(artBytes), 260, 260, true, true));
            } else {
                artworkView.setImage(null);
            }
        } else {
            artworkView.setImage(null);
        }

        // Build Tracks List
        tracksContainer.getChildren().clear();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            HBox row = createTrackRow(i + 1, song);
            tracksContainer.getChildren().add(row);
        }

        // Footer info
        int totalSecs = 0;
        for (Song s : songs) {
            totalSecs += s.getDuration();
        }
        int mins = totalSecs / 60;
        footerLabel.setText(songs.size() + " items, " + mins + " minutes");
    }

    private HBox createTrackRow(int index, Song song) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 15, 8, 15));
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        // Star + Index container
        HBox indexContainer = new HBox(6);
        indexContainer.setAlignment(Pos.CENTER_RIGHT);
        indexContainer.setPrefWidth(35);
        indexContainer.setMinWidth(35);
        indexContainer.setMaxWidth(35);

        javafx.scene.Node starNode;
        if (song.isFavorite()) {
            starNode = SVGIcons.createStarIcon(10, Color.web("#ff2d55"), true);
        } else {
            Region spacer = new Region();
            spacer.setPrefSize(10, 10);
            starNode = spacer;
        }

        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.4);");

        indexContainer.getChildren().addAll(starNode, indexLabel);

        VBox titleCol = new VBox(3);
        Label titleText = new Label(song.getTitle());
        titleText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: white;");

        Label artistText = new Label(song.getArtist());
        artistText.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.5);");
        titleCol.getChildren().addAll(titleText, artistText);
        HBox.setHgrow(titleCol, Priority.ALWAYS);

        int minutes = (int) (song.getDuration() / 60);
        int secs = (int) (song.getDuration() % 60);
        Label durationLabel = new Label(String.format("%d:%02d", minutes, secs));
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.5);");

        Button menuBtn = new Button("•••");
        menuBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 10px; -fx-cursor: hand;");

        row.getChildren().addAll(indexContainer, titleCol, durationLabel, menuBtn);

        // Hover styles
        row.setOnMouseEntered(e -> row
                .setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseExited(
                e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));

        // Double click to play
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                viewModel.play(song);
            }
        });

        return row;
    }
}
