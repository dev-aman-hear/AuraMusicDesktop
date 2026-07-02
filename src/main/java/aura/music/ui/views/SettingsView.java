package aura.music.ui.views;

import aura.music.library.LibraryManager;

import aura.music.theme.ThemeEngine;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView extends ScrollPane {

    private final ThemeEngine themeEngine = ThemeEngine.getInstance();
    private final Runnable onPlaylistsChanged;

    public SettingsView(BooleanProperty albumsGrid, BooleanProperty artistsGrid, BooleanProperty genresGrid, Runnable onPlaylistsChanged) {
        this.onPlaylistsChanged = onPlaylistsChanged;
        setFitToWidth(true);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 40, 40, 40));
        content.setStyle("-fx-background-color: transparent;");

        // Header Title
        Label titleLabel = new Label("Settings");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(titleLabel);

        // --- LAYOUT SECTION ---
        VBox layoutCard = createSectionCard("Layout Options");
        GridPane layoutGrid = new GridPane();
        layoutGrid.setHgap(30);
        layoutGrid.setVgap(15);

        Label albumsLabel = createSettingLabel("Albums Section Layout");
        ComboBox<String> albumsCombo = createLayoutCombo(albumsGrid);

        Label artistsLabel = createSettingLabel("Artists Section Layout");
        ComboBox<String> artistsCombo = createLayoutCombo(artistsGrid);

        Label genresLabel = createSettingLabel("Genres Section Layout");
        ComboBox<String> genresCombo = createLayoutCombo(genresGrid);

        layoutGrid.add(albumsLabel, 0, 0);
        layoutGrid.add(albumsCombo, 1, 0);
        layoutGrid.add(artistsLabel, 0, 1);
        layoutGrid.add(artistsCombo, 1, 1);
        layoutGrid.add(genresLabel, 0, 2);
        layoutGrid.add(genresCombo, 1, 2);

        layoutCard.getChildren().add(layoutGrid);
        content.getChildren().add(layoutCard);

        // --- THEME SECTION ---
        VBox themeCard = createSectionCard("Theme Options");
        VBox themeContent = new VBox(15);

        // Dynamic Accent Toggle Row
        HBox dynamicRow = new HBox(20);
        dynamicRow.setAlignment(Pos.CENTER_LEFT);
        Label dynamicLabel = createSettingLabel("Dynamic Accent Coloring");
        Label dynamicDesc = new Label("Extract accent colors automatically from playing song artwork");
        dynamicDesc.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 11px;");
        VBox dynamicTextCol = new VBox(2, dynamicLabel, dynamicDesc);
        HBox.setHgrow(dynamicTextCol, Priority.ALWAYS);

        Button dynamicToggle = new Button();
        updateToggleButtonState(dynamicToggle, themeEngine.dynamicColoringProperty().get());

        // Light/Dark Mode Row (Only active when dynamic coloring is off)
        HBox modeRow = new HBox(20);
        modeRow.setAlignment(Pos.CENTER_LEFT);
        Label modeLabel = createSettingLabel("Base App Theme");
        Label modeDesc = new Label("Choose Light or Dark theme manually when dynamic accent is disabled");
        modeDesc.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 11px;");
        VBox modeTextCol = new VBox(2, modeLabel, modeDesc);
        HBox.setHgrow(modeTextCol, Priority.ALWAYS);

        Button modeToggle = new Button();
        updateModeButtonState(modeToggle, themeEngine.lightModeProperty().get());

        // Toggles action
        dynamicToggle.setOnAction(e -> {
            boolean active = !themeEngine.dynamicColoringProperty().get();
            themeEngine.dynamicColoringProperty().set(active);
            updateToggleButtonState(dynamicToggle, active);
            modeToggle.setDisable(active);
            if (!active) {
                themeEngine.applyDefaultStaticTheme();
            }
        });

        modeToggle.setOnAction(e -> {
            boolean light = !themeEngine.lightModeProperty().get();
            themeEngine.lightModeProperty().set(light);
            updateModeButtonState(modeToggle, light);
            themeEngine.applyDefaultStaticTheme();
        });

        // Initialize state
        modeToggle.setDisable(themeEngine.dynamicColoringProperty().get());

        dynamicRow.getChildren().addAll(dynamicTextCol, dynamicToggle);
        modeRow.getChildren().addAll(modeTextCol, modeToggle);
        themeContent.getChildren().addAll(dynamicRow, modeRow);
        themeCard.getChildren().add(themeContent);
        content.getChildren().add(themeCard);

        // Add Username setting UI
        VBox usernameCard = createSectionCard("Username");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setText(LibraryManager.getInstance().getUsername());
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            LibraryManager.getInstance().setUsername(newVal);
        });
        usernameCard.getChildren().add(usernameField);
        content.getChildren().add(usernameCard);

        // --- PLAYLIST BACKUP SECTION ---
        VBox playlistCard = createSectionCard("Playlist Backup & Restore");
        VBox playlistContent = new VBox(15);

        HBox exportRow = new HBox(20);
        exportRow.setAlignment(Pos.CENTER_LEFT);
        Label exportLabel = createSettingLabel("Export Playlist");
        exportLabel.setMinWidth(120);

        ComboBox<String> playlistCombo = new ComboBox<>();
        playlistCombo.setPromptText("Select Playlist");
        playlistCombo.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-background-radius: 6; -fx-min-width: 180;");
        
        // Refresh playlist list dynamically whenever ComboBox is opened/showing
        playlistCombo.setOnShowing(e -> {
            String currentSelected = playlistCombo.getValue();
            playlistCombo.getItems().clear();
            java.util.List<aura.music.model.Playlist> updatedList = LibraryManager.getInstance().getPlaylists();
            for (aura.music.model.Playlist p : updatedList) {
                playlistCombo.getItems().add(p.getName());
            }
            if (currentSelected != null && playlistCombo.getItems().contains(currentSelected)) {
                playlistCombo.setValue(currentSelected);
            }
        });
        
        // Populate ComboBox with current playlists
        java.util.List<aura.music.model.Playlist> currentPlaylists = LibraryManager.getInstance().getPlaylists();
        for (aura.music.model.Playlist p : currentPlaylists) {
            playlistCombo.getItems().add(p.getName());
        }

        Button exportBtn = new Button("Export");
        exportBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        exportBtn.setDisable(true);

        playlistCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            exportBtn.setDisable(newVal == null);
        });

        exportBtn.setOnAction(e -> {
            String selectedName = playlistCombo.getValue();
            if (selectedName != null) {
                java.util.List<aura.music.model.Playlist> updatedList = LibraryManager.getInstance().getPlaylists();
                aura.music.model.Playlist target = updatedList.stream()
                        .filter(p -> selectedName.equals(p.getName()))
                        .findFirst().orElse(null);
                if (target != null) {
                    exportPlaylist(target);
                }
            }
        });

        exportRow.getChildren().addAll(exportLabel, playlistCombo, exportBtn);

        HBox importRow = new HBox(20);
        importRow.setAlignment(Pos.CENTER_LEFT);
        Label importLabel = createSettingLabel("Import Playlist");
        importLabel.setMinWidth(120);

        Button importBtn = new Button("Import");
        importBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        importBtn.setOnAction(e -> {
            importPlaylist();
            // Refresh list
            playlistCombo.getItems().clear();
            for (aura.music.model.Playlist p : LibraryManager.getInstance().getPlaylists()) {
                playlistCombo.getItems().add(p.getName());
            }
        });

        importRow.getChildren().addAll(importLabel, importBtn);

        playlistContent.getChildren().addAll(exportRow, importRow);
        playlistCard.getChildren().add(playlistContent);
        content.getChildren().add(playlistCard);

        setContent(content);
    }

    private void exportPlaylist(aura.music.model.Playlist playlist) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Playlist");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        fileChooser.setInitialFileName(playlist.getName() + ".json");
        
        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (java.io.Writer writer = new java.io.FileWriter(file)) {
                PlaylistExportData data = new PlaylistExportData();
                data.name = playlist.getName();
                data.songs = new java.util.ArrayList<>();
                for (aura.music.model.Song s : playlist.getSongs()) {
                    SongRecord r = new SongRecord();
                    r.path = s.getPath();
                    r.title = s.getTitle();
                    r.artist = s.getArtist();
                    data.songs.add(r);
                }
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Playlist successfully exported to " + file.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to export playlist");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void importPlaylist() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Import Playlist");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        
        java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try (java.io.Reader reader = new java.io.FileReader(file)) {
                PlaylistExportData data = new com.google.gson.Gson().fromJson(reader, PlaylistExportData.class);
                if (data == null || data.name == null || data.songs == null) {
                    throw new IllegalArgumentException("Invalid playlist format");
                }

                LibraryManager lib = LibraryManager.getInstance();
                aura.music.model.Playlist imported = lib.createPlaylist(data.name);

                java.util.List<aura.music.model.Song> librarySongs = lib.getSongs();
                int matchedCount = 0;

                for (SongRecord record : data.songs) {
                    aura.music.model.Song matchedSong = null;
                    
                    if (record.path != null) {
                        matchedSong = librarySongs.stream()
                                .filter(s -> s.getPath().equals(record.path))
                                .findFirst().orElse(null);
                    }
                    
                    if (matchedSong == null && record.title != null && record.artist != null) {
                        matchedSong = librarySongs.stream()
                                .filter(s -> s.getTitle().equalsIgnoreCase(record.title) && s.getArtist().equalsIgnoreCase(record.artist))
                                .findFirst().orElse(null);
                    }

                    if (matchedSong == null && record.title != null) {
                        matchedSong = librarySongs.stream()
                                .filter(s -> s.getTitle().toLowerCase().contains(record.title.toLowerCase()))
                                .findFirst().orElse(null);
                    }

                    if (matchedSong != null) {
                        imported.addSong(matchedSong);
                        matchedCount++;
                    }
                }

                lib.savePlaylists();
                if (onPlaylistsChanged != null) {
                    onPlaylistsChanged.run();
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Import Complete");
                alert.setContentText(String.format("Playlist '%s' successfully imported!\nMatched %d out of %d songs in local library.",
                        data.name, matchedCount, data.songs.size()));
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to import playlist");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private static class PlaylistExportData {
        String name;
        java.util.List<SongRecord> songs;
    }

    private static class SongRecord {
        String path;
        String title;
        String artist;
    }

    private VBox createSectionCard(String sectionTitle) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(35, 35, 35, 0.4); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1; -fx-border-radius: 12;");

        Label title = new Label(sectionTitle);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-accent;");
        card.getChildren().add(title);
        return card;
    }

    private Label createSettingLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: 500;");
        return label;
    }

    private ComboBox<String> createLayoutCombo(BooleanProperty gridProperty) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Grid View", "List View");
        combo.setValue(gridProperty.get() ? "Grid View" : "List View");
        combo.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-background-radius: 6;");

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            gridProperty.set("Grid View".equals(newVal));
        });
        return combo;
    }

    private void updateToggleButtonState(Button btn, boolean on) {
        if (on) {
            btn.setText("ON");
            btn.setStyle(
                    "-fx-background-color: #38ef7d; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 60; -fx-min-height: 30; -fx-cursor: hand;");
        } else {
            btn.setText("OFF");
            btn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 60; -fx-min-height: 30; -fx-cursor: hand;");
        }
    }

    private void updateModeButtonState(Button btn, boolean light) {
        if (light) {
            btn.setText("LIGHT");
            btn.setStyle(
                    "-fx-background-color: #ffffff; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        } else {
            btn.setText("DARK");
            btn.setStyle(
                    "-fx-background-color: #1c1c1e; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        }
    }
}
