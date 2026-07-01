package aura.music.ui.components;

import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;

public class SVGIcons {

    public static SVGPath createPlayIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M8 5v14l11-7z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createPauseIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M6 19h4V5H6v14zm8-14v14h4V5h-4z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createNextIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createPreviousIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createVolumeIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createMuteIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.21.05-.42.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createHeartIcon(double size, Color color, boolean filled) {
        SVGPath path = new SVGPath();
        if (filled) {
            path.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
        } else {
            path.setContent("M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z");
        }
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createStarIcon(double size, Color color, boolean filled) {
        SVGPath path = new SVGPath();
        if (filled) {
            path.setContent("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z");
        } else {
            path.setContent("M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z");
        }
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createLyricsIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9v-2h2v2zm0-4H9V7h2v5zm4 4h-2v-5h2v5zm0-6h-2V7h2v3z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createQueueIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createSearchIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createSettingsIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createHomeIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createCompassIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 14.19L8.81 19l2.81-4.19L15.19 12 12.38 16.19zM12 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createRadioIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 2C6.48 2 2 6.48 2 12c0 4.82 3.4 8.84 8 9.82V18.1c-2.28-.88-4-3.1-4-5.7 0-3.48 2.82-6.3 6.3-6.3s6.3 2.82 6.3 6.3c0 2.6-1.72 4.82-4 5.7v3.72c4.6-.98 8-5 8-9.82 0-5.52-4.48-10-10-10zm0 8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createClockIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createArtistIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createAlbumIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.48 0-4.5-2.02-4.5-4.5S9.52 7.5 12 7.5s4.5 2.02 4.5 4.5-2.02 4.5-4.5 4.5zM12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createMusicIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createBackIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createExitFullScreenIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createMenuIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createFolderIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createGenreIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.75z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createPlaylistIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M4 10h12v2H4zm0-4h12v2H4zm0 8h8v2H4zm10 0v6l5-3z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createMiniPlayerIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-8-2h6v-6h-6v6z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createShuffleIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.45 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createRepeatIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createAirPlayIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M6 22h12l-6-6-6 6zM21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h4v-2H3V5h18v12h-4v2h4c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z");
        setupPath(path, size, color);
        return path;
    }

    public static SVGPath createWaveIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M2 12v-4M6 16V4M10 14V6M14 18V2M18 15v-6M22 12v-2");
        path.setStroke(color);
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    private static void setupPath(SVGPath path, double size, Color color) {
        path.setFill(color);
        // Compute scale to match requested size (default viewport is usually 24x24)
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
    }
}
