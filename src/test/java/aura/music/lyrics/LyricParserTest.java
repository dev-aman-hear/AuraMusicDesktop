package aura.music.lyrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LyricParserTest {

    @Test
    void parsesUnsynchronisedLyricsReturnedByOnlineProvider() {
        List<LyricLine> lyrics = LyricParser.parse("First line\n\nSecond line\nThird line");

        assertEquals(3, lyrics.size());
        assertEquals("First line", lyrics.get(0).getText());
        assertEquals(-1, lyrics.get(0).getTimestamp());
        assertEquals(-1, lyrics.get(1).getTimestamp());
    }

    @Test
    void ignoresMetadataWhenParsingUnsynchronisedLyrics() {
        List<LyricLine> lyrics = LyricParser.parse("[ar:Artist]\n[ti:Title]\nFirst line");

        assertEquals(1, lyrics.size());
        assertEquals("First line", lyrics.get(0).getText());
    }

    @Test
    void retainsTimestampsWhenLyricsAreSynchronised() {
        List<LyricLine> lyrics = LyricParser.parse("[00:01.25]First line\n[00:05.00]Second line");

        assertEquals(2, lyrics.size());
        assertEquals(1_250, lyrics.get(0).getTimestamp());
        assertEquals(5_000, lyrics.get(1).getTimestamp());
    }
}
