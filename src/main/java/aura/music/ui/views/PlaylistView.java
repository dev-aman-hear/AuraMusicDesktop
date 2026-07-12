package aura.music.ui.views;

import aura.music.library.LibraryManager;
import aura.music.model.Playlist;
import aura.music.model.Song;
import aura.music.ui.components.MenuUtils;
import aura.music.ui.components.SVGIcons;
import aura.music.viewmodel.MainViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaylistView extends HBox {

    private final MainViewModel viewModel;
    private final LibraryManager libraryManager = LibraryManager.getInstance();

    private ListView<Playlist> playlistListView;
    private VBox detailContainer;

    private Label playlistTitleLabel;
    private Label playlistInfoLabel;
    private ImageView playlistArtView;
    private VBox playlistSongsContainer;

    private Playlist selectedPlaylist;

    public PlaylistView(MainViewModel viewModel) {
        this.viewModel = viewModel;
        setSpacing(20);
        setPadding(new Insets(30, 40, 20, 40));
        setStyle("-fx-background-color: transparent;");

        // Left Panel: Playlist List
        VBox leftPanel = new VBox(15);
        leftPanel.setPrefWidth(220);
        leftPanel.setMinWidth(220);
        leftPanel.setStyle("-fx-background-color: transparent;");

        Label sidebarTitle = new Label("Playlists");
        sidebarTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button createPlaylistBtn = new Button("Create Playlist");
        createPlaylistBtn.setStyle(
                "-fx-background-color: #ff2d55; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;");
        createPlaylistBtn.setMaxWidth(Double.MAX_VALUE);
        createPlaylistBtn.setOnAction(e -> handleCreatePlaylist());

        playlistListView = new ListView<>();
        VBox.setVgrow(playlistListView, Priority.ALWAYS);
        playlistListView.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 8;");
        playlistListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    setStyle(
                            "-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 12; -fx-background-color: transparent;");
                    // Highlight selected item via CSS or inline style
                    selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        if (isSelected) {
                            setStyle(
                                    "-fx-text-fill: #ff2d55; -fx-font-size: 14px; -fx-padding: 8 12; -fx-background-color: rgba(255,255,255,0.05);");
                        } else {
                            setStyle(
                                    "-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 12; -fx-background-color: transparent;");
                        }
                    });
                }
            }
        });

        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showPlaylistDetail(newVal);
            }
        });

        leftPanel.getChildren().addAll(sidebarTitle, createPlaylistBtn, playlistListView);

        // Right Panel: Playlist Details
        detailContainer = new VBox(25);
        HBox.setHgrow(detailContainer, Priority.ALWAYS);
        detailContainer.setStyle("-fx-background-color: transparent;");
        detailContainer.setVisible(false);

        // Header of detail
        HBox detailHeader = new HBox(30);
        detailHeader.setAlignment(Pos.TOP_LEFT);

        playlistArtView = new ImageView();
        playlistArtView.setFitWidth(150);
        playlistArtView.setFitHeight(150);
        playlistArtView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(150, 150);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        playlistArtView.setClip(clip);

        StackPane artWrapper = new StackPane(playlistArtView);
        artWrapper.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12px;");
        artWrapper.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.3)));

        VBox metaCol = new VBox(8);
        metaCol.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(metaCol, Priority.ALWAYS);

        playlistTitleLabel = new Label("Playlist Name");
        playlistTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: white;");

        playlistInfoLabel = new Label("0 tracks");
        playlistInfoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.4);");

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button playBtn = new Button("Play");
        playBtn.setGraphic(SVGIcons.createPlayIcon(12, Color.WHITE));
        playBtn.setStyle(
                "-fx-background-color: #ff2d55; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-size: 13px;");
        playBtn.setOnAction(e -> {
            if (selectedPlaylist != null && !selectedPlaylist.getSongs().isEmpty()) {
                viewModel.getQueue().setAll(selectedPlaylist.getSongs());
                viewModel.playQueueIndex(0);
            }
        });

        Button renameBtn = new Button("Rename");
        renameBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 6;");
        renameBtn.setOnAction(e -> handleRenamePlaylist());

        Button addSongsBtn = new Button("Add Songs");
        addSongsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 6;");
        addSongsBtn.setOnAction(e -> handleAddSongs());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: rgba(255,50,50,0.15); -fx-text-fill: #ff3b30; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: rgba(255,50,50,0.25); -fx-border-radius: 6;");
        deleteBtn.setOnAction(e -> handleDeletePlaylist());

        btnRow.getChildren().addAll(playBtn, renameBtn, addSongsBtn, deleteBtn);
        metaCol.getChildren().addAll(playlistTitleLabel, playlistInfoLabel, btnRow);
        detailHeader.getChildren().addAll(artWrapper, metaCol);

        ScrollPane songsScroll = new ScrollPane();
        songsScroll.setFitToWidth(true);
        songsScroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        playlistSongsContainer = new VBox(2);
        songsScroll.setContent(playlistSongsContainer);
        VBox.setVgrow(songsScroll, Priority.ALWAYS);

        detailContainer.getChildren().addAll(detailHeader, songsScroll);

        getChildren().addAll(leftPanel, detailContainer);

        refreshPlaylists();
    }

    public void refreshPlaylists() {
        List<Playlist> lists = libraryManager.getPlaylists();
        playlistListView.getItems().setAll(lists);
        if (selectedPlaylist != null) {
            Optional<Playlist> match = lists.stream().filter(p -> p.getId().equals(selectedPlaylist.getId()))
                    .findFirst();
            if (match.isPresent()) {
                playlistListView.getSelectionModel().select(match.get());
            } else {
                detailContainer.setVisible(false);
                selectedPlaylist = null;
            }
        }
    }

    private void showPlaylistDetail(Playlist playlist) {
        selectedPlaylist = playlist;
        detailContainer.setVisible(true);
        playlistTitleLabel.setText(playlist.getName());
        playlistInfoLabel.setText(playlist.getSongs().size() + " songs");

        // Display artwork if we have songs
        if (!playlist.getSongs().isEmpty()) {
            byte[] art = aura.music.library.MetadataExtractor.extractArtworkBytes(playlist.getSongs().get(0).getPath());
            if (art != null) {
                playlistArtView.setImage(new Image(new ByteArrayInputStream(art), 150, 150, true, true));
            } else {
                playlistArtView.setImage(null);
            }
        } else {
            playlistArtView.setImage(null);
        }

        playlistSongsContainer.getChildren().clear();
        int index = 1;
        for (Song song : playlist.getSongs()) {
            HBox songRow = createSongRow(index++, song);
            playlistSongsContainer.getChildren().add(songRow);
        }
    }

    private HBox createSongRow(int index, Song song) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 15, 8, 15));
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.4); -fx-pref-width: 25;");

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

        Button optionsBtn = new Button("•••");
        optionsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 5;");
        optionsBtn.setOnAction(e -> {
            e.consume();
            MenuUtils.showSongContextMenu(optionsBtn, song, viewModel);
        });

        row.getChildren().addAll(indexLabel, titleCol, durationLabel, optionsBtn);

        row.setOnMouseEntered(e -> row
                .setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseExited(
                e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (selectedPlaylist != null) {
                    List<Song> playlistSongs = selectedPlaylist.getSongs();
                    int idx = playlistSongs.indexOf(song);
                    if (idx != -1) {
                        viewModel.getQueue().setAll(playlistSongs);
                        viewModel.playQueueIndex(idx);
                    } else {
                        viewModel.play(song);
                    }
                } else {
                    viewModel.play(song);
                }
            }
        });

        return row;
    }

    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog("New Playlist");
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter playlist name:");
        dialog.setContentText("Name:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Playlist pl = libraryManager.createPlaylist(name.trim());
                refreshPlaylists();
                playlistListView.getSelectionModel().select(pl);
            }
        });
    }

    private void handleRenamePlaylist() {
        if (selectedPlaylist == null)
            return;
        TextInputDialog dialog = new TextInputDialog(selectedPlaylist.getName());
        dialog.setTitle("Rename Playlist");
        dialog.setHeaderText("Enter new playlist name:");
        dialog.setContentText("Name:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                selectedPlaylist.setName(name.trim());
                libraryManager.savePlaylists();
                refreshPlaylists();
            }
        });
    }

    private void handleDeletePlaylist() {
        if (selectedPlaylist == null)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Playlist");
        alert.setHeaderText("Are you sure you want to delete this playlist?");
        alert.setContentText(selectedPlaylist.getName());
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryManager.deletePlaylist(selectedPlaylist);
            selectedPlaylist = null;
            refreshPlaylists();
        }
    }

    private void handleAddSongs() {
        if (selectedPlaylist == null)
            return;

        // Custom beautiful dialog to select songs
        Dialog<List<Song>> dialog = new Dialog<>();
        dialog.setTitle("Add Songs");
        dialog.setHeaderText("Select songs to add to " + selectedPlaylist.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/aura/music/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        ButtonType addType = new ButtonType("Add Selected", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPrefSize(400, 300);

        TextField searchField = new TextField();
        searchField.setPromptText("Search songs...");
        searchField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 6;");

        ListView<SongSelectionWrapper> songList = new ListView<>();
        songList.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 6;");
        VBox.setVgrow(songList, Priority.ALWAYS);

        List<SongSelectionWrapper> allWrappers = new ArrayList<>();
        for (Song song : viewModel.getLibrarySongs()) {
            allWrappers.add(new SongSelectionWrapper(song));
        }
        songList.getItems().setAll(allWrappers);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal.toLowerCase();
            List<SongSelectionWrapper> filtered = new ArrayList<>();
            for (SongSelectionWrapper w : allWrappers) {
                if (w.song.getTitle().toLowerCase().contains(q) || w.song.getArtist().toLowerCase().contains(q)) {
                    filtered.add(w);
                }
            }
            songList.getItems().setAll(filtered);
        });

        songList.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(SongSelectionWrapper item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item.selected);
                    checkBox.textProperty().bind(
                            javafx.beans.binding.Bindings.concat(item.song.getTitle(), " - ", item.song.getArtist()));
                    checkBox.setStyle("-fx-text-fill: white;");
                    checkBox.setOnAction(e -> item.selected = checkBox.isSelected());
                    setGraphic(checkBox);
                }
            }
        });

        content.getChildren().addAll(searchField, songList);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == addType) {
                List<Song> selected = new ArrayList<>();
                for (SongSelectionWrapper w : allWrappers) {
                    if (w.selected)
                        selected.add(w.song);
                }
                return selected;
            }
            return null;
        });

        Optional<List<Song>> chosenSongs = dialog.showAndWait();
        chosenSongs.ifPresent(songs -> {
            for (Song s : songs) {
                selectedPlaylist.addSong(s);
            }
            libraryManager.savePlaylists();
            showPlaylistDetail(selectedPlaylist);
        });
    }

    private static class SongSelectionWrapper {
        final Song song;
        boolean selected = false;

        SongSelectionWrapper(Song song) {
            this.song = song;
        }
    }
}
