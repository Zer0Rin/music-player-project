package com.musicplayer.service;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MusicService {

    @Value("${music.data.path:./music-data}")
    private String musicDataPath;

    private final SongRepository songRepository;

    public MusicService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /**
     * 启动时扫描 music-data/audio/ 目录
     * 新增的文件入库，已删除的文件从库中移除
     */
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
                                || name.endsWith(".wav") || name.endsWith(".ogg");
                    })
                    .sorted()
                    .collect(Collectors.toList());

            // 当前磁盘上的文件名集合
            Set<String> diskFileNames = audioFiles.stream()
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());

            // 数据库中已有的歌曲
            Map<String, Song> dbSongs = songRepository.findAll().stream()
                    .collect(Collectors.toMap(Song::getAudioFile, s -> s, (a, b) -> a));

            // 1. 删除数据库中已不存在于磁盘的歌曲
            for (Song dbSong : dbSongs.values()) {
                if (!diskFileNames.contains(dbSong.getAudioFile())) {
                    songRepository.delete(dbSong);
                    System.out.println("[MusicService] 已移除（文件不存在）: " + dbSong.getTitle());
                }
            }

            // 2. 新增或更新歌曲
            for (int i = 0; i < audioFiles.size(); i++) {
                Path audioFile = audioFiles.get(i);
                String fileName = audioFile.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                // 用文件名生成稳定 ID（不随文件排序变化）
                String id = String.valueOf(Math.abs(fileName.hashCode()) % 100000 + 1);

                String lyricsFile = findFile(Paths.get(musicDataPath, "lyrics"), baseName, ".lrc", ".elrc", ".ttml");
                String coverFile = findFile(Paths.get(musicDataPath, "covers"), baseName, ".jpg", ".png", ".jpeg", ".webp");

                Song existing = dbSongs.get(fileName);
                if (existing != null) {
                    // 只更新关联文件，不改 ID
                    existing.setLyricsFile(lyricsFile);
                    existing.setCoverFile(coverFile);
                    songRepository.save(existing);
                } else {
                    // 确保 ID 不冲突
                    while (songRepository.existsById(id)) {
                        id = String.valueOf(Integer.parseInt(id) + 1);
                    }
                    Song song = new Song(id, baseName, "未知艺术家", "未知专辑",
                            fileName, lyricsFile, coverFile, 0);
                    songRepository.save(song);
                    System.out.println("[MusicService] 新增: " + baseName
                            + " | 歌词: " + (lyricsFile != null ? "✓" : "✗")
                            + " | 封面: " + (coverFile != null ? "✓" : "✗"));
                }
            }

            System.out.println("[MusicService] 数据库共 " + songRepository.count() + " 首歌曲");

        } catch (IOException e) {
            System.err.println("[MusicService] 扫描音频文件失败: " + e.getMessage());
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

    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    public Optional<Song> getSongById(String id) {
        return songRepository.findById(id);
    }

    public Song saveSong(Song song) {
        return songRepository.save(song);
    }

    public Path getAudioPath(String fileName) {
        return Paths.get(musicDataPath, "audio", fileName);
    }

    public Path getLyricsPath(String fileName) {
        return Paths.get(musicDataPath, "lyrics", fileName);
    }

    public Path getCoverPath(String fileName) {
        return Paths.get(musicDataPath, "covers", fileName);
    }
}
