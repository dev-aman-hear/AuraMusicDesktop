package aura.music.lyrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParser {

    // Pattern to match timestamp: [mm:ss], [mm:ss.xx], [hh:mm:ss], [hh:mm:ss.xxx],
    // [m:ss], etc.
    // Group 1: Optional hours, Group 2: Minutes, Group 3: Seconds, Group 4:
    // Optional milliseconds/hundredths
    private static final Pattern TIME_PATTERN = Pattern
            .compile("\\[(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]");
    // Pattern to match word timestamp like <00:12.50>
    private static final Pattern WORD_TIME_PATTERN = Pattern.compile("<(\\d{2}):(\\d{2})[.:](\\d{2,3})>");

    public static List<LyricLine> parse(File file) {
        List<LyricLine> lines = new ArrayList<>();
        try (BufferedReader reader = new FileInputReader(file)) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Strip UTF-8 BOM if present
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                }
                parseLine(line, lines);
            }
        } catch (Exception e) {
            System.err.println("Error parsing lyric file: " + e.getMessage());
        }
        sortLyrics(lines);
        return lines;
    }

    public static List<LyricLine> parse(String content) {
        List<LyricLine> lines = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                }
                parseLine(line, lines);
            }
        } catch (Exception e) {
            System.err.println("Error parsing lyric content: " + e.getMessage());
        }
        sortLyrics(lines);
        return lines;
    }

    private static void parseLine(String rawLine, List<LyricLine> lines) {
        rawLine = rawLine.trim();
        if (rawLine.isEmpty())
            return;

        // Skip metadata tags like [ar:Artist]
        if (rawLine.startsWith("[") && !Character.isDigit(rawLine.charAt(1))) {
            return;
        }

        // Find all timestamps at the beginning of the line
        Matcher matcher = TIME_PATTERN.matcher(rawLine);
        List<Long> timestamps = new ArrayList<>();
        int lastIndex = 0;

        while (matcher.find()) {
            long ms = parseTimestampToMs(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            timestamps.add(ms);
            lastIndex = matcher.end();
        }

        if (timestamps.isEmpty())
            return;

        // The rest of the line is the lyric text
        String lyricText = rawLine.substring(lastIndex).trim();

        for (long ts : timestamps) {
            LyricLine lyricLine = new LyricLine(ts, cleanWordTimestamps(lyricText));
            // Parse word-by-word timestamps if present
            parseWordTimestamps(lyricText, lyricLine);
            lines.add(lyricLine);
        }
    }

    private static long parseTimestampToMs(String hourStr, String minStr, String secStr, String msStr) {
        long hours = hourStr != null ? Long.parseLong(hourStr) : 0;
        long minutes = Long.parseLong(minStr);
        long seconds = Long.parseLong(secStr);
        long ms = 0;
        if (msStr != null) {
            if (msStr.length() == 2) {
                ms = Long.parseLong(msStr) * 10; // .45 -> 450ms
            } else if (msStr.length() == 3) {
                ms = Long.parseLong(msStr); // .450 -> 450ms
            }
        }
        return (((hours * 60) + minutes) * 60 + seconds) * 1000 + ms;
    }

    private static String cleanWordTimestamps(String text) {
        return text.replaceAll("<\\d{2}:\\d{2}[.:]\\d{2,3}>", "");
    }

    private static void parseWordTimestamps(String text, LyricLine lyricLine) {
        Matcher matcher = WORD_TIME_PATTERN.matcher(text);
        int lastEnd = 0;

        // Add the first word (starts at the beginning of the line)
        long currentWordStart = lyricLine.getTimestamp();

        while (matcher.find()) {
            String word = text.substring(lastEnd, matcher.start()).trim();
            if (!word.isEmpty()) {
                lyricLine.addWord(currentWordStart, word);
            }
            // For word timestamps, they usually don't have hours, but let's parse using
            // 3-argument helper
            currentWordStart = parseTimestampToMs(null, matcher.group(1), matcher.group(2), matcher.group(3));
            lastEnd = matcher.end();
        }

        // Add the last word
        String lastWord = text.substring(lastEnd).trim();
        if (!lastWord.isEmpty()) {
            lyricLine.addWord(currentWordStart, lastWord);
        }
    }

    private static void sortLyrics(List<LyricLine> lines) {
        lines.sort(Comparator.comparingLong(LyricLine::getTimestamp));
    }

    // Custom helper class to handle file reading safely in UTF-8
    private static class FileInputReader extends BufferedReader {
        public FileInputReader(File file) throws Exception {
            super(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        }
    }
}
