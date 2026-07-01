package aura.music.lyrics;

import java.util.ArrayList;
import java.util.List;

public class LyricLine {
    private final long timestamp; // in milliseconds
    private final String text;
    private final List<WordTime> words;

    public static class WordTime {
        private final long timestamp; // offset or absolute in ms
        private final String word;

        public WordTime(long timestamp, String word) {
            this.timestamp = timestamp;
            this.word = word;
        }

        public long getTimestamp() { return timestamp; }
        public String getWord() { return word; }
    }

    public LyricLine(long timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
        this.words = new ArrayList<>();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    public List<WordTime> getWords() {
        return words;
    }

    public void addWord(long timestamp, String word) {
        words.add(new WordTime(timestamp, word));
    }

    public boolean hasWordTimestamps() {
        return !words.isEmpty();
    }
}
