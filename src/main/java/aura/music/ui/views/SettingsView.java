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
    private final javafx.beans.property.IntegerProperty lyricTextSize;

    public SettingsView(BooleanProperty albumsGrid, BooleanProperty artistsGrid, BooleanProperty genresGrid, BooleanProperty miniplayerPinned, javafx.beans.property.IntegerProperty lyricTextSize, Runnable onPlaylistsChanged) {
        this.onPlaylistsChanged = onPlaylistsChanged;
        this.lyricTextSize = lyricTextSize;
        setFitToWidth(true);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(30);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setStyle("-fx-background-color: transparent;");

        // Header Title
        Label titleLabel = new Label("Settings");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
        content.getChildren().add(titleLabel);

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

        Label miniplayerLabel = createSettingLabel("Always miniplayer on top");
        Button miniplayerToggle = new Button();
        updateToggleButtonState(miniplayerToggle, miniplayerPinned.get());
        miniplayerToggle.setOnAction(e -> {
            boolean active = !miniplayerPinned.get();
            miniplayerPinned.set(active);
            updateToggleButtonState(miniplayerToggle, active);
        });
        layoutGrid.add(miniplayerLabel, 0, 3);
        layoutGrid.add(miniplayerToggle, 1, 3);

        Label lyricSizeLabel = createSettingLabel("Lyric Text Size (" + lyricTextSize.get() + "px)");
        
        HBox stepsContainer = new HBox(6);
        stepsContainer.setAlignment(Pos.CENTER_LEFT);
        int[] stepValues = {14, 18, 22, 26, 30, 34, 38, 42, 46, 50};
        
        Runnable updateSteps = new Runnable() {
            @Override
            public void run() {
                stepsContainer.getChildren().clear();
                for (int val : stepValues) {
                    Region step = new Region();
                    step.setPrefSize(24, 8);
                    boolean isSelected = val <= lyricTextSize.get();
                    String baseStyle = isSelected 
                        ? "-fx-background-color: linear-gradient(to right, #00c6ff, #0072ff); -fx-background-radius: 4;" 
                        : "-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 4;";
                    
                    step.setStyle(baseStyle);
                    step.setCursor(javafx.scene.Cursor.HAND);
                    
                    step.setOnMouseClicked(e -> {
                        lyricTextSize.set(val);
                        lyricSizeLabel.setText("Lyric Text Size (" + val + "px)");
                        this.run();
                    });
                    
                    step.setOnMouseEntered(e -> {
                        if (!isSelected) {
                            step.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 4;");
                        }
                    });
                    
                    step.setOnMouseExited(e -> {
                        if (!isSelected) {
                            step.setStyle(baseStyle);
                        }
                    });
                    
                    stepsContainer.getChildren().add(step);
                }
            }
        };
        updateSteps.run();

        layoutGrid.add(lyricSizeLabel, 0, 4);
        layoutGrid.add(stepsContainer, 1, 4);

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


        // --- FOLDERS SECTION ---
        VBox foldersCard = createSectionCard("Library Folders");
        VBox foldersListContainer = new VBox(10);
        
        Runnable updateFoldersList = new Runnable() {
            @Override
            public void run() {
                foldersListContainer.getChildren().clear();
                for (String folderPath : LibraryManager.getInstance().getWatchedFolders()) {
                    HBox row = new HBox(15);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 8;");
                    
                    Label pathLabel = new Label(folderPath);
                    pathLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                    HBox.setHgrow(pathLabel, Priority.ALWAYS);
                    pathLabel.setMaxWidth(Double.MAX_VALUE);
                    
                    Button removeBtn = new Button("Remove");
                    removeBtn.setStyle("-fx-background-color: rgba(255,76,76,0.8); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> {
                        LibraryManager.getInstance().removeWatchedFolder(folderPath);
                        this.run();
                    });
                    
                    row.getChildren().addAll(pathLabel, removeBtn);
                    foldersListContainer.getChildren().add(row);
                }
            }
        };
        updateFoldersList.run();

        Button addFolderBtn = new Button("Add Folder");
        addFolderBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 100; -fx-min-height: 30; -fx-cursor: hand;");
        addFolderBtn.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Select Library Folder");
            java.io.File selectedDir = dirChooser.showDialog(getScene().getWindow());
            if (selectedDir != null) {
                LibraryManager.getInstance().addWatchedFolder(selectedDir.getAbsolutePath());
                updateFoldersList.run();
            }
        });

        foldersCard.getChildren().addAll(foldersListContainer, addFolderBtn);
        content.getChildren().add(foldersCard);

        // --- PLAYLIST BACKUP SECTION ---
        VBox playlistCard = createSectionCard("Playlist Backup & Restore");
        VBox playlistContent = new VBox(15);

        HBox exportRow = new HBox(20);
        exportRow.setAlignment(Pos.CENTER_LEFT);
        Label exportLabel = createSettingLabel("Export Playlist");
        exportLabel.setMinWidth(120);

        ComboBox<String> playlistCombo = new ComboBox<>();
        playlistCombo.setPromptText("Select Playlist");
        playlistCombo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: rgba(255,255,255,0.2); " +
                "-fx-border-width: 1; " +
                "-fx-min-width: 200; " +
                "-fx-pref-height: 36; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-cursor: hand;"
        );

        playlistCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(playlistCombo.getPromptText());
                    setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-weight: 600; -fx-font-size: 14px; -fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-weight: 600; -fx-font-size: 14px; -fx-background-color: transparent;");
                }
            }
        });
        
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
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background-color: rgba(20, 20, 20, 0.6); -fx-background-radius: 16; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; -fx-border-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 5);");

        Label title = new Label(sectionTitle);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: white;");
        card.getChildren().add(title);
        return card;
    }

    private Label createSettingLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85); -fx-font-weight: 600;");
        return label;
    }

    private ComboBox<String> createLayoutCombo(BooleanProperty gridProperty) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Grid View", "List View");
        combo.setValue(gridProperty.get() ? "Grid View" : "List View");
        combo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: rgba(255,255,255,0.2); " +
                "-fx-border-width: 1; " +
                "-fx-pref-width: 150; " +
                "-fx-pref-height: 36; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-cursor: hand;"
        );

        combo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-weight: 600; -fx-font-size: 14px; -fx-background-color: transparent;");
                }
            }
        });

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            gridProperty.set("Grid View".equals(newVal));
        });
        return combo;
    }

    private void updateToggleButtonState(Button btn, boolean on) {
        if (on) {
            btn.setText("ON");
            btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #00c6ff, #0072ff); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 70; -fx-min-height: 32; -fx-cursor: hand;");
        } else {
            btn.setText("OFF");
            btn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: rgba(255,255,255,0.5); -fx-font-weight: bold; -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-min-width: 70; -fx-min-height: 32; -fx-cursor: hand;");
        }
    }

    private void updateModeButtonState(Button btn, boolean light) {
        if (light) {
            btn.setText("LIGHT");
            btn.setStyle(
                    "-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 90; -fx-min-height: 32; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 10, 0, 0, 0);");
        } else {
            btn.setText("DARK");
            btn.setStyle(
                    "-fx-background-color: #1a1a1d; -fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-min-width: 90; -fx-min-height: 32; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        }
    }
}
