package aura.music.library;

import aura.music.model.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;

public class MetadataExtractor {

    public static Song extract(File file) {
        Song song = new Song(file.getAbsolutePath());
        try {
            AudioFile f = AudioFileIO.read(file);
            AudioHeader header = f.getAudioHeader();
            if (header != null) {
                try {
                    song.setDuration(header.getTrackLength());
                } catch (Exception e) {}
                
                try {
                    song.setBitRate((int) header.getBitRateAsNumber());
                } catch (Exception e) {}
                
                try {
                    song.setSampleRate(header.getSampleRateAsNumber());
                } catch (Exception e) {}
                
                try {
                    int bitsPerSample = header.getBitsPerSample();
                    if (bitsPerSample > 0) {
                        song.setBitsPerSample(bitsPerSample);
                    } else {
                        String encoding = header.getEncodingType();
                        if (encoding != null && (encoding.contains("24") || encoding.toLowerCase().contains("24bit"))) {
                            song.setBitsPerSample(24);
                        } else if (encoding != null && (encoding.contains("32") || encoding.toLowerCase().contains("32bit"))) {
                            song.setBitsPerSample(32);
                        } else {
                            song.setBitsPerSample(16);
                        }
                    }
                } catch (Exception e) {
                    song.setBitsPerSample(16);
                }
            }

            Tag tag = f.getTag();
            if (tag != null) {
                populateSongFromTag(song, tag);
            }
        } catch (Exception e) {
            boolean fallbackSuccess = false;
            // Fallback for WAV files if jaudiotagger fails due to the header parsing bug
            if (file.getName().toLowerCase().endsWith(".wav")) {
                try {
                    byte[] id3Bytes = extractWavId3Chunk(file);
                    if (id3Bytes != null && id3Bytes.length > 10 && 
                        id3Bytes[0] == 'I' && id3Bytes[1] == 'D' && id3Bytes[2] == '3') {
                        int version = id3Bytes[3] & 0xFF;
                        Tag tag = null;
                        if (version == 2) {
                            tag = new org.jaudiotagger.tag.id3.ID3v22Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        } else if (version == 3) {
                            tag = new org.jaudiotagger.tag.id3.ID3v23Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        } else if (version == 4) {
                            tag = new org.jaudiotagger.tag.id3.ID3v24Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        }
                        if (tag != null) {
                            populateSongFromTag(song, tag);
                            fallbackSuccess = true;
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("WAV metadata fallback failed: " + ex.getMessage());
                }
            }
            // Fallback for M4A files if jaudiotagger fails due to the header parsing bug (ALAC)
            else if (file.getName().toLowerCase().endsWith(".m4a")) {
                try {
                    Tag tag = new org.jaudiotagger.audio.mp4.Mp4TagReader().read(file.toPath());
                    if (tag != null) {
                        populateSongFromTag(song, tag);
                        fallbackSuccess = true;
                    }
                } catch (Exception ex) {
                    System.err.println("M4A metadata fallback failed: " + ex.getMessage());
                }
            }
            
            if (!fallbackSuccess) {
                System.err.println("Error extracting metadata for: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }

        // Fallback for WAV files to extract true sample rate and bit depth
        if (file.getName().toLowerCase().endsWith(".wav") && (song.getSampleRate() <= 0 || song.getBitsPerSample() <= 0)) {
            try {
                javax.sound.sampled.AudioFileFormat baseFormat = javax.sound.sampled.AudioSystem.getAudioFileFormat(file);
                javax.sound.sampled.AudioFormat format = baseFormat.getFormat();
                if (song.getSampleRate() <= 0) {
                    song.setSampleRate((int) format.getSampleRate());
                }
                if (song.getBitsPerSample() <= 0) {
                    song.setBitsPerSample(format.getSampleSizeInBits());
                }
                if (song.getBitRate() <= 0 && format.getSampleRate() > 0 && format.getSampleSizeInBits() > 0) {
                    int channels = format.getChannels();
                    if (channels <= 0) channels = 2;
                    song.setBitRate((int) (format.getSampleRate() * format.getSampleSizeInBits() * channels / 1000));
                }
            } catch (Exception ignored) {}
        }

        return song;
    }

    private static void populateSongFromTag(Song song, Tag tag) {
        String title = tag.getFirst(FieldKey.TITLE);
        if (title != null && !title.trim().isEmpty() && 
            !title.equalsIgnoreCase("unknown") && !title.equalsIgnoreCase("unknown title")) {
            song.setTitle(title);
        }
        
        String artist = tag.getFirst(FieldKey.ARTIST);
        if (artist != null && !artist.trim().isEmpty() && !artist.equalsIgnoreCase("unknown")) {
            song.setArtist(artist);
        }

        String album = tag.getFirst(FieldKey.ALBUM);
        if (album != null && !album.trim().isEmpty() && !album.equalsIgnoreCase("unknown")) {
            song.setAlbum(album);
        }

        String genre = tag.getFirst(FieldKey.GENRE);
        if (genre != null && !genre.trim().isEmpty()) {
            song.setGenre(genre);
        }

        String year = tag.getFirst(FieldKey.YEAR);
        if (year != null && !year.trim().isEmpty()) {
            song.setYear(year);
        }

        String track = tag.getFirst(FieldKey.TRACK);
        if (track != null && !track.trim().isEmpty()) {
            try {
                song.setTrackNumber(Integer.parseInt(track.split("/")[0].trim()));
            } catch (NumberFormatException ignored) {}
        }

        // Extract embedded lyrics, checking multiple common tag names
        String lyrics = tag.getFirst(FieldKey.LYRICS);
        if (lyrics == null || lyrics.trim().isEmpty()) {
            lyrics = tag.getFirst("UNSYNCEDLYRICS");
        }
        if (lyrics == null || lyrics.trim().isEmpty()) {
            lyrics = tag.getFirst("UNSYNCED LYRICS");
        }
        if (lyrics == null || lyrics.trim().isEmpty()) {
            lyrics = tag.getFirst("LYRICS");
        }
        
        if (lyrics != null && !lyrics.trim().isEmpty()) {
            song.setEmbeddedLyrics(lyrics);
        }
    }

    public static byte[] extractArtworkBytes(String path) {
        try {
            File file = new File(path);
            AudioFile f = AudioFileIO.read(file);
            Tag tag = f.getTag();
            if (tag != null) {
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    return artwork.getBinaryData();
                }
            }
        } catch (Exception e) {
            // Fallback for WAV files if jaudiotagger fails due to the header parsing bug
            if (path.toLowerCase().endsWith(".wav")) {
                try {
                    byte[] id3Bytes = extractWavId3Chunk(new File(path));
                    if (id3Bytes != null && id3Bytes.length > 10 && 
                        id3Bytes[0] == 'I' && id3Bytes[1] == 'D' && id3Bytes[2] == '3') {
                        int version = id3Bytes[3] & 0xFF;
                        Tag tag = null;
                        if (version == 2) {
                            tag = new org.jaudiotagger.tag.id3.ID3v22Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        } else if (version == 3) {
                            tag = new org.jaudiotagger.tag.id3.ID3v23Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        } else if (version == 4) {
                            tag = new org.jaudiotagger.tag.id3.ID3v24Tag(java.nio.ByteBuffer.wrap(id3Bytes), "");
                        }
                        if (tag != null) {
                            Artwork artwork = tag.getFirstArtwork();
                            if (artwork != null) {
                                return artwork.getBinaryData();
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("WAV artwork fallback failed: " + ex.getMessage());
                }
            }
            // Fallback for M4A files if jaudiotagger fails due to the header parsing bug (ALAC)
            else if (path.toLowerCase().endsWith(".m4a")) {
                try {
                    Tag tag = new org.jaudiotagger.audio.mp4.Mp4TagReader().read(new File(path).toPath());
                    if (tag != null) {
                        Artwork artwork = tag.getFirstArtwork();
                        if (artwork != null) {
                            return artwork.getBinaryData();
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("M4A artwork fallback failed: " + ex.getMessage());
                }
            }
            
            // Only log if we didn't successfully return via one of the fallbacks
            System.err.println("Error extracting artwork: " + e.getMessage());
        }
        return null;
    }

    private static byte[] extractWavId3Chunk(File file) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            if (raf.length() < 12) return null;
            byte[] header = new byte[12];
            raf.readFully(header);
            if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F' ||
                header[8] != 'W' || header[9] != 'A' || header[10] != 'V' || header[11] != 'E') {
                return null;
            }
            long fileLength = raf.length();
            long pos = 12;
            while (pos + 8 <= fileLength) {
                raf.seek(pos);
                byte[] chunkIdBytes = new byte[4];
                raf.readFully(chunkIdBytes);
                String chunkId = new String(chunkIdBytes, java.nio.charset.StandardCharsets.US_ASCII);
                
                byte[] sizeBytes = new byte[4];
                raf.readFully(sizeBytes);
                long chunkSize = ((sizeBytes[0] & 0xFFL)) |
                                 ((sizeBytes[1] & 0xFFL) << 8) |
                                 ((sizeBytes[2] & 0xFFL) << 16) |
                                 ((sizeBytes[3] & 0xFFL) << 24);
                
                pos += 8;
                if (chunkId.equals("id3 ")) {
                    byte[] id3Bytes = new byte[(int) chunkSize];
                    raf.readFully(id3Bytes);
                    return id3Bytes;
                }
                
                pos += chunkSize;
                if (chunkSize % 2 != 0) {
                    pos++; // 1-byte padding for odd-sized chunks
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing WAV RIFF chunks: " + e.getMessage());
        }
        return null;
    }
}
