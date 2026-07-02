package aura.music.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class MarqueeUtils {

    public static Node createMarqueeLabel(String text, String style, double maxWidth) {
        // Create the primary label
        Label label1 = new Label(text);
        label1.setStyle(style);
        label1.setWrapText(false);
        label1.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(maxWidth);
        container.setMinWidth(maxWidth);
        container.setPrefWidth(maxWidth);

        Rectangle clip = new Rectangle(maxWidth, 40);
        container.setClip(clip);

        container.heightProperty().addListener((obs, oldVal, newVal) -> {
            clip.setHeight(newVal.doubleValue());
        });

        // One-time listener to prevent infinite layout recalculation loops
        label1.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
                double textWidth = newVal.doubleValue();
                if (textWidth > 0) {
                    label1.widthProperty().removeListener(this);
                    if (textWidth > maxWidth) {
                        Label label2 = new Label(text);
                        label2.setStyle(style);
                        label2.setWrapText(false);
                        label2.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                        double gap = 50.0;
                        HBox hbox = new HBox(gap, label1, label2);
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        hbox.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                        container.getChildren().setAll(hbox);

                        double scrollDist = textWidth + gap;

                        TranslateTransition transition = new TranslateTransition(
                                Duration.seconds(scrollDist / 30.0), hbox); // Smooth speed
                        transition.setFromX(0);
                        transition.setToX(-scrollDist);
                        transition.setInterpolator(Interpolator.LINEAR);
                        transition.setCycleCount(Animation.INDEFINITE);
                        transition.play();
                    } else {
                        container.getChildren().setAll(label1);
                    }
                }
            }
        });

        // Add label1 initially so it can be measured
        container.getChildren().setAll(label1);

        return container;
    }
}
