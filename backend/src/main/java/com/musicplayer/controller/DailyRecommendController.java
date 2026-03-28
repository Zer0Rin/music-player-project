package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import com.musicplayer.service.DailyRecommendService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/daily")
public class DailyRecommendController {

    private final DailyRecommendService dailyRecommendService;
    private final SongRepository songRepository;

    public DailyRecommendController(DailyRecommendService dailyRecommendService,
                                    SongRepository songRepository) {
        this.dailyRecommendService = dailyRecommendService;
        this.songRepository = songRepository;
    }

    @GetMapping("/recommend")
    public Map<String, Object> getDailyRecommend(Authentication auth) {
        String userId = auth.getName();
        List<String> songIds = dailyRecommendService.getDailyRecommend(userId);

        // 返回完整歌曲信息
        List<Map<String, Object>> songs = songIds.stream()
                .map(id -> songRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("title", s.getTitle());
                    map.put("artist", s.getArtist());
                    map.put("coverFile", s.getCoverFile());
                    return map;
                })
                .toList();

        return Map.of(
                "date", java.time.LocalDate.now().toString(),
                "songs", songs
        );
    }

    //日推刷新
    @PostMapping("/recommend/refresh")
    public Map<String, Object> refreshDailyRecommend(Authentication auth) {
        String userId = auth.getName();
        List<String> songIds = dailyRecommendService.refreshDailyRecommend(userId);

        List<Map<String, Object>> songs = songIds.stream()
                .map(id -> songRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("title", s.getTitle());
                    map.put("artist", s.getArtist());
                    map.put("coverFile", s.getCoverFile());
                    return map;
                })
                .toList();

        return Map.of(
                "date", java.time.LocalDate.now().toString(),
                "songs", songs
        );
    }

}