package com.musicplayer.controller;

import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.repository.PlaylistRepository;
import com.musicplayer.service.MusicService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import com.musicplayer.service.HotScoreService;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/hot")
public class HotController {

    private final MusicService musicService;
    private final PlaylistRepository playlistRepository;


    private final HotScoreService hotScoreService;

    public HotController(MusicService musicService,
                         PlaylistRepository playlistRepository,
                         HotScoreService hotScoreService) {
        this.musicService = musicService;
        this.playlistRepository = playlistRepository;
        this.hotScoreService = hotScoreService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        hotScoreService.updateNow();
        return ResponseEntity.ok("热度已刷新");
    }

    @GetMapping
    public Map<String, Object> getHotItems(Authentication auth) {
        String userId = auth.getName();

        // 1. 热门歌单（非系统歌单，非当前用户自己的）
        List<Map<String, Object>> hotPlaylists = playlistRepository.findAll().stream()
                .filter(pl -> !pl.isSystem())
                .filter(pl -> pl.getSongIds() != null && !pl.getSongIds().isEmpty())
                .map(pl -> {
                    // 歌单热度 = 导入次数 × 5（高权重）+ 歌曲热度均值 × 1（低权重）
                    double songHotAvg = pl.getSongIds().stream()
                            .map(sid -> musicService.getSongById(sid).orElse(null))
                            .filter(Objects::nonNull)
                            .mapToInt(Song::getHotScore)
                            .average()
                            .orElse(0);
                    int plScore = (int)(pl.getImportCount() * 5 + songHotAvg * 1);

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", "playlist");
                    map.put("id", pl.getId());
                    map.put("name", pl.getName());
                    map.put("description", pl.getDescription() != null ? pl.getDescription() : "");
                    map.put("coverImage", pl.getCoverImage());
                    map.put("songCount", pl.getSongIds().size());
                    map.put("hotScore", plScore);
                    // 取第一首歌的封面作为备用
                    if (pl.getCoverImage() == null && !pl.getSongIds().isEmpty()) {
                        map.put("coverSongId", pl.getSongIds().get(0));
                    }
                    return map;
                })
                .filter(m -> (int)m.get("hotScore") > 0)
                .sorted((a, b) -> (int)b.get("hotScore") - (int)a.get("hotScore"))
                .limit(10)
                .collect(Collectors.toList());

        // 2. 热门歌曲
        List<Map<String, Object>> hotSongs = musicService.getAllSongs().stream()
                .filter(s -> s.getHotScore() > 0)
                .sorted((a, b) -> b.getHotScore() - a.getHotScore())
                .limit(15)
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", "song");
                    map.put("id", s.getId());
                    map.put("title", s.getTitle());
                    map.put("artist", s.getArtist());
                    map.put("coverFile", s.getCoverFile());
                    map.put("hotScore", s.getHotScore());
                    return map;
                })
                .collect(Collectors.toList());

        return Map.of("playlists", hotPlaylists, "songs", hotSongs);
    }
}