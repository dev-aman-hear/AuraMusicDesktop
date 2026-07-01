package aura.music.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private final List<Song> songs;
    private String customCoverPath;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void addSong(Song song) {
        if (!songs.contains(song)) {
            songs.add(song);
        }
    }

    public void addSongAt(int index, Song song) {
        if (index >= 0 && index <= songs.size()) {
            songs.remove(song); // Avoid duplicates
            songs.add(index, song);
        }
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public String getCustomCoverPath() {
        return customCoverPath;
    }

    public void setCustomCoverPath(String customCoverPath) {
        this.customCoverPath = customCoverPath;
    }

    public void moveSong(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < songs.size() && toIndex >= 0 && toIndex < songs.size()) {
            Song song = songs.remove(fromIndex);
            songs.add(toIndex, song);
        }
    }
}
