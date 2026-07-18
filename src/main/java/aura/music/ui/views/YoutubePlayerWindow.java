package aura.music.ui.views;

import aura.music.model.Song;
import aura.music.viewmodel.MainViewModel;
import com.sun.net.httpserver.HttpServer;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Official YouTube iframe player, bridged to Aura's shared playback controls. */
public final class YoutubePlayerWindow {
    private static WebView activePlayer;
    private static Stage hostStage;
    private static String activeSongPath;
    private YoutubePlayerWindow() { }

    public static void play(String videoId, String title, String artist, String artworkUrl, MainViewModel viewModel) {
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{11}")) return;
        try {
            Song song = new Song("youtube:" + videoId);
            song.setTitle(title); song.setArtist(artist); song.setAlbum("YouTube"); song.setArtworkUrl(artworkUrl);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = server.getAddress().getPort();
            String origin = "http://127.0.0.1:" + port;
            server.createContext("/", exchange -> {
                String page = playerPage(videoId, origin);
                byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
            });
            server.start();

            WebView player = new WebView();
            WebEngine engine = player.getEngine();
            engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36");
            Stage stage = new Stage();
            stage.setTitle(title == null ? "YouTube" : title);
            stage.setScene(new Scene(player, 960, 540));
            PlayerBridge bridge = new PlayerBridge(viewModel);
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    ((JSObject) engine.executeScript("window")).setMember("auraPlayer", bridge);
                }
            });
            stage.setOnHidden(event -> { server.stop(0); viewModel.finishExternalPlayback(song); });
            viewModel.startExternalPlayback(song,
                    () -> execute(engine, "auraResume()"),
                    () -> execute(engine, "auraPause()"),
                    () -> { execute(engine, "auraStop()"); stage.hide(); },
                    seconds -> execute(engine, "auraSeek(" + Math.max(0, seconds) + ")"));
            engine.load(origin + "/");
            stage.show();
            parkHostStage(stage);
            activePlayer = player;
            hostStage = stage;
            activeSongPath = song.getPath();
        } catch (Exception error) {
            System.err.println("Unable to open the embedded YouTube player: " + error.getMessage());
        }
    }

    public static boolean hasVideoFor(Song song) {
        return activePlayer != null && song != null && song.getPath().equals(activeSongPath);
    }

    public static void showVideoIn(StackPane target, boolean disableInteraction) {
        moveVideo(target, disableInteraction, false);
    }

    public static void showVideoTheaterIn(StackPane target) {
        moveVideo(target, true, true);
    }

    private static void moveVideo(StackPane target, boolean disableInteraction, boolean theater) {
        if (activePlayer == null || target == null) return;
        if (hostStage != null && hostStage.getScene() != null) hostStage.getScene().setRoot(new Pane());
        target.getChildren().remove(activePlayer);
        activePlayer.prefWidthProperty().unbind();
        activePlayer.prefHeightProperty().unbind();
        if (theater) {
            activePlayer.setMaxWidth(Double.MAX_VALUE);
            activePlayer.setMaxHeight(Double.MAX_VALUE);
            javafx.application.Platform.runLater(() -> {
                activePlayer.setPrefWidth(target.getWidth());
                activePlayer.setPrefHeight(target.getHeight());
            });
        } else {
            activePlayer.setPrefWidth(480);
            activePlayer.setPrefHeight(480);
            activePlayer.setMaxWidth(480);
            activePlayer.setMaxHeight(480);
        }
        activePlayer.setMouseTransparent(disableInteraction);
        target.getChildren().add(activePlayer);
        StackPane.setAlignment(activePlayer, javafx.geometry.Pos.CENTER);
    }

    public static void hideVideo() {
        if (activePlayer == null || hostStage == null) return;
        if (activePlayer.getParent() instanceof Pane parent) parent.getChildren().remove(activePlayer);
        activePlayer.prefWidthProperty().unbind();
        activePlayer.prefHeightProperty().unbind();
        activePlayer.setPrefWidth(1);
        activePlayer.setPrefHeight(1);
        hostStage.getScene().setRoot(activePlayer);
        if (!hostStage.isShowing()) hostStage.show();
        parkHostStage(hostStage);
    }

    private static void parkHostStage(Stage stage) {
        // Iconified WebViews pause timers on some Windows systems; an off-screen host keeps
        // the official player and its JavaScript control bridge active without a visible popup.
        stage.setIconified(false);
        stage.setWidth(1);
        stage.setHeight(1);
        stage.setX(-10000);
        stage.setY(-10000);
    }

    private static void execute(WebEngine engine, String javascript) {
        try { engine.executeScript(javascript); } catch (Exception ignored) { }
    }

    private static String playerPage(String videoId, String origin) {
        String escapedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
        return "<!doctype html><html><head><meta charset='utf-8'><style>html,body,#player{margin:0;width:100%;height:100%;background:#000}</style>"
                + "<script src='https://www.youtube.com/iframe_api'></script></head><body><div id='player'></div><script>"
                + "let player,requestedPlaying=true,pendingSeek=null; function onYouTubeIframeAPIReady(){player=new YT.Player('player',{videoId:'" + videoId + "',playerVars:{autoplay:1,playsinline:1,origin:'" + escapedOrigin + "'},events:{onReady:onReady,onStateChange:onState}});}"
                + "function onReady(){if(pendingSeek!==null)player.seekTo(pendingSeek,true); if(requestedPlaying)player.playVideo();else player.pauseVideo(); setInterval(report,500);} function onState(e){if(window.auraPlayer) auraPlayer.state(e.data===1); if(e.data===0&&window.auraPlayer) auraPlayer.ended();}"
                + "function report(){if(player&&window.auraPlayer) auraPlayer.progress(player.getCurrentTime(),player.getDuration(),player.getPlayerState()===1);}"
                + "function auraResume(){requestedPlaying=true;if(player)player.playVideo()} function auraPause(){requestedPlaying=false;if(player)player.pauseVideo()} function auraStop(){requestedPlaying=false;if(player)player.stopVideo()} function auraSeek(s){pendingSeek=s;if(player)player.seekTo(s,true)}"
                + "</script></body></html>";
    }

    public static final class PlayerBridge {
        private final MainViewModel viewModel;
        PlayerBridge(MainViewModel viewModel) { this.viewModel = viewModel; }
        public void state(boolean playing) { viewModel.updateExternalPlayback(viewModel.currentTimeProperty().get(), viewModel.totalDurationProperty().get(), playing); }
        public void progress(double position, double duration, boolean playing) { viewModel.updateExternalPlayback(position, duration, playing); }
        public void ended() { viewModel.updateExternalPlayback(viewModel.totalDurationProperty().get(), viewModel.totalDurationProperty().get(), false); }
    }
}
