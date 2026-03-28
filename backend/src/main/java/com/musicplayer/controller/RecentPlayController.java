package com.musicplayer.controller;

import com.musicplayer.model.RecentPlay;
import com.musicplayer.model.Song;
import com.musicplayer.repository.RecentPlayRepository;
import com.musicplayer.service.MusicService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/recent")
public class RecentPlayController {

    private final RecentPlayRepository recentPlayRepository;
    private final MusicService musicService;
    private static final int MAX_RECENT = 100;

    public RecentPlayController(RecentPlayRepository recentPlayRepository,
                                MusicService musicService) {
        this.recentPlayRepository = recentPlayRepository;
        this.musicService = musicService;
    }

    // 记录播放
    @PostMapping("/{songId}")
    public ResponseEntity<?> recordPlay(
            @PathVariable String songId,
            @RequestParam(required = false, defaultValue = "0") Integer playDuration,
            @RequestParam(required = false, defaultValue = "false") Boolean skipped,
            Authentication auth) {

        String userId = auth.getName();

        RecentPlay record = recentPlayRepository
                .findByUserIdAndSongId(userId, songId)
                .orElseGet(() -> {
                    RecentPlay r = new RecentPlay();
                    r.setId(UUID.randomUUID().toString());
                    r.setUserId(userId);
                    r.setSongId(songId);
                    return r;
                });

        record.setPlayedAt(LocalDateTime.now());
        record.setPlayDuration(playDuration);
        record.setSkipped(skipped);
        recentPlayRepository.save(record);

        if (recentPlayRepository.countByUserId(userId) > MAX_RECENT) {
            recentPlayRepository.deleteOldestByUserId(userId);
        }

        return ResponseEntity.ok().build();
    }

    // 获取最近播放列表（返回完整 Song 对象）
    @GetMapping
    public ResponseEntity<?> getRecent(Authentication auth) {
        String userId = auth.getName();
        List<RecentPlay> records = recentPlayRepository
                .findByUserIdOrderByPlayedAtDesc(userId, PageRequest.of(0, 100));

        List<Map<String, Object>> result = new ArrayList<>();
        for (RecentPlay record : records) {
            musicService.getSongById(record.getSongId()).ifPresent(song -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", song.getId());
                item.put("title", song.getTitle());
                item.put("artist", song.getArtist());
                item.put("album", song.getAlbum());
                item.put("duration", song.getDuration());
                item.put("audioFile", song.getAudioFile());
                item.put("coverFile", song.getCoverFile());
                item.put("lyricsFile", song.getLyricsFile());
                item.put("playedAt", record.getPlayedAt().toString());
                result.add(item);
            });
        }
        return ResponseEntity.ok(result);
    }
}