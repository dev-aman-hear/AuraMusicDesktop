package aura.music.model;

import aura.music.library.LibraryManager;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SmartPlaylist extends Playlist {
    
    private transient Predicate<Song> rule;
    private String ruleDescription;

    public SmartPlaylist(String id, String name, String ruleDescription, Predicate<Song> rule) {
        super(id, name);
        this.ruleDescription = ruleDescription;
        this.rule = rule;
    }
    
    public void setRule(Predicate<Song> rule) {
        this.rule = rule;
    }
    
    public String getRuleDescription() {
        return ruleDescription;
    }

    @Override
    public List<Song> getSongs() {
        if (rule != null) {
            return LibraryManager.getInstance().getSongs().stream()
                .filter(rule)
                .collect(Collectors.toList());
        }
        return super.getSongs();
    }
    
    @Override
    public void addSong(Song song) {
        // Smart playlists are dynamic, manual additions are ignored
    }
    
    @Override
    public void removeSong(Song song) {
        // Smart playlists are dynamic, manual removals are ignored
    }
}
