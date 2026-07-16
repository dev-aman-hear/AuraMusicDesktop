package aura.music.ui.components;

import aura.music.lyrics.LyricLine;
import aura.music.viewmodel.MainViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

        lyricsContainer = new VBox(20);
        lyricsContainer.setAlignment(Pos.CENTER);
        lyricsContainer.setStyle("-fx-padding: 200 20 200 20;");
        scrollPane.setContent(lyricsContainer);

        // Return to Live Button
        returnToLiveButton = new Button("Return to Live Lyrics");
        returnToLiveButton.getStyleClass().add("glass-panel");
        returnToLiveButton.setStyle("-fx-text-fill: white; -fx-padding: 8 16; -fx-cursor: hand;");
        returnToLiveButton.setVisible(false);
        StackPane.setAlignment(returnToLiveButton, Pos.BOTTOM_CENTER);
        returnToLiveButton.setTranslateY(-20);

        returnToLiveButton.setOnAction(e -> {
            userScrolling = false;
            returnToLiveButton.setVisible(false);
            scrollToActiveLine(true);
        });

        getChildren().addAll(scrollPane, returnToLiveButton);

        // Detect user manual scroll
        scrollPane.setOnScroll(e -> {
            triggerUserScroll();
        });
        scrollPane.setOnMousePressed(e -> {
            triggerUserScroll();
        });

        // Bind data from ViewModel
        viewModel.getLyricsLines()
                .addListener((javafx.collections.ListChangeListener.Change<? extends LyricLine> c) -> {
                    rebuildLyrics();
                });

        viewModel.activeLyricLineIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateActiveLine(newVal.intValue());
        });

        rebuildLyrics();
    }

    private void triggerUserScroll() {
        userScrolling = true;
        returnToLiveButton.setVisible(true);

        // Auto-return to live lyrics after 5 seconds of inactivity
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
            Label placeholder = new Label("No Lyrics Available");
            placeholder.getStyleClass().add("lyric-line-label");
            lyricsContainer.getChildren().add(placeholder);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            Label label = new Label(line.getText());
            label.getStyleClass().add("lyric-line-label");
            label.setWrapText(true);
            label.setAlignment(Pos.CENTER);
            label.setMaxWidth(Double.MAX_VALUE);
            
            label.styleProperty().bind(
                javafx.beans.binding.Bindings.concat("-fx-font-size: ", viewModel.lyricTextSizeProperty(), "px;")
            );
            
            // Allow CSS to control text fill base, but we will override it via class for active
            
            label.setOnMouseEntered(e -> {
                int idx = lineLabels.indexOf(label);
                if (idx != currentActiveIndex) {
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), label);
                    st.setToX(1.02);
                    st.setToY(1.02);
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(150), label);
                    ft.setToValue(0.7);
                    st.play();
                    ft.play();
                }
            });
            
            label.setOnMouseExited(e -> {
                int idx = lineLabels.indexOf(label);
                if (idx != currentActiveIndex) {
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), label);
                    st.setToX(idx < currentActiveIndex ? 0.96 : 1.0);
                    st.setToY(idx < currentActiveIndex ? 0.96 : 1.0);
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(150), label);
                    ft.setToValue(idx < currentActiveIndex ? 0.18 : 0.45);
                    st.play();
                    ft.play();
                }
            });

            label.setOnMouseClicked(e -> {
                viewModel.seek(line.getTimestamp() / 1000.0);
                userScrolling = false;
                returnToLiveButton.setVisible(false);
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

                double targetScale = 1.0;
                double targetOpacity = 0.45;

                if (i == activeIndex) {
                    targetScale = 1.06;
                    targetOpacity = 1.0;
                    if (!label.getStyleClass().contains("lyric-line-active")) {
                        label.getStyleClass().add("lyric-line-active");
                    }
                } else if (i < activeIndex) {
                    targetScale = 0.96;
                    targetOpacity = 0.18;
                    label.getStyleClass().remove("lyric-line-active");
                } else {
                    targetScale = 1.0;
                    targetOpacity = 0.45;
                    label.getStyleClass().remove("lyric-line-active");
                }

                // Smoothly animate scale and opacity
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(250), label);
                ft.setToValue(targetOpacity);

                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(250), label);
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
        double containerHeight = lyricsContainer.getHeight();
        double scrollPaneHeight = scrollPane.getHeight();

        if (containerHeight <= scrollPaneHeight)
            return;

        // Calculate target scroll value to center the active label
        double targetV = (layoutY - (scrollPaneHeight / 2.0) + (activeLabel.getHeight() / 2.0))
                / (containerHeight - scrollPaneHeight);

        targetV = Math.max(0.0, Math.min(1.0, targetV));

        if (scrollTimeline != null) {
            scrollTimeline.stop();
        }

        if (smooth) {
            scrollTimeline = new Timeline(new KeyFrame(Duration.millis(350),
                    new KeyValue(scrollPane.vvalueProperty(), targetV)));
            scrollTimeline.play();
        } else {
            scrollPane.setVvalue(targetV);
        }
    }
}
