package aura.music.ui.components;

import aura.music.lyrics.LyricLine;
import aura.music.viewmodel.MainViewModel;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LyricsView extends StackPane {

    private final ScrollPane scrollPane;
    private final VBox lyricsContainer;
    private final Button returnToLiveButton;
    private final MainViewModel viewModel;

    private final List<Label> lineLabels = new ArrayList<>();
    private int currentActiveIndex = -1;
    private boolean userScrolling = false;
    private Timeline scrollTimeline;
    private Timeline userScrollTimeout;

    public LyricsView(MainViewModel viewModel) {
        this.viewModel = viewModel;

        // ScrollPane Setup
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        lyricsContainer = new VBox(22);
        lyricsContainer.setAlignment(Pos.CENTER);
        lyricsContainer.setStyle("-fx-padding: 180 24 180 24;");
        scrollPane.setContent(lyricsContainer);

        // Return to Live Button (Apple Music Pill Style)
        returnToLiveButton = new Button("Return to Live Lyrics");
        returnToLiveButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 8 18; " +
                "-fx-background-radius: 20; " +
                "-fx-border-color: rgba(255, 255, 255, 0.25); " +
                "-fx-border-radius: 20; " +
                "-fx-cursor: hand;"
        );
        returnToLiveButton.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.4)));
        returnToLiveButton.setVisible(false);
        StackPane.setAlignment(returnToLiveButton, Pos.BOTTOM_CENTER);
        returnToLiveButton.setTranslateY(-25);

        returnToLiveButton.setOnAction(e -> {
            userScrolling = false;
            returnToLiveButton.setVisible(false);
            scrollToActiveLine(true);
        });

        getChildren().addAll(scrollPane, returnToLiveButton);

        // Detect user manual scroll
        scrollPane.setOnScroll(e -> triggerUserScroll());
        scrollPane.setOnMousePressed(e -> triggerUserScroll());

        // Bind data from ViewModel
        viewModel.getLyricsLines()
                .addListener((javafx.collections.ListChangeListener.Change<? extends LyricLine> c) -> {
                    Platform.runLater(this::rebuildLyrics);
                });

        viewModel.activeLyricLineIndexProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateActiveLine(newVal.intValue()));
        });

        // Ensure auto-scrolling triggers when container layout height updates
        lyricsContainer.heightProperty().addListener((obs, oldH, newH) -> {
            if (!userScrolling && currentActiveIndex >= 0) {
                Platform.runLater(() -> scrollToActiveLine(false));
            }
        });
        scrollPane.heightProperty().addListener((obs, oldH, newH) -> {
            if (!userScrolling && currentActiveIndex >= 0) {
                Platform.runLater(() -> scrollToActiveLine(false));
            }
        });

        rebuildLyrics();
    }

    private void triggerUserScroll() {
        userScrolling = true;
        returnToLiveButton.setVisible(true);

        if (userScrollTimeout != null) {
            userScrollTimeout.stop();
        }
        userScrollTimeout = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (userScrolling) {
                userScrolling = false;
                returnToLiveButton.setVisible(false);
                scrollToActiveLine(true);
            }
        }));
        userScrollTimeout.play();
    }

    private void rebuildLyrics() {
        lyricsContainer.getChildren().clear();
        lineLabels.clear();
        currentActiveIndex = -1;

        List<LyricLine> lines = viewModel.getLyricsLines();
        if (lines.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);

            Label iconLabel = new Label("🎵");
            iconLabel.setStyle("-fx-font-size: 32px; -fx-opacity: 0.5;");

            Label titleLabel = new Label("No Lyrics Available");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-opacity: 0.85;");

            Label subtitleLabel = new Label("Lyrics for this offline track aren't in your local library.");
            subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255, 255, 255, 0.45);");

            emptyBox.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);
            lyricsContainer.getChildren().add(emptyBox);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            Label label = new Label(line.getText());
            label.getStyleClass().add("lyric-line-label");
            label.setWrapText(true);
            label.setAlignment(Pos.CENTER);
            label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            label.setMaxWidth(Double.MAX_VALUE);

            // Apple Music Font Styling
            int baseFontSize = viewModel.lyricTextSizeProperty().get();
            label.setStyle(
                    "-fx-font-family: 'SF Pro Display', 'Segoe UI', 'Inter', sans-serif; " +
                    "-fx-font-weight: 700; " +
                    "-fx-text-fill: rgba(255, 255, 255, 0.65); " +
                    "-fx-padding: 4px 12px; " +
                    "-fx-text-alignment: center; " +
                    "-fx-font-size: " + baseFontSize + "px;"
            );

            label.setOnMouseEntered(e -> {
                int idx = lineLabels.indexOf(label);
                if (idx != currentActiveIndex) {
                    ScaleTransition st = new ScaleTransition(Duration.millis(180), label);
                    st.setToX(1.02);
                    st.setToY(1.02);
                    FadeTransition ft = new FadeTransition(Duration.millis(180), label);
                    ft.setToValue(0.70);
                    st.play();
                    ft.play();
                }
            });

            label.setOnMouseExited(e -> {
                int idx = lineLabels.indexOf(label);
                if (idx != currentActiveIndex) {
                    ScaleTransition st = new ScaleTransition(Duration.millis(180), label);
                    st.setToX(idx < currentActiveIndex ? 0.95 : 1.00);
                    st.setToY(idx < currentActiveIndex ? 0.95 : 1.00);
                    FadeTransition ft = new FadeTransition(Duration.millis(180), label);
                    ft.setToValue(idx < currentActiveIndex ? 0.25 : 0.50);
                    st.play();
                    ft.play();
                }
            });

            label.setOnMouseClicked(e -> {
                if (line.getTimestamp() >= 0) {
                    viewModel.seek(line.getTimestamp() / 1000.0);
                    userScrolling = false;
                    returnToLiveButton.setVisible(false);
                }
            });

            lineLabels.add(label);
            lyricsContainer.getChildren().add(label);
        }
    }

    private void updateActiveLine(int activeIndex) {
        if (activeIndex == currentActiveIndex || lineLabels.isEmpty())
            return;
        currentActiveIndex = activeIndex;

        Platform.runLater(() -> {
            for (int i = 0; i < lineLabels.size(); i++) {
                Label label = lineLabels.get(i);

                double targetScale = 1.00;
                double targetOpacity = 0.50;

                if (i == activeIndex) {
                    targetScale = 1.08;
                    targetOpacity = 1.00;
                    label.setStyle(
                            "-fx-font-family: 'SF Pro Display', 'Segoe UI', 'Inter', sans-serif; " +
                            "-fx-font-weight: 800; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-background-color: transparent; " +
                            "-fx-padding: 4px 12px; " +
                            "-fx-text-alignment: center; " +
                            "-fx-font-size: " + (viewModel.lyricTextSizeProperty().get() + 2) + "px;"
                    );
                    // Soft pure white ambient glow effect
                    DropShadow glowEffect = new DropShadow();
                    glowEffect.setRadius(20.0);
                    glowEffect.setSpread(0.20);
                    glowEffect.setColor(Color.rgb(255, 255, 255, 0.65));
                    label.setEffect(glowEffect);
                } else if (i < activeIndex) {
                    targetScale = 0.95;
                    targetOpacity = 0.25;
                    label.setStyle(
                            "-fx-font-family: 'SF Pro Display', 'Segoe UI', 'Inter', sans-serif; " +
                            "-fx-font-weight: 700; " +
                            "-fx-text-fill: rgba(255, 255, 255, 0.45); " +
                            "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-padding: 4px 12px; " +
                            "-fx-text-alignment: center; " +
                            "-fx-font-size: " + viewModel.lyricTextSizeProperty().get() + "px;"
                    );
                    label.setEffect(null);
                } else {
                    targetScale = 1.00;
                    targetOpacity = 0.50;
                    label.setStyle(
                            "-fx-font-family: 'SF Pro Display', 'Segoe UI', 'Inter', sans-serif; " +
                            "-fx-font-weight: 700; " +
                            "-fx-text-fill: rgba(255, 255, 255, 0.65); " +
                            "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-padding: 4px 12px; " +
                            "-fx-text-alignment: center; " +
                            "-fx-font-size: " + viewModel.lyricTextSizeProperty().get() + "px;"
                    );
                    label.setEffect(null);
                }

                // Smooth scale and opacity transitions
                FadeTransition ft = new FadeTransition(Duration.millis(250), label);
                ft.setToValue(targetOpacity);

                ScaleTransition st = new ScaleTransition(Duration.millis(250), label);
                st.setToX(targetScale);
                st.setToY(targetScale);

                ft.play();
                st.play();
            }

            if (!userScrolling) {
                scrollToActiveLine(true);
            }
        });
    }

    private void scrollToActiveLine(boolean smooth) {
        if (currentActiveIndex < 0 || currentActiveIndex >= lineLabels.size())
            return;

        Label activeLabel = lineLabels.get(currentActiveIndex);
        double layoutY = activeLabel.getLayoutY();
        double activeHeight = activeLabel.getHeight();
        if (activeHeight <= 0) activeHeight = activeLabel.prefHeight(-1);

        double containerHeight = lyricsContainer.getHeight();
        double scrollPaneHeight = scrollPane.getHeight();

        if (containerHeight <= scrollPaneHeight || containerHeight <= 0)
            return;

        // Precision vertical centering calculation
        double targetV = (layoutY + (activeHeight / 2.0) - (scrollPaneHeight / 2.0))
                / (containerHeight - scrollPaneHeight);

        targetV = Math.max(0.0, Math.min(1.0, targetV));

        if (scrollTimeline != null) {
            scrollTimeline.stop();
        }

        if (smooth) {
            scrollTimeline = new Timeline(new KeyFrame(Duration.millis(350),
                    new KeyValue(scrollPane.vvalueProperty(), targetV, javafx.animation.Interpolator.EASE_BOTH)));
            scrollTimeline.play();
        } else {
            scrollPane.setVvalue(targetV);
        }
    }
}
