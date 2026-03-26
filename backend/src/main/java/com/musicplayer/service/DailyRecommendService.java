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

        // 命中缓存直接返回
        Optional<DailyRecommendEntity> cached =
                dailyRecommendRepository.findByUserIdAndDate(userId, today);
        if (cached.isPresent()) {
            return cached.get().getSongIds();
        }

        // 生成今日推荐
        return generateAndCache(userId, today);
    }

    private List<String> generateAndCache(String userId, String date) {
        System.out.println("[DailyRecommend] 开始生成用户: " + userId);

        // 收藏歌曲（权重×3）
        List<String> favoriteSongIds = playlistRepository.findAll().stream()
                .filter(pl -> pl.isSystem()
                        && "我喜欢".equals(pl.getName())
                        && userId.equals(pl.getUserId()))
                .findFirst()
                .map(Playlist::getSongIds)
                .orElse(List.of());
        System.out.println("[DailyRecommend] 收藏歌曲数: " + favoriteSongIds.size());

        // 歌单中的歌曲（权重×2）
        List<String> playlistSongIds = playlistRepository.findAll().stream()
                .filter(pl -> !pl.isSystem() && userId.equals(pl.getUserId()))
                .flatMap(pl -> pl.getSongIds().stream())
                .distinct()
                .collect(Collectors.toList());
        System.out.println("[DailyRecommend] 歌单歌曲数: " + playlistSongIds.size());

        // 最近播放（权重×1）
        List<String> recentSongIds = recentPlayRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .limit(20)
                .map(RecentPlay::getSongId)
                .collect(Collectors.toList());
        System.out.println("[DailyRecommend] 最近播放数: " + recentSongIds.size());

        // 采样（必须在空判断之前）
        List<String> sampleSongs = buildSampleDescription(favoriteSongIds, playlistSongIds, recentSongIds);
        System.out.println("[DailyRecommend] 采样歌曲数: " + sampleSongs.size());

        if (sampleSongs.isEmpty()) {
            System.out.println("[DailyRecommend] 无行为数据，走热度兜底");
            return fallbackHot(10);
        }

        // 构建 prompt
        String prompt = "用户的音乐偏好数据如下：\n\n" +
                "最喜欢的歌曲（权重最高）：\n" +
                getSongDescriptions(favoriteSongIds, 8) + "\n\n" +
                "用户整理进歌单的歌曲（权重中等）：\n" +
                getSongDescriptions(playlistSongIds, 6) + "\n\n" +
                "最近播放记录（权重较低）：\n" +
                getSongDescriptions(recentSongIds, 5) + "\n\n" +
                "请分析用户的音乐口味，使用 searchLocalMusic 工具搜索曲库，" +
                "推荐10首用户可能喜欢但未出现在以上列表中的新歌。" +
                "多搜几次不同关键词以扩大候选范围。";

        System.out.println("[DailyRecommend] 开始调用 AI...");



        // 调用 AI
        try {
            String systemPrompt = "你是一个精准的音乐推荐引擎。" +
                    "根据用户的听歌偏好，在本地曲库中搜索并推荐10首他可能喜欢的歌曲。\n" +
                    "规则：必须使用 searchLocalMusic 工具搜索，" +
                    "禁止推荐用户已听过或收藏的歌曲，" +
                    "禁止推荐不存在的歌曲，" +
                    "返回纯JSON格式：" +
                    "{\"songs\": [{\"id\": \"x\", \"title\": \"x\", \"artist\": \"x\"}]}";

            System.out.println("[DailyRecommend] systemPrompt: " + systemPrompt);

            String safeSystem = systemPrompt
                    .replace("[", "")
                    .replace("]", "")
                    .replace("{", "")
                    .replace("}", "");

            String safePrompt = prompt
                    .replace("[", "")
                    .replace("]", "")
                    .replace("{", "")
                    .replace("}", "");


            System.out.println("[DailyRecommend] 开始调用 AI...");

            String response = chatClient.prompt()
                    .system(safeSystem)
                    .user(safePrompt)
                    .call()
                    .content();

            System.out.println("[DailyRecommend] AI 返回: " + response);

            String cleaned = response;
            // 提取 { "songs": [...] } 部分
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start != -1 && end != -1 && end > start) {
                cleaned = response.substring(start, end + 1);
            } else {
                System.err.println("[DailyRecommend] 无法提取 JSON");
                return fallbackHot(10);
            }

            JsonNode root = objectMapper.readTree(cleaned);

            JsonNode songsNode = root.get("songs");

            List<String> songIds = new ArrayList<>();
            if (songsNode != null && songsNode.isArray()) {
                for (JsonNode s : songsNode) {
                    String id = s.get("id").asText();
                    if (songRepository.existsById(id)) {
                        songIds.add(id);
                    }
                }
            }

            System.out.println("[DailyRecommend] 有效歌曲数: " + songIds.size());

            if (songIds.isEmpty()) return fallbackHot(10);

            // 缓存到数据库
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