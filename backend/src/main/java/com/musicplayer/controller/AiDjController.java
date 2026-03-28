package com.musicplayer.controller;

import com.musicplayer.model.RecentPlay;
import com.musicplayer.model.Song;
import com.musicplayer.repository.RecentPlayRepository;
import com.musicplayer.service.MusicService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai-dj")
public class AiDjController {

    private final ChatClient chatClient;
    private final RecentPlayRepository recentPlayRepository;
    private final MusicService musicService;

    public AiDjController(ChatClient.Builder builder,
                          RecentPlayRepository recentPlayRepository,
                          MusicService musicService) {
        this.recentPlayRepository = recentPlayRepository;
        this.musicService = musicService;
        this.chatClient = builder
                .defaultSystem("你是一个资深且品味极佳的音乐DJ。你的任务是根据用户的需求，从本地曲库中生成一个精选歌单。\n" +
                        "【核心搜索策略】\n" +
                        "当用户请求某种语言、地区、风格或情绪的歌曲（例如：日文歌、粤语歌、流行乐、忧郁）时，绝对不要直接把这些抽象词汇作为关键词去搜索！\n" +
                        "你必须先自行思考该分类下最具代表性的 3-5 位歌手或经典歌曲名，然后用这些具体的【歌手名字】或【歌曲名字】去调用工具检索。\n\n" +
                        "规则：\n" +
                        "1. 你【必须】使用 'searchLocalMusic' 工具在本地曲库中搜索歌曲。如果一次没搜到，换个歌手名多搜几次。\n" +
                        "2. 【绝对禁止】推荐搜索结果中不存在的歌曲！如果没有合适的，就从已搜索到的结果中挑最接近的。\n" +
                        "3. 你必须返回一个纯 JSON 格式的数据，【绝对不要】包含任何 markdown 标记（例如禁止输出 ```json ），【绝对不要】在 JSON 前后说任何废话，只要原生的 JSON 字符串。\n" +
                        "返回的 JSON 必须包含以下三个属性：\n" +
                        "  - playlistName: 字符串，生成的极具吸引力的歌单名字\n" +
                        "  - description: 字符串，专业且走心的歌单推荐语\n" +
                        "  - songs: 数组，包含挑选出的歌曲对象，每个对象必须有 id, title, artist 三个字段")
                .defaultFunctions("searchLocalMusic")
                .build();
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generatePlaylist(
            @RequestBody String userPrompt,
            Authentication auth) {

        // 构建用户行为摘要
        String behaviorContext = "";
        if (auth != null) {
            behaviorContext = buildBehaviorContext(auth.getName());
        }

        // 将行为摘要拼接到用户 Prompt 前面
        String enrichedPrompt = behaviorContext.isEmpty()
                ? userPrompt
                : behaviorContext + "\n\n用户的请求是：" + userPrompt;

        System.out.println("[AI DJ] enrichedPrompt: " + enrichedPrompt);

        String result = chatClient.prompt()
                .user(enrichedPrompt)
                .call()
                .content();

        return ResponseEntity.ok(result);
    }

    private String buildBehaviorContext(String userId) {
        List<RecentPlay> records = recentPlayRepository
                .findByUserIdOrderByPlayedAtDesc(userId, PageRequest.of(0, 30));

        if (records.isEmpty()) return "";

        // 统计跳过的歌曲
        List<String> skippedTitles = records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSkipped()))
                .map(r -> musicService.getSongById(r.getSongId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTitle() + " - " + o.get().getArtist())
                .limit(5)
                .collect(Collectors.toList());

        // 统计听完的歌曲（播放时长 > 0 且未跳过）
        List<String> completedTitles = records.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getSkipped())
                        && r.getPlayDuration() != null
                        && r.getPlayDuration() > 30)
                .map(r -> musicService.getSongById(r.getSongId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTitle() + " - " + o.get().getArtist())
                .limit(8)
                .collect(Collectors.toList());

        if (skippedTitles.isEmpty() && completedTitles.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【用户近期听歌偏好（请参考但不要直接复用这些歌曲）】\n");
        if (!completedTitles.isEmpty()) {
            sb.append("听完过的歌（用户喜欢）：").append(String.join("、", completedTitles)).append("\n");
        }
        if (!skippedTitles.isEmpty()) {
            sb.append("中途跳过的歌（用户不喜欢）：").append(String.join("、", skippedTitles)).append("\n");
        }
        sb.append("请根据以上偏好推断用户的音乐口味，生成更符合其喜好的歌单。");

        return sb.toString();
    }
}