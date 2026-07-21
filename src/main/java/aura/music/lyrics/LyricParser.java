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

    // Matches standard LRC timestamps:
    // Group 1: Minutes / Hours
    // Group 2: Seconds / Minutes
    // Group 3: Optional Seconds (if 3-part)
    // Group 4: Optional fractional seconds / hundredths
    private static final Pattern TIME_PATTERN = Pattern
            .compile("\\[\\s*(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?(?:[.:,](\\d{1,3}))?\\s*\\]");

    // Pattern to match word timestamp like <00:12.50>
    private static final Pattern WORD_TIME_PATTERN = Pattern.compile("<(\\d{1,2}):(\\d{1,2})[.:,](\\d{1,3})>");

    public static List<LyricLine> parse(File file) {
        List<LyricLine> lines = new ArrayList<>();
        List<String> rawLines = new ArrayList<>();
        long fileOffsetMs = 0;

        try (BufferedReader reader = new FileInputReader(file)) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                }
                String trimmed = line.trim();
                if (trimmed.toLowerCase().startsWith("[offset:")) {
                    fileOffsetMs = parseOffsetHeader(trimmed);
                    continue;
                }
                rawLines.add(line);
                parseLine(line, lines, fileOffsetMs);
            }
        } catch (Exception e) {
            System.err.println("Error parsing lyric file: " + e.getMessage());
        }
        sortLyrics(lines);
        return lines.isEmpty() ? parsePlainLyrics(rawLines) : lines;
    }

    public static List<LyricLine> parse(String content) {
        List<LyricLine> lines = new ArrayList<>();
        List<String> rawLines = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return lines;
        }
        long fileOffsetMs = 0;

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
                String trimmed = line.trim();
                if (trimmed.toLowerCase().startsWith("[offset:")) {
                    fileOffsetMs = parseOffsetHeader(trimmed);
                    continue;
                }
                rawLines.add(line);
                parseLine(line, lines, fileOffsetMs);
            }
        } catch (Exception e) {
            System.err.println("Error parsing lyric content: " + e.getMessage());
        }
        sortLyrics(lines);
        return lines.isEmpty() ? parsePlainLyrics(rawLines) : lines;
    }

    private static long parseOffsetHeader(String line) {
        try {
            int colon = line.indexOf(':');
            int bracket = line.indexOf(']');
            if (colon != -1 && bracket > colon) {
                String val = line.substring(colon + 1, bracket).trim();
                return Long.parseLong(val);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * Unsynchronised lyrics do not have timestamps. Display them statically
     * without fake timers.
     */
    private static List<LyricLine> parsePlainLyrics(List<String> rawLines) {
        List<LyricLine> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String text = rawLine.trim();
            if (text.isEmpty() || (text.startsWith("[") && text.endsWith("]") && !text.contains(" "))) {
                continue;
            }
            lines.add(new LyricLine(-1, text));
        }
        return lines;
    }

    private static void parseLine(String rawLine, List<LyricLine> lines, long fileOffsetMs) {
        rawLine = rawLine.trim();
        if (rawLine.isEmpty())
            return;

        Matcher matcher = TIME_PATTERN.matcher(rawLine);
        List<Long> timestamps = new ArrayList<>();
        int lastIndex = 0;

        while (matcher.find()) {
            long ms = parseTimestampToMs(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)) + fileOffsetMs;
            timestamps.add(Math.max(0, ms));
            lastIndex = matcher.end();
        }

        if (timestamps.isEmpty())
            return;

        // The rest of the line is the lyric text
        String lyricText = rawLine.substring(lastIndex).trim();

        for (long ts : timestamps) {
            LyricLine lyricLine = new LyricLine(ts, cleanWordTimestamps(lyricText));
            parseWordTimestamps(lyricText, lyricLine, fileOffsetMs);
            lines.add(lyricLine);
        }
    }

    private static long parseTimestampToMs(String g1, String g2, String g3, String g4) {
        long p1 = Long.parseLong(g1);
        long p2 = Long.parseLong(g2);
        long p3 = g3 != null ? Long.parseLong(g3) : -1;
        long ms = 0;

        if (g4 != null) {
            // Format: [p1:p2:p3.g4] (hours:minutes:seconds.ms) or [p1:p2.g4] (minutes:seconds.ms)
            if (p3 != -1) {
                long hours = p1, min = p2, sec = p3;
                ms = parseFractionalMs(g4);
                return (((hours * 60) + min) * 60 + sec) * 1000 + ms;
            } else {
                long min = p1, sec = p2;
                ms = parseFractionalMs(g4);
                return (min * 60 + sec) * 1000 + ms;
            }
        } else {
            if (p3 != -1) {
                // Format: [p1:p2:p3]
                // In standard LRC, [mm:ss:xx] uses colon for hundredths (p3 = hundredths)
                long min = p1, sec = p2;
                ms = p3 * 10;
                return (min * 60 + sec) * 1000 + ms;
            } else {
                // Format: [minutes:seconds]
                long min = p1, sec = p2;
                return (min * 60 + sec) * 1000;
            }
        }
    }

    private static long parseFractionalMs(String str) {
        if (str == null || str.isEmpty()) return 0;
        if (str.length() == 1) return Long.parseLong(str) * 100;
        if (str.length() == 2) return Long.parseLong(str) * 10;
        if (str.length() >= 3) return Long.parseLong(str.substring(0, 3));
        return 0;
    }

    private static String cleanWordTimestamps(String text) {
        return text.replaceAll("<\\d{1,2}:\\d{1,2}[.:,]\\d{1,3}>", "");
    }

    private static void parseWordTimestamps(String text, LyricLine lyricLine, long fileOffsetMs) {
        Matcher matcher = WORD_TIME_PATTERN.matcher(text);
        int lastEnd = 0;

        long currentWordStart = lyricLine.getTimestamp();

        while (matcher.find()) {
            String word = text.substring(lastEnd, matcher.start()).trim();
            if (!word.isEmpty()) {
                lyricLine.addWord(currentWordStart, word);
            }
            currentWordStart = parseTimestampToMs(matcher.group(1), matcher.group(2), null, matcher.group(3)) + fileOffsetMs;
            lastEnd = matcher.end();
        }

        String lastWord = text.substring(lastEnd).trim();
        if (!lastWord.isEmpty()) {
            lyricLine.addWord(currentWordStart, lastWord);
        }
    }

    private static void sortLyrics(List<LyricLine> lines) {
        lines.sort(Comparator.comparingLong(LyricLine::getTimestamp));
    }

    private static class FileInputReader extends BufferedReader {
        public FileInputReader(File file) throws Exception {
            super(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        }
    }
}

