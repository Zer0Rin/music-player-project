package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.service.MusicService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/analysis")
public class AiSongAnalysisController {

    private final ChatClient chatClient;
    private final MusicService musicService;

    public AiSongAnalysisController(ChatClient.Builder builder, MusicService musicService) {
        this.musicService = musicService;
        this.chatClient = builder
                .defaultSystem("你是一位资深音乐评论人，文字有温度、有文学性，像朋友聊音乐一样自然。")
                .build();
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

        CompletableFuture.runAsync(() -> {
            try {
                chatClient.prompt()
                        .user(prompt)
                        .stream()
                        .chatResponse()
                        .doOnNext(chunk -> {
                            String text = chunk.getResult().getOutput().getContent();
                            if (text != null && !text.isEmpty()) {
                                try {
                                    emitter.send(SseEmitter.event().data(text));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }
                        })
                        .doOnComplete(emitter::complete)
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        return emitter;
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