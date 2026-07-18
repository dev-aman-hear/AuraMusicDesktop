package aura.music.model;

import java.io.Serializable;

public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String path;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private double duration; // in seconds
    private int trackNumber;
    private String year;
    private int bitRate;      // kbps
    private int sampleRate;  // Hz (e.g. 44100, 96000)
    private int bitsPerSample; // e.g. 16, 24, 32 (for Hi-Res detection)
    private boolean favorite;
    private String embeddedLyrics;
    private long dateAdded;
    private long lastModified;
    private String artworkUrl;
    private String externalUrl;

    public Song(String path) {
        this.path = path;
        this.title = getFilenameWithoutExtension(path);
        this.artist = "Unknown Artist";
        this.album = "Unknown Album";
        this.genre = "Unknown";
        this.dateAdded = System.currentTimeMillis();
    }

    private String getFilenameWithoutExtension(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String filename = lastSlash == -1 ? path : path.substring(lastSlash + 1);
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? filename : filename.substring(0, lastDot);
    }

    // Getters and Setters
    public String getPath() { return path; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public int getTrackNumber() { return trackNumber; }
    public void setTrackNumber(int trackNumber) { this.trackNumber = trackNumber; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public int getBitRate() { return bitRate; }
    public void setBitRate(int bitRate) { this.bitRate = bitRate; }

    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }

    public int getBitsPerSample() { return bitsPerSample; }
    public void setBitsPerSample(int bitsPerSample) { this.bitsPerSample = bitsPerSample; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public String getEmbeddedLyrics() { return embeddedLyrics; }
    public void setEmbeddedLyrics(String embeddedLyrics) { this.embeddedLyrics = embeddedLyrics; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getArtworkUrl() { return artworkUrl; }
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public boolean isHiRes() {
        return sampleRate > 48000 || bitsPerSample > 16;
    }

    @Override
    public String toString() {
        return artist + " - " + title + (isHiRes() ? " [Hi-Res]" : "");
    }
}
