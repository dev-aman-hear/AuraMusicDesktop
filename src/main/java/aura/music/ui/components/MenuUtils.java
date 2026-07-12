package aura.music.ui.components;

import aura.music.library.LibraryManager;
import aura.music.model.Playlist;
import aura.music.model.Song;
import aura.music.viewmodel.MainViewModel;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class MenuUtils {

    public static List<MenuItem> getSongContextMenuItems(Song song, MainViewModel viewModel) {
        List<MenuItem> items = new ArrayList<>();
        if (song == null || viewModel == null) {
            return items;
        }

        // 1. Add / Remove from Favorite
        MenuItem favoriteItem = new MenuItem(song.isFavorite() ? "Remove from Favorites" : "Add to Favorites");
        favoriteItem.setStyle("-fx-text-fill: white;");
        favoriteItem.setOnAction(e -> viewModel.toggleFavorite(song));

        // 2. Add to Playlist
        Menu playlistMenu = new Menu("Add to Playlist...");
        playlistMenu.setStyle("-fx-text-fill: white;");
        
        var playlists = LibraryManager.getInstance().getPlaylists();
        if (playlists.isEmpty()) {
            MenuItem emptyItem = new MenuItem("No playlists available");
            emptyItem.setDisable(true);
            playlistMenu.getItems().add(emptyItem);
        } else {
            for (Playlist playlist : playlists) {
                MenuItem item = new MenuItem(playlist.getName());
                item.setStyle("-fx-text-fill: white;");
                item.setOnAction(e -> {
                    if (!playlist.getSongs().contains(song)) {
                        playlist.addSong(song);
                        LibraryManager.getInstance().savePlaylists();
                    }
                });
                playlistMenu.getItems().add(item);
            }
        }

        // 3. Play Now
        MenuItem playItem = new MenuItem("Play Now");
        playItem.setStyle("-fx-text-fill: white;");
        playItem.setOnAction(e -> viewModel.play(song));

        // 4. Add to Queue
        MenuItem queueItem = new MenuItem("Add to Queue");
        queueItem.setStyle("-fx-text-fill: white;");
        queueItem.setOnAction(e -> {
            if (!viewModel.getQueue().contains(song)) {
                viewModel.getQueue().add(song);
            }
        });

        // 5. Sleep Timer
        Menu sleepMenu = new Menu("Sleep Timer");
        sleepMenu.setStyle("-fx-text-fill: white;");
        
        MenuItem sleepOff = new MenuItem("Off");
        sleepOff.setStyle("-fx-text-fill: white;");
        sleepOff.setOnAction(e -> viewModel.cancelSleepTimer());

        MenuItem sleep15 = new MenuItem("15 Minutes");
        sleep15.setStyle("-fx-text-fill: white;");
        sleep15.setOnAction(e -> viewModel.startSleepTimer(15));
        
        MenuItem sleep30 = new MenuItem("30 Minutes");
        sleep30.setStyle("-fx-text-fill: white;");
        sleep30.setOnAction(e -> viewModel.startSleepTimer(30));
        
        MenuItem sleep60 = new MenuItem("60 Minutes");
        sleep60.setStyle("-fx-text-fill: white;");
        sleep60.setOnAction(e -> viewModel.startSleepTimer(60));
        
        sleepMenu.getItems().addAll(sleepOff, sleep15, sleep30, sleep60);

        items.add(favoriteItem);
        items.add(playlistMenu);
        items.add(playItem);
        items.add(queueItem);
        items.add(sleepMenu);

        return items;
    }

    public static void showSongContextMenu(Node anchor, Song song, MainViewModel viewModel) {
        if (song == null || viewModel == null || anchor == null) {
            return;
        }

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: rgba(30, 30, 35, 0.75); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 12px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 8);");
        contextMenu.getItems().addAll(getSongContextMenuItems(song, viewModel));
        contextMenu.show(anchor, Side.BOTTOM, 0, 5);
    }
}
