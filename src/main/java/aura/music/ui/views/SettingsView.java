package aura.music.ui.views;

import aura.music.library.LibraryManager;

import aura.music.theme.ThemeEngine;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class SettingsView extends ScrollPane {

    private final ThemeEngine themeEngine = ThemeEngine.getInstance();
    private final BooleanProperty albumsGrid;
    private final BooleanProperty artistsGrid;
    private final BooleanProperty genresGrid;

    public SettingsView(BooleanProperty albumsGrid, BooleanProperty artistsGrid, BooleanProperty genresGrid) {
        this.albumsGrid = albumsGrid;
        this.artistsGrid = artistsGrid;
        this.genresGrid = genresGrid;

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
        
        setContent(content);
    }

    private VBox createSectionCard(String sectionTitle) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: rgba(35, 35, 35, 0.4); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1; -fx-border-radius: 12;");
        
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
            btn.setStyle("-fx-background-color: #38ef7d; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 60; -fx-min-height: 30; -fx-cursor: hand;");
        } else {
            btn.setText("OFF");
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 60; -fx-min-height: 30; -fx-cursor: hand;");
        }
    }

    private void updateModeButtonState(Button btn, boolean light) {
        if (light) {
            btn.setText("LIGHT");
            btn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        } else {
            btn.setText("DARK");
            btn.setStyle("-fx-background-color: #1c1c1e; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 80; -fx-min-height: 30; -fx-cursor: hand;");
        }
    }
}
