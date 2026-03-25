package com.musicplayer.service;

import com.musicplayer.model.Comment;
import com.musicplayer.model.Song;
import com.musicplayer.repository.CommentRepository;
import com.musicplayer.repository.SongRepository;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

import java.util.UUID;

@Service
public class AiCommentService {

    private final ChatClient chatClient;
    private final SongRepository songRepository;
    private final CommentRepository commentRepository;
    private final MusicService musicService;
    private final Random random = new Random();

    public AiCommentService(ChatClient.Builder builder,
                            SongRepository songRepository,
                            CommentRepository commentRepository,
                            MusicService musicService) {
        this.songRepository = songRepository;
        this.commentRepository = commentRepository;
        this.musicService = musicService;
        this.chatClient = builder
                .defaultSystem("""
                        你是一个资深音乐评论人，同时也懂得网易云评论区的情感表达方式。
                        你需要为一首歌生成一条真实自然的评论。
                        
                        【风格要求】随机选择以下两种风格之一：
                        风格A - 网易云情绪流：短小精悍，1-2句，戳中人心，
                          带有个人情感投射，像真实用户的深夜感悟。
                          例："听这首歌的时候刚好在下雨，突然就哭了。"
                        风格B - 乐评人专业风：2-3句，有音乐分析深度，
                          提到编曲/歌词意象/艺术家风格，但不晦涩。
                          例："间奏那段钢琴的留白处理得极好，像在替听众喘息。"
                        
                        【硬性规则】
                        1. 只输出评论正文，不要任何前缀、解释或标点以外的内容
                        2. 不超过80字
                        3. 不要提及"AI"或"生成"等字眼
                        4. 结合歌词内容，不要说空话
                        """)
                .build();
    }

    public String generateAndSaveComment(String songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));

        String lyrics = readLyricsText(song);
        String prompt = buildPrompt(song, lyrics);

        String commentText = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (commentText == null || commentText.isBlank()) {
            throw new RuntimeException("AI 生成评论为空");
        }

        // 保存评论
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setSongId(songId);
        comment.setUserId(UserService.AI_CRITIC_ID);
        comment.setNickname("AI 乐评人");
        comment.setAvatarFile("ai-critic-avatar.png"); // 放一张固定头像到 uploads/avatars 目录
        comment.setContent(commentText.trim());
        comment.setLikes(0);
        // createdAt 由 @PrePersist 自动填
        commentRepository.save(comment);



        return commentText.trim();
    }

    private String buildPrompt(Song song, String lyrics) {
        String lyricsSection = (lyrics != null && !lyrics.isBlank())
                ? lyrics : "（暂无歌词）";
        return """
                请为以下歌曲生成一条评论：
                
                歌曲：%s
                艺术家：%s
                专辑：%s
                
                歌词节选：
                %s
                """.formatted(
                song.getTitle(),
                song.getArtist() != null ? song.getArtist() : "未知",
                song.getAlbum() != null ? song.getAlbum() : "未知",
                lyricsSection.length() > 300
                        ? lyricsSection.substring(0, 300) + "..." // 只取前300字
                        : lyricsSection
        );
    }

    private String readLyricsText(Song song) {
        if (song.getLyricsFile() == null) return null;
        try {
            var path = musicService.getLyricsPath(song.getLyricsFile());
            if (!Files.exists(path)) return null;
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            return raw
                    .replaceAll("\\[\\d{2}:\\d{2}[.:]\\d{2,3}\\]", "")
                    .replaceAll("\\[.*?\\]", "")
                    .lines()
                    .map(String::trim)
                    .filter(l -> !l.isBlank())
                    .collect(java.util.stream.Collectors.joining("\n"));
        } catch (Exception e) {
            return null;
        }
    }
}