package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.online.OnlineMusicService;
import aura.music.online.OnlineTrack;
import aura.music.online.YoutubeMusicService;
import aura.music.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

/** Online discovery page. The provider exposes only short, playable previews. */
public class OnlineMusicView extends BorderPane {
    private final MainViewModel viewModel;
    private final ObservableList<OnlineTrack> tracks = FXCollections.observableArrayList();
    private final ListView<OnlineTrack> results = new ListView<>(tracks);
    private final TextField search = new TextField();
    private final Label subtitle = new Label("Loading trending previews…");

    public OnlineMusicView(MainViewModel viewModel) {
        this.viewModel = viewModel;
        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(38, 60, 50, 60));

        Label title = new Label("Online");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: white;");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.55);");
        VBox heading = new VBox(5, title, subtitle);

        search.setPromptText("Search songs, artists, or albums");
        search.setPrefHeight(44);
        search.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.48); -fx-background-radius: 10; -fx-font-size: 14px;");
        search.setOnAction(e -> load(search.getText()));
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #ff2d55; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        searchButton.setOnAction(e -> load(search.getText()));
        HBox searchRow = new HBox(10, search, searchButton);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox top = new VBox(24, heading, searchRow);
        setTop(top);
        BorderPane.setMargin(top, new Insets(0, 0, 22, 0));

        results.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        results.setPlaceholder(new Label("No previews found. Check your connection and try a search."));
        results.setCellFactory(list -> new TrackCell(viewModel));
        setCenter(results);
        load("new music");
    }

    public void refreshTrending() { load("new music"); }

    private void load(String query) {
        subtitle.setText(query == null || query.isBlank() ? "Loading trending previews…" : "Searching online music previews…");
        tracks.clear();
        String youtubeApiKey = viewModel.youtubeApiKeyProperty().get();
        if (youtubeApiKey != null && !youtubeApiKey.isBlank()) {
            YoutubeMusicService.search(query, youtubeApiKey).thenAccept(found -> Platform.runLater(() -> {
                tracks.setAll(found);
                subtitle.setText(found.isEmpty() ? "No YouTube music results found." : "YouTube Music results - local files play first when matched");
            }));
            return;
        }
        OnlineMusicService.search(query).thenAccept(found -> Platform.runLater(() -> {
            tracks.setAll(found);
            subtitle.setText(found.isEmpty() ? "No previews available — check your connection." : "Trending and new music · 30-second previews");
        }));
    }

    private static class TrackCell extends ListCell<OnlineTrack> {
        private final ImageView artwork = new ImageView();
        private final Label title = new Label();
        private final Label artist = new Label();
        private final Label album = new Label();
        private final Button play = new Button("Play preview");
        private final HBox row;
        private final MainViewModel viewModel;

        TrackCell(MainViewModel viewModel) {
            this.viewModel = viewModel;
            artwork.setFitWidth(56); artwork.setFitHeight(56); artwork.setPreserveRatio(true);
            Rectangle clip = new Rectangle(56, 56); clip.setArcWidth(10); clip.setArcHeight(10); artwork.setClip(clip);
            title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
            artist.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 13px;");
            album.setStyle("-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 12px;");
            VBox text = new VBox(3, title, artist, album); HBox.setHgrow(text, Priority.ALWAYS);
            play.setStyle("-fx-background-color: rgba(255,45,85,0.92); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 16; -fx-cursor: hand;");
            play.setOnAction(e -> {
                OnlineTrack track = getItem();
                if (track == null) return;
                Song localTrack = viewModel.findLocalTrack(track.title(), track.artist());
                if (localTrack != null) {
                    viewModel.play(localTrack);
                    return;
                }
                if (track.source() == OnlineTrack.Source.YOUTUBE) {
                    YoutubePlayerWindow.play(track.youtubeVideoId(), track.title(), track.artist(),
                            highResolutionArtwork(track.artworkUrl()), viewModel);
                    return;
                }
                Song song = new Song(track.previewUrl());
                song.setTitle(track.title()); song.setArtist(track.artist()); song.setAlbum(track.album());
                song.setDuration(track.durationMillis() / 1000.0);
                song.setArtworkUrl(highResolutionArtwork(track.artworkUrl()));
                viewModel.play(song);
            });
            row = new HBox(14, artwork, text, play); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(9, 8, 9, 8));
        }
        @Override protected void updateItem(OnlineTrack item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); return; }
            title.setText(item.title()); artist.setText(item.artist()); album.setText(item.album());
            artwork.setImage(item.artworkUrl().isBlank() ? null : new Image(highResolutionArtwork(item.artworkUrl()), 56, 56, true, true, true));
            Song localTrack = viewModel.findLocalTrack(item.title(), item.artist());
            if (localTrack != null) {
                play.setText("Play local");
                play.setTooltip(new Tooltip("Full local file found in your library"));
            } else if (item.source() == OnlineTrack.Source.YOUTUBE) {
                play.setText("Play YouTube");
                play.setTooltip(new Tooltip("Play in the official embedded YouTube player"));
            } else {
                play.setText("Play preview");
                play.setTooltip(new Tooltip("Play the 30-second online preview"));
            }
            setGraphic(row);
        }

        private static String highResolutionArtwork(String url) {
            return url == null ? "" : url.replace("100x100bb", "600x600bb");
        }

    }
}
