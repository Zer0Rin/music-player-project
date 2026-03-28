package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplayer.model.*;
import com.musicplayer.repository.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;

@Service
public class DailyRecommendService {

    private final ChatClient chatClient;
    private final DailyRecommendRepository dailyRecommendRepository;
    private final PlaylistRepository playlistRepository;
    private final RecentPlayRepository recentPlayRepository;
    private final SongRepository songRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DailyRecommendService(ChatClient.Builder builder,
                                 DailyRecommendRepository dailyRecommendRepository,
                                 PlaylistRepository playlistRepository,
                                 RecentPlayRepository recentPlayRepository,
                                 SongRepository songRepository) {
        this.dailyRecommendRepository = dailyRecommendRepository;
        this.playlistRepository = playlistRepository;
        this.recentPlayRepository = recentPlayRepository;
        this.songRepository = songRepository;
        // 构造器里不设 defaultSystem，只设 functions
        this.chatClient = builder
                .defaultFunctions("searchLocalMusic")
                .build();
    }

    /**
     * 获取今日推荐，有缓存直接返回，无缓存则生成
     */

    public List<String> getDailyRecommend(String userId) {
        String today = LocalDate.now().toString();
        Optional<DailyRecommendEntity> cached =
                dailyRecommendRepository.findFirstByUserIdAndDate(userId, today);
        if (cached.isPresent()) {
            return cached.get().getSongIds();
        }
        return generateAndCache(userId, today);
    }

    //刷新日推
    public List<String> refreshDailyRecommend(String userId) {
        String today = LocalDate.now().toString();
        // 删除今天所有缓存（防止重复记录）
        List<DailyRecommendEntity> existing = dailyRecommendRepository.findAllByUserIdAndDate(userId, today);
        if (!existing.isEmpty()) dailyRecommendRepository.deleteAll(existing);
        return generateAndCache(userId, today);
    }

    private List<String> generateAndCache(String userId, String date) {
        System.out.println("[DailyRecommend] 开始生成用户: " + userId);

        List<String> favoriteSongIds = playlistRepository.findAll().stream()
                .filter(pl -> pl.isSystem() && "我喜欢".equals(pl.getName()) && userId.equals(pl.getUserId()))
                .findFirst()
                .map(Playlist::getSongIds)
                .orElse(List.of())
                .stream()
                .limit(20)  // 收藏20首
                .collect(Collectors.toList());

        List<String> playlistSongIds = playlistRepository.findAll().stream()
                .filter(pl -> !pl.isSystem() && userId.equals(pl.getUserId()))
                .flatMap(pl -> pl.getSongIds().stream())
                .distinct()
                .limit(20)  // 最多取20首，避免歌单过大占满上下文
                .collect(Collectors.toList());

        List<String> recentSongIds = recentPlayRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .limit(20)
                .map(RecentPlay::getSongId)
                .collect(Collectors.toList());

        List<String> sampleSongs = buildSampleDescription(favoriteSongIds, playlistSongIds, recentSongIds);
        if (sampleSongs.isEmpty()) {
            System.out.println("[DailyRecommend] 无行为数据，走热度兜底");
            return fallbackHot(10);
        }

        // 构建已听过的歌曲集合（用于排除）
        Set<String> listenedIds = new HashSet<>(sampleSongs);

        // 只把歌曲描述给 AI 提炼风格，不让它直接推荐这些歌
        String favoriteDesc = getSongDescriptions(favoriteSongIds, 8);
        String playlistDesc = getSongDescriptions(playlistSongIds, 5);
        String recentDesc = getSongDescriptions(recentSongIds, 4);

        String systemPrompt = "你是一个精准的音乐推荐引擎。" +
                "根据用户的听歌偏好，使用 searchLocalMusic 工具在本地曲库中搜索并推荐10首新歌。\n" +
                "规则：\n" +
                "1. 必须使用 searchLocalMusic 工具搜索，最多搜索3次，每次用不同维度的关键词。\n" +
                "2. 禁止推荐用户已听过或收藏的歌曲。\n" +
                "3. 禁止推荐不存在于搜索结果中的歌曲。\n" +
                "4. 只返回纯JSON，包含songs数组，每个元素有id、title、artist三个字段。";

        String userPrompt = "用户音乐偏好：\n" +
                "最喜欢（权重高）：" + favoriteDesc + "\n" +
                "自建歌单（权重中）：" + playlistDesc + "\n" +
                "最近播放（权重低）：" + recentDesc + "\n\n" +
                "请分析用户口味，搜索并推荐10首风格相似但用户未听过的新歌。";

        System.out.println("[DailyRecommend] 开始调用 AI...");

        try {
            String safeSystem = systemPrompt.replace("{", "\\{").replace("}", "\\}");
            String safeUser = userPrompt.replace("[", "\\[").replace("]", "\\]");

            String response = chatClient.prompt()
                    .system(safeSystem)
                    .user(safeUser)
                    .call()
                    .content();

            System.out.println("[DailyRecommend] AI 返回: " + response);

            // 提取 JSON
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start == -1 || end == -1 || end <= start) {
                System.err.println("[DailyRecommend] 无法提取 JSON");
                return fallbackHot(10);
            }
            String cleaned = response.substring(start, end + 1);

            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode songsNode = root.get("songs");

            List<String> songIds = new ArrayList<>();
            if (songsNode != null && songsNode.isArray()) {
                for (JsonNode s : songsNode) {
                    String id = s.get("id").asText();
                    // 双重校验：数据库存在 且 用户未听过
                    if (songRepository.existsById(id) && !listenedIds.contains(id)) {
                        songIds.add(id);
                    }
                }
            }

            System.out.println("[DailyRecommend] 有效歌曲数: " + songIds.size());
            if (songIds.isEmpty()) return fallbackHot(10);

            DailyRecommendEntity entity = new DailyRecommendEntity();
            entity.setUserId(userId);
            entity.setDate(date);
            entity.setSongIds(songIds);
            dailyRecommendRepository.save(entity);

            return songIds;

        } catch (Exception e) {
            System.err.println("[DailyRecommend] 生成失败: " + e.getMessage());
            e.printStackTrace();
            return fallbackHot(10);
        }
    }

    private String getSongDescriptions(List<String> songIds, int limit) {
        return songIds.stream()
                .limit(limit)
                .map(id -> songRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(s -> {
                    String title = s.getTitle().replace("[", "").replace("]", "");
                    String artist = s.getArtist().replace("[", "").replace("]", "");
                    return title + " - " + artist;
                })
                .collect(Collectors.joining(", "));
    }

    private List<String> buildSampleDescription(List<String> fav, List<String> pl, List<String> recent) {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(fav.stream().limit(8).toList());
        all.addAll(pl.stream().limit(6).toList());
        all.addAll(recent.stream().limit(5).toList());
        return new ArrayList<>(all);
    }

    private List<String> fallbackHot(int limit) {
        return songRepository.findAll().stream()
                .sorted((a, b) -> b.getHotScore() - a.getHotScore())
                .limit(limit)
                .map(Song::getId)
                .collect(Collectors.toList());
    }

    // 每天凌晨 1 点清理 7 天前的缓存
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * *")
    public void cleanOldCache() {
        String sevenDaysAgo = LocalDate.now().minusDays(7).toString();
        dailyRecommendRepository.deleteByDate(sevenDaysAgo);
    }
}