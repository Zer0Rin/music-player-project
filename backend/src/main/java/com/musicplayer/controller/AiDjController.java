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
                .defaultSystem("你是一个资深且品味极佳的音乐DJ。你的任务是根据用户的需求，从本地曲库中生成一个精选歌单。\n\n" +
                        "【搜索策略】\n" +
                        "本地曲库支持语义搜索，你可以用情绪、风格、场景、歌手名、歌曲名等关键词搜索。\n" +
                        "每次生成歌单最多调用 3 次搜索，每次换不同维度的关键词以获取多样化结果。\n\n" +
                        "【规则】\n" +
                        "1. 必须使用 'searchLocalMusic' 工具搜索，禁止推荐搜索结果中不存在的歌曲。\n" +
                        "2. 歌单歌曲数量：8-12 首，保证多样性，不要全是同一歌手。\n" +
                        "3. 只返回纯 JSON，禁止包含 markdown 标记或任何额外文字。\n" +
                        "JSON 格式：\n" +
                        "{\n" +
                        "  \"playlistName\": \"歌单名\",\n" +
                        "  \"description\": \"歌单推荐语\",\n" +
                        "  \"songs\": [{\"id\": \"\", \"title\": \"\", \"artist\": \"\"}]\n" +
                        "}")
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

        // 听完的歌（播放时长 > 30s 且未跳过）
        List<String> completedTitles = records.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getSkipped())
                        && r.getPlayDuration() != null
                        && r.getPlayDuration() > 30)
                .map(r -> musicService.getSongById(r.getSongId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTitle() + " - " + o.get().getArtist())
                .limit(8)
                .collect(Collectors.toList());

        // 跳过的歌：必须播放时长极短（<15s）才算真的不喜欢，过滤掉可能是单曲循环的误判
        List<String> skippedTitles = records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSkipped())
                        && r.getPlayDuration() != null
                        && r.getPlayDuration() < 15) // 只有听了不到15秒就跳过才算不喜欢
                .map(r -> musicService.getSongById(r.getSongId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTitle() + " - " + o.get().getArtist())
                .limit(3) // 最多只提3首，降低负面权重
                .collect(Collectors.toList());

        if (completedTitles.isEmpty() && skippedTitles.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【用户近期听歌偏好（请参考但不要直接复用这些歌曲）】\n");
        if (!completedTitles.isEmpty()) {
            sb.append("听完过的歌（用户喜欢，权重高）：")
                    .append(String.join("、", completedTitles)).append("\n");
        }
        if (!skippedTitles.isEmpty()) {
            sb.append("快速跳过的歌（用户可能不喜欢，仅供参考）：")
                    .append(String.join("、", skippedTitles)).append("\n");
        }
        sb.append("请重点参考用户喜欢的歌曲风格，生成更符合其喜好的歌单。");

        return sb.toString();
    }
}