package com.musicplayer.service;

import com.musicplayer.model.Song;
import com.musicplayer.repository.CommentRepository;
import com.musicplayer.repository.PlaylistRepository;
import com.musicplayer.repository.SongRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Service
public class HotScoreService {

    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final CommentRepository commentRepository;

    public HotScoreService(SongRepository songRepository,
                           PlaylistRepository playlistRepository,
                           CommentRepository commentRepository) {
        this.songRepository = songRepository;
        this.playlistRepository = playlistRepository;
        this.commentRepository = commentRepository;
    }

    // 每小时更新一次，cron = 每小时0分0秒
    @Scheduled(cron = "0 0 * * * *")
    public void updateHotScores() {
        System.out.println("[HotScore] 开始更新热度分...");
        List<Song> songs = songRepository.findAll();

        for (Song song : songs) {
            // 1. 收藏数：统计所有歌单里包含该歌曲的数量
            long favoriteCount = playlistRepository.findAll().stream()
                    .filter(pl -> pl.getSongIds().contains(song.getId()))
                    .count();

            // 2. 评论数
            long commentCount = commentRepository.countBySongId(song.getId());

            // 3. 被导入次数：统计包含该歌曲的歌单的总导入次数
            int importCount = playlistRepository.findAll().stream()
                    .filter(pl -> pl.getSongIds().contains(song.getId()))
                    .mapToInt(pl -> pl.getImportCount())
                    .sum();

            // 热度公式
            int score = (int)(favoriteCount * 3 + commentCount * 2 + importCount * 5);
            song.setHotScore(score);
            song.setHotScoreUpdatedAt(System.currentTimeMillis());
            songRepository.save(song);
        }

        songs.forEach(s -> System.out.println("[HotScore] " + s.getTitle() + " -> " + s.getHotScore()));
        System.out.println("[HotScore] 热度分更新完成");
    }

    // 启动时自动刷新
    @PostConstruct
    public void init() {
        updateHotScores();
    }

    // 手动触发（供接口调用）
    public void updateNow() {
        updateHotScores();
    }
}