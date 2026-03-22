package com.musicplayer.service;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import jakarta.annotation.PostConstruct;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.musicplayer.model.Playlist;
import com.musicplayer.repository.PlaylistRepository;

@Service
public class MusicService {

    @Value("${music.data.path:./music-data}")
    private String musicDataPath;

    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;

    public MusicService(SongRepository songRepository, PlaylistRepository playlistRepository) {
        this.songRepository = songRepository;
        this.playlistRepository = playlistRepository;
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    @PostConstruct
    public void scanMusicFiles() {
        Path audioDir = Paths.get(musicDataPath, "audio");
        if (!Files.exists(audioDir)) {
            System.out.println("[MusicService] 音频目录不存在: " + audioDir.toAbsolutePath());
            return;
        }

        try (var stream = Files.list(audioDir)) {
            List<Path> audioFiles = stream
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".mp3") || name.endsWith(".flac")
                                || name.endsWith(".wav") || name.endsWith(".ogg")
                                || name.endsWith(".m4a");
                    })
                    .sorted()
                    .collect(Collectors.toList());

            Set<String> diskFileNames = audioFiles.stream()
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());

            Map<String, Song> dbSongs = songRepository.findAll().stream()
                    .collect(Collectors.toMap(Song::getAudioFile, s -> s, (a, b) -> a));

            // 删除已不存在的文件
            for (Song dbSong : dbSongs.values()) {
                if (!diskFileNames.contains(dbSong.getAudioFile())) {
                    songRepository.delete(dbSong);
                    System.out.println("[MusicService] 已移除: " + dbSong.getTitle());
                }
            }

            // 新增或更新歌曲
            for (Path audioFile : audioFiles) {
                String fileName = audioFile.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                Song existing = dbSongs.get(fileName);
                if (existing != null) {
                    // 已有歌曲：只更新外部歌词/封面文件关联（不重新读 ID3，避免覆盖用户修改）
                    String lyricsFile = findFile(Paths.get(musicDataPath, "lyrics"), baseName, ".lrc", ".elrc", ".ttml");
                    String coverFile = findFile(Paths.get(musicDataPath, "covers"), baseName, ".jpg", ".png", ".jpeg", ".webp");
                    if (lyricsFile != null) existing.setLyricsFile(lyricsFile);
                    if (coverFile != null) existing.setCoverFile(coverFile);
                    songRepository.save(existing);
                } else {
                    // 新歌曲：读取 ID3 标签入库
                    Song song = createSongFromFile(audioFile);
                    songRepository.save(song);
                    System.out.println("[MusicService] 新增: " + song.getTitle()
                            + " | 艺术家: " + song.getArtist()
                            + " | 专辑: " + song.getAlbum()
                            + " | 时长: " + song.getDuration() + "s"
                            + " | 内嵌封面: " + (song.isHasEmbeddedCover() ? "✓" : "✗")
                            + " | 歌词: " + (song.getLyricsFile() != null ? "✓" : "✗"));
                }
            }

            System.out.println("[MusicService] 数据库共 " + songRepository.count() + " 首歌曲");

        } catch (IOException e) {
            System.err.println("[MusicService] 扫描失败: " + e.getMessage());
        }
    }

    /** 从音频文件创建 Song 对象（读取 ID3/FLAC 标签） */
    private Song createSongFromFile(Path audioFile) {
        String fileName = audioFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String id = String.valueOf(Math.abs(fileName.hashCode()) % 100000 + 1);

        // 确保 ID 不冲突
        while (songRepository.existsById(id)) {
            id = String.valueOf(Integer.parseInt(id) + 1);
        }

        // 外部歌词和封面文件
        String lyricsFile = findFile(Paths.get(musicDataPath, "lyrics"), baseName, ".lrc", ".elrc", ".ttml");
        String coverFile = findFile(Paths.get(musicDataPath, "covers"), baseName, ".jpg", ".png", ".jpeg", ".webp");

        // 默认值（文件名做标题）
        String title = baseName;
        String artist = "";
        String album = "";
        String genre = "";
        String year = "";
        int duration = 0;
        boolean hasEmbeddedCover = false;
        boolean hasEmbeddedLyrics = false;

        try {
            AudioFile af = AudioFileIO.read(audioFile.toFile());

            // 读取时长
            duration = af.getAudioHeader().getTrackLength();

            // 读取标签
            Tag tag = af.getTag();
            if (tag != null) {
                String t;

                t = tag.getFirst(FieldKey.TITLE);
                if (t != null && !t.isBlank()) title = t.trim();

                t = tag.getFirst(FieldKey.ARTIST);
                if (t != null && !t.isBlank()) artist = t.trim();

                t = tag.getFirst(FieldKey.ALBUM);
                if (t != null && !t.isBlank()) album = t.trim();

                t = tag.getFirst(FieldKey.GENRE);
                if (t != null && !t.isBlank()) genre = t.trim();

                t = tag.getFirst(FieldKey.YEAR);
                if (t != null && !t.isBlank()) year = t.trim();

                // 内嵌封面
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null && artwork.getBinaryData() != null && artwork.getBinaryData().length > 0) {
                    hasEmbeddedCover = true;
                    // 如果没有外部封面文件，把内嵌封面导出到 covers 目录
                    if (coverFile == null) {
                        coverFile = exportEmbeddedCover(baseName, artwork);
                    }
                }

                // 内嵌歌词（LYRICS / UNSYNC_LYRICS）
                t = tag.getFirst(FieldKey.LYRICS);
                if (t != null && !t.isBlank()) {
                    hasEmbeddedLyrics = true;
                    // 如果没有外部歌词文件，把内嵌歌词导出到 lyrics 目录
                    if (lyricsFile == null) {
                        lyricsFile = exportEmbeddedLyrics(baseName, t);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MusicService] 读取标签失败 (" + fileName + "): " + e.getMessage());
        }

        Song song = new Song(id, title, artist, album, fileName, lyricsFile, coverFile, duration);
        song.setGenre(genre);
        song.setYear(year);
        song.setHasEmbeddedCover(hasEmbeddedCover);
        song.setHasEmbeddedLyrics(hasEmbeddedLyrics);
        song.setCreatedAt(System.currentTimeMillis());
        return song;
    }

    /** 导出内嵌封面到 covers 目录 */
    private String exportEmbeddedCover(String baseName, Artwork artwork) {
        try {
            Path coverDir = Paths.get(musicDataPath, "covers");
            Files.createDirectories(coverDir);

            String mimeType = artwork.getMimeType();
            String ext = ".jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) ext = ".png";
                else if (mimeType.contains("webp")) ext = ".webp";
            }

            String coverFileName = baseName + ext;
            Path coverPath = coverDir.resolve(coverFileName);
            if (!Files.exists(coverPath)) {
                Files.write(coverPath, artwork.getBinaryData());
                System.out.println("[MusicService] 导出内嵌封面: " + coverFileName);
            }
            return coverFileName;
        } catch (Exception e) {
            System.err.println("[MusicService] 导出封面失败: " + e.getMessage());
            return null;
        }
    }

    /** 导出内嵌歌词到 lyrics 目录 */
    private String exportEmbeddedLyrics(String baseName, String lyricsContent) {
        try {
            Path lyricsDir = Paths.get(musicDataPath, "lyrics");
            Files.createDirectories(lyricsDir);

            String lyricsFileName = baseName + ".lrc";
            Path lyricsPath = lyricsDir.resolve(lyricsFileName);
            if (!Files.exists(lyricsPath)) {
                Files.writeString(lyricsPath, lyricsContent);
                System.out.println("[MusicService] 导出内嵌歌词: " + lyricsFileName);
            }
            return lyricsFileName;
        } catch (Exception e) {
            System.err.println("[MusicService] 导出歌词失败: " + e.getMessage());
            return null;
        }
    }

    private String findFile(Path dir, String baseName, String... extensions) {
        if (!Files.exists(dir)) return null;
        for (String ext : extensions) {
            Path file = dir.resolve(baseName + ext);
            if (Files.exists(file)) return file.getFileName().toString();
        }
        return null;
    }

    public List<Song> getAllSongs() { return songRepository.findAll(); }
    public Optional<Song> getSongById(String id) { return songRepository.findById(id); }
    public Song saveSong(Song song) { return songRepository.save(song); }

    public Path getAudioPath(String fileName) { return Paths.get(musicDataPath, "audio", fileName); }
    public Path getLyricsPath(String fileName) { return Paths.get(musicDataPath, "lyrics", fileName); }
    public Path getCoverPath(String fileName) { return Paths.get(musicDataPath, "covers", fileName); }


    /** 暴露 audio 目录路径 */
    public String getAudioDir() {
        return Paths.get(musicDataPath, "audio").toString();
    }

    /** 公开扫描方法，上传后调用 */
    public void scanMusic() {
        scanMusicFiles();
    }

    /** 删除歌曲及其关联文件 */
    public void deleteSong(String id) {
        songRepository.findById(id).ifPresent(song -> {
            // 从所有歌单中移除该歌曲
            List<Playlist> playlists = playlistRepository.findAll();
            for (Playlist pl : playlists) {
                if (pl.getSongIds().remove(song.getId())) {
                    playlistRepository.save(pl);
                }
            }

            try { Files.deleteIfExists(Paths.get(musicDataPath, "audio", song.getAudioFile())); } catch (Exception ignored) {}
            if (song.getCoverFile() != null) {
                try { Files.deleteIfExists(Paths.get(musicDataPath, "covers", song.getCoverFile())); } catch (Exception ignored) {}
            }
            if (song.getLyricsFile() != null) {
                try { Files.deleteIfExists(Paths.get(musicDataPath, "lyrics", song.getLyricsFile())); } catch (Exception ignored) {}
            }
            songRepository.delete(song);
            System.out.println("[MusicService] 已删除歌曲: " + song.getTitle());
        });
    }


    /** admin 管理 */
    public Path getCoverDir() {
        return Paths.get(musicDataPath, "covers");
    }

    public Path getLyricsDir() {
        return Paths.get(musicDataPath, "lyrics");
    }


}
