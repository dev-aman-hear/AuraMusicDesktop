package aura.music.library;

import aura.music.model.Playlist;
import aura.music.model.Song;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final String dbUrl;

    private DatabaseManager() {
        String userHome = System.getProperty("user.home");
        String appDataPath = userHome + File.separator + ".auramusic";
        new File(appDataPath).mkdirs();
        this.dbUrl = "jdbc:sqlite:" + appDataPath + File.separator + "library.db";
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        String createSongsTable = """
            CREATE TABLE IF NOT EXISTS songs (
                path TEXT PRIMARY KEY,
                title TEXT,
                artist TEXT,
                album TEXT,
                genre TEXT,
                duration REAL,
                trackNumber INTEGER,
                year TEXT,
                bitRate INTEGER,
                sampleRate INTEGER,
                bitsPerSample INTEGER,
                favorite INTEGER,
                dateAdded INTEGER,
                lastModified INTEGER,
                embeddedLyrics TEXT,
                artworkUrl TEXT,
                externalUrl TEXT
            );
        """;

        String createPlaylistsTable = """
            CREATE TABLE IF NOT EXISTS playlists (
                id TEXT PRIMARY KEY,
                name TEXT
            );
        """;

        String createPlaylistSongsTable = """
            CREATE TABLE IF NOT EXISTS playlist_songs (
                playlist_id TEXT,
                song_path TEXT,
                list_order INTEGER,
                PRIMARY KEY (playlist_id, song_path),
                FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                FOREIGN KEY (song_path) REFERENCES songs(path) ON DELETE CASCADE
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
             
            // Enable foreign keys and memory mapping (30MB)
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA mmap_size = 30000000;");
            
            stmt.execute(createSongsTable);
            stmt.execute(createPlaylistsTable);
            stmt.execute(createPlaylistSongsTable);
            
            // FTS5 Virtual Table for Search Optimization
            String createFtsTable = """
                CREATE VIRTUAL TABLE IF NOT EXISTS songs_fts USING fts5(
                    path UNINDEXED,
                    title,
                    artist,
                    album,
                    tokenize='trigram'
                );
            """;
            stmt.execute(createFtsTable);

            // Triggers to keep FTS synchronized with songs table
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS songs_ai AFTER INSERT ON songs BEGIN
                    INSERT INTO songs_fts(rowid, path, title, artist, album)
                    VALUES (new.rowid, new.path, new.title, new.artist, new.album);
                END;
            """);

            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS songs_ad AFTER DELETE ON songs BEGIN
                    INSERT INTO songs_fts(songs_fts, rowid, path, title, artist, album)
                    VALUES ('delete', old.rowid, old.path, old.title, old.artist, old.album);
                END;
            """);

            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS songs_au AFTER UPDATE ON songs BEGIN
                    INSERT INTO songs_fts(songs_fts, rowid, path, title, artist, album)
                    VALUES ('delete', old.rowid, old.path, old.title, old.artist, old.album);
                    INSERT INTO songs_fts(rowid, path, title, artist, album)
                    VALUES (new.rowid, new.path, new.title, new.artist, new.album);
                END;
            """);

            // Seed FTS table if it's empty but songs exist (migration)
            stmt.execute("""
                INSERT INTO songs_fts(rowid, path, title, artist, album)
                SELECT rowid, path, title, artist, album FROM songs
                WHERE NOT EXISTS (SELECT 1 FROM songs_fts LIMIT 1);
            """);
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public void insertOrUpdateSong(Song song) {
        String sql = """
            INSERT INTO songs (
                path, title, artist, album, genre, duration, trackNumber, year,
                bitRate, sampleRate, bitsPerSample, favorite, dateAdded, lastModified,
                embeddedLyrics, artworkUrl, externalUrl
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(path) DO UPDATE SET
                title=excluded.title,
                artist=excluded.artist,
                album=excluded.album,
                genre=excluded.genre,
                duration=excluded.duration,
                trackNumber=excluded.trackNumber,
                year=excluded.year,
                bitRate=excluded.bitRate,
                sampleRate=excluded.sampleRate,
                bitsPerSample=excluded.bitsPerSample,
                favorite=excluded.favorite,
                lastModified=excluded.lastModified,
                embeddedLyrics=excluded.embeddedLyrics,
                artworkUrl=excluded.artworkUrl,
                externalUrl=excluded.externalUrl;
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, song.getPath());
            pstmt.setString(2, song.getTitle());
            pstmt.setString(3, song.getArtist());
            pstmt.setString(4, song.getAlbum());
            pstmt.setString(5, song.getGenre());
            pstmt.setDouble(6, song.getDuration());
            pstmt.setInt(7, song.getTrackNumber());
            pstmt.setString(8, song.getYear());
            pstmt.setInt(9, song.getBitRate());
            pstmt.setInt(10, song.getSampleRate());
            pstmt.setInt(11, song.getBitsPerSample());
            pstmt.setInt(12, song.isFavorite() ? 1 : 0);
            pstmt.setLong(13, song.getDateAdded());
            pstmt.setLong(14, song.getLastModified());
            pstmt.setString(15, song.getEmbeddedLyrics());
            pstmt.setString(16, song.getArtworkUrl());
            pstmt.setString(17, song.getExternalUrl());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to insert/update song: " + e.getMessage());
        }
    }

    public void removeSong(String path) {
        String sql = "DELETE FROM songs WHERE path = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to remove song: " + e.getMessage());
        }
    }

    public List<Song> getAllSongs() {
        return getSongs("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC");
    }

    public List<Song> searchSongs(String query) {
        String sql = """
            SELECT s.* FROM songs s
            JOIN songs_fts f ON s.rowid = f.rowid
            WHERE songs_fts MATCH ?
            ORDER BY rank
            LIMIT 100
        """;
        
        List<Song> songs = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            // SQLite FTS5 requires quotes around the query if it contains special characters, 
            // but a wildcard approach with exact match is generally safe:
            pstmt.setString(1, query + "*");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapRowToSong(rs));
                }
            }
        } catch (SQLException e) {
            // Fallback to standard LIKE if FTS query syntax error (e.g. user typed unescaped quotes)
            String fallbackSql = "SELECT * FROM songs WHERE title LIKE ? OR artist LIKE ? OR album LIKE ? ORDER BY title COLLATE NOCASE ASC LIMIT 100";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(fallbackSql)) {
                String likeQuery = "%" + query + "%";
                pstmt.setString(1, likeQuery);
                pstmt.setString(2, likeQuery);
                pstmt.setString(3, likeQuery);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        songs.add(mapRowToSong(rs));
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Failed to search songs: " + ex.getMessage());
            }
        }
        return songs;
    }

    private List<Song> getSongs(String sql) {
        List<Song> songs = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                songs.add(mapRowToSong(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get songs: " + e.getMessage());
        }
        return songs;
    }

    private Song mapRowToSong(ResultSet rs) throws SQLException {
        Song song = new Song(rs.getString("path"));
        song.setTitle(rs.getString("title"));
        song.setArtist(rs.getString("artist"));
        song.setAlbum(rs.getString("album"));
        song.setGenre(rs.getString("genre"));
        song.setDuration(rs.getDouble("duration"));
        song.setTrackNumber(rs.getInt("trackNumber"));
        song.setYear(rs.getString("year"));
        song.setBitRate(rs.getInt("bitRate"));
        song.setSampleRate(rs.getInt("sampleRate"));
        song.setBitsPerSample(rs.getInt("bitsPerSample"));
        song.setFavorite(rs.getInt("favorite") == 1);
        song.setDateAdded(rs.getLong("dateAdded"));
        song.setLastModified(rs.getLong("lastModified"));
        song.setEmbeddedLyrics(rs.getString("embeddedLyrics"));
        song.setArtworkUrl(rs.getString("artworkUrl"));
        song.setExternalUrl(rs.getString("externalUrl"));
        return song;
    }

    // Playlists
    public void savePlaylist(Playlist playlist) {
        String sql = "INSERT INTO playlists (id, name) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET name=excluded.name";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playlist.getId());
            pstmt.setString(2, playlist.getName());
            pstmt.executeUpdate();
            
            // Clear existing songs
            try (PreparedStatement clearStmt = conn.prepareStatement("DELETE FROM playlist_songs WHERE playlist_id = ?")) {
                clearStmt.setString(1, playlist.getId());
                clearStmt.executeUpdate();
            }
            
            // Insert songs
            String insertSongSql = "INSERT INTO playlist_songs (playlist_id, song_path, list_order) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSongSql)) {
                List<Song> songs = playlist.getSongs();
                for (int i = 0; i < songs.size(); i++) {
                    insertStmt.setString(1, playlist.getId());
                    insertStmt.setString(2, songs.get(i).getPath());
                    insertStmt.setInt(3, i);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Failed to save playlist: " + e.getMessage());
        }
    }

    public void removePlaylist(String id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM playlists WHERE id = ?")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to remove playlist: " + e.getMessage());
        }
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT id, name FROM playlists";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                String id = rs.getString("id");
                Playlist p = new Playlist(id, rs.getString("name"));
                
                // Load songs for this playlist
                String songsSql = """
                    SELECT s.* FROM songs s
                    JOIN playlist_songs ps ON s.path = ps.song_path
                    WHERE ps.playlist_id = ?
                    ORDER BY ps.list_order ASC
                """;
                try (PreparedStatement pst = conn.prepareStatement(songsSql)) {
                    pst.setString(1, id);
                    try (ResultSet srs = pst.executeQuery()) {
                        while (srs.next()) {
                            p.getSongs().add(mapRowToSong(srs));
                        }
                    }
                }
                playlists.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get playlists: " + e.getMessage());
        }
        return playlists;
    }
}
