package aura.music.ui.components;

import aura.music.theme.ThemeEngine;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class VisualizerCanvas extends Canvas {

    public enum Mode {
        BARS, WAVEFORM, CIRCULAR
    }

    private Mode mode = Mode.BARS;
    private float[] magnitudes = new float[64];
    private final float[] smoothMagnitudes = new float[64];
    private final float decayRate = 1.8f; // Smooth decay rate

    private final ThemeEngine themeEngine = ThemeEngine.getInstance();

    public VisualizerCanvas(double width, double height) {
        super(width, height);
        
        // Redraw when width/height changes
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());

        // Periodically decay values for smooth animation even when audio pauses
        Thread decayThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(16); // ~60 FPS decay
                    boolean needsRedraw = false;
                    for (int i = 0; i < smoothMagnitudes.length; i++) {
                        if (smoothMagnitudes[i] > 0) {
                            smoothMagnitudes[i] = Math.max(0, smoothMagnitudes[i] - decayRate);
                            needsRedraw = true;
                        }
                    }
                    if (needsRedraw) {
                        Platform.runLater(this::draw);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        decayThread.setDaemon(true);
        decayThread.start();
        
        draw();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        draw();
    }

    public Mode getMode() {
        return mode;
    }

    public void updateSpectrum(float[] newMagnitudes) {
        if (newMagnitudes == null) return;
        
        // JavaFX spectrum magnitudes are in dB, ranging from -60 (silent) to 0 (loudest).
        // Convert to positive values.
        for (int i = 0; i < Math.min(newMagnitudes.length, magnitudes.length); i++) {
            float val = newMagnitudes[i] + 60; // Now 0 to 60
            val = Math.max(0, val);
            magnitudes[i] = val;
            
            if (val > smoothMagnitudes[i]) {
                smoothMagnitudes[i] = val; // Instant rise
            }
        }
        Platform.runLater(this::draw);
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        Color accent = themeEngine.accentColorProperty().get();
        if (accent == null) accent = Color.BLUE;

        switch (mode) {
            case BARS:
                drawBars(gc, w, h, accent);
                break;
            case WAVEFORM:
                drawWaveform(gc, w, h, accent);
                break;
            case CIRCULAR:
                drawCircular(gc, w, h, accent);
                break;
        }
    }

    private void drawBars(GraphicsContext gc, double w, double h, Color accent) {
        int numBars = smoothMagnitudes.length;
        double spacing = 4.0;
        double barWidth = (w - (spacing * (numBars - 1))) / numBars;

        // Gradient color for bars
        LinearGradient gradient = new LinearGradient(0, h, 0, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, accent.deriveColor(0, 1, 1, 0.2)),
                new Stop(1, accent)
        );
        gc.setFill(gradient);

        for (int i = 0; i < numBars; i++) {
            double barHeight = (smoothMagnitudes[i] / 60.0) * h * 0.8;
            double x = i * (barWidth + spacing);
            double y = h - barHeight;
            // Rounded bars
            gc.fillRoundRect(x, y, barWidth, barHeight, barWidth / 2, barWidth / 2);
        }
    }

    private void drawWaveform(GraphicsContext gc, double w, double h, Color accent) {
        int numPoints = smoothMagnitudes.length;
        double step = w / (numPoints - 1);
        double midY = h / 2.0;

        gc.setStroke(accent);
        gc.setLineWidth(3.0);
        gc.beginPath();

        for (int i = 0; i < numPoints; i++) {
            double amplitude = (smoothMagnitudes[i] / 60.0) * (h / 2.5);
            // Alternate sign to simulate wave
            double y = midY + (i % 2 == 0 ? amplitude : -amplitude);
            double x = i * step;

            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();
    }

    private void drawCircular(GraphicsContext gc, double w, double h, Color accent) {
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double baseRadius = Math.min(w, h) * 0.25;

        int numPoints = smoothMagnitudes.length;
        double angleStep = 2 * Math.PI / numPoints;

        gc.setStroke(accent);
        gc.setLineWidth(3.0);

        for (int i = 0; i < numPoints; i++) {
            double amplitude = (smoothMagnitudes[i] / 60.0) * baseRadius * 0.7;
            double angle = i * angleStep;
            
            double r = baseRadius + amplitude;
            double x1 = centerX + baseRadius * Math.cos(angle);
            double y1 = centerY + baseRadius * Math.sin(angle);
            double x2 = centerX + r * Math.cos(angle);
            double y2 = centerY + r * Math.sin(angle);

            gc.strokeLine(x1, y1, x2, y2);
        }
    }
}
