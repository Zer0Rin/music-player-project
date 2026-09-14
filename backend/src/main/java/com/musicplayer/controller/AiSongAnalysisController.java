package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.security.StreamTicketService;
import com.musicplayer.service.MusicService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

@RestController
@RequestMapping("/api/ai/analysis")
public class AiSongAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AiSongAnalysisController.class);

    private final ChatClient chatClient;
    private final MusicService musicService;
    private final StreamTicketService streamTicketService;

    public AiSongAnalysisController(ChatClient.Builder builder,
                                    MusicService musicService,
                                    StreamTicketService streamTicketService) {
        this.musicService = musicService;
        this.streamTicketService = streamTicketService;
        this.chatClient = builder
                .defaultSystem("你是一位资深音乐评论人，文字有温度、有文学性，像朋友聊音乐一样自然。")
                .build();
    }

    /**
     * 为 SSE 开流签发一张一次性票据。
     *
     * <p>浏览器的 {@code EventSource} 无法设置请求头，因此不能要求它带 Authorization。
     * 客户端先用普通请求（带 Authorization 头）换票，再用票开流——避免把长期有效的 JWT
     * 放进 URL 而泄漏到访问日志、浏览器历史与 Referer。
     */
    @PostMapping("/{songId}/ticket")
    public ResponseEntity<Map<String, Object>> issueStreamTicket(@PathVariable String songId,
                                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("USER");

        StreamTicketService.Issued issued =
                streamTicketService.issue(authentication.getName(), role, songId);

        return ResponseEntity.ok(Map.of(
                "ticket", issued.ticket(),
                "expiresInSeconds", issued.expiresInSeconds()));
    }

    @GetMapping(value = "/{songId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(@PathVariable String songId) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Optional<Song> songOpt = musicService.getSongById(songId);
        if (songOpt.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().data("❌ 歌曲不存在"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        Song song = songOpt.get();
        String lyrics = readLyricsText(song);
        String prompt = buildPrompt(song, lyrics);

        // 客户端断开（关标签页 / 断网 / 切歌）时释放上游订阅。
        //
        // ⚠️ 实测限制（不要高估这个修复）：dispose 之后**上游 HTTP 连接并不会被及时关闭**。
        // 用本地桩模型验证：客户端在读 48 字节后断开，应用侧回调确实触发、订阅确实被 dispose，
        // 但对端在 18 秒里把 60 段（每段约 60KB）全部写成功 —— 说明连接仍被读取，
        // provider 侧仍会继续生成。也就是说：这个修复解决的是"我们这边不要继续处理、不要继续往
        // 已断开的客户端写"，**不能**据此宣称"停止计费"。
        // 真正封顶成本要靠 provider 侧的额度/上限，或给 WebClient 配一个读超时。
        UpstreamSubscriptionGuard upstream = new UpstreamSubscriptionGuard();
        emitter.onCompletion(() -> releaseUpstream(upstream, songId, "客户端断开或流已结束"));
        emitter.onError(error -> releaseUpstream(upstream, songId, "SSE 出错"));
        emitter.onTimeout(() -> {
            releaseUpstream(upstream, songId, "SSE 超时");
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                Disposable subscription = chatClient.prompt()
                        .user(prompt)
                        .stream()
                        .chatResponse()
                        .doOnNext(chunk -> {
                            String text = chunk.getResult().getOutput().getContent();
                            if (text != null && !text.isEmpty()) {
                                try {
                                    emitter.send(SseEmitter.event().data(text));
                                } catch (IOException e) {
                                    // 客户端已断开：结束 emitter，由上面的回调释放上游订阅
                                    emitter.completeWithError(e);
                                }
                            }
                        })
                        .doOnComplete(emitter::complete)
                        .doOnError(emitter::completeWithError)
                        .subscribe();
                // 竞态：客户端可能在 subscribe() 返回之前就断开了，register 会就地取消
                upstream.register(subscription);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 释放上游订阅，避免继续处理已无人接收的数据。
     *
     * <p>只有真的释放掉一个已建立的订阅时才记 INFO——这样"断开是否被处理到"在日志里是可见的。
     * <b>注意这条日志不代表计费已停止</b>：实测上游 HTTP 连接不会因此及时关闭，
     * 详见本类 analyze() 里的说明。
     */
    private void releaseUpstream(UpstreamSubscriptionGuard upstream, String songId, String reason) {
        if (upstream.release()) {
            log.info("[AI 解析] 已释放上游订阅 songId={} reason={}", songId, reason);
        }
    }

    /** 读取歌词文件并去除时间轴标签，返回纯文本 */
    private String readLyricsText(Song song) {
        if (song.getLyricsFile() == null) return null;
        try {
            Path lyricsPath = musicService.getLyricsPath(song.getLyricsFile());
            if (!Files.exists(lyricsPath)) return null;
            String raw = Files.readString(lyricsPath, StandardCharsets.UTF_8);
            return raw
                    .replaceAll("\\[\\d{2}:\\d{2}[.:]\\d{2,3}\\]", "") // 去 LRC 时间轴
                    .replaceAll("\\[.*?\\]", "")                         // 去其他标签
                    .lines()
                    .map(String::trim)
                    .filter(l -> !l.isBlank())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(Song song, String lyrics) {
        String lyricsSection = (lyrics != null && !lyrics.isBlank())
                ? lyrics
                : "（暂无歌词）";

        return """
                请对以下歌曲进行深度解析，使用 Markdown 格式输出：
                
                歌曲信息：
                - 标题：%s
                - 艺术家：%s
                - 专辑：%s
                
                歌词：
                %s
                
                请按以下结构输出：
                
                ## 🎭 情感基调
                （2-3句，描述整体情绪氛围）
                
                ## 📖 歌词意境
                （深度解读核心意象与表达手法，3-5句）
                
                ## 💬 金句赏析
                （挑出1-2句最有共鸣的歌词并点评）
                
                ## 🎬 创作视角
                （从风格/时代/艺术家角度简析，2-3句）
                """.formatted(
                song.getTitle(),
                song.getArtist() != null ? song.getArtist() : "未知艺术家",
                song.getAlbum() != null ? song.getAlbum() : "未知专辑",
                lyricsSection
        );
    }
}