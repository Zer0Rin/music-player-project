package com.musicplayer.service;

import com.musicplayer.model.Comment;
import com.musicplayer.model.Song;
import com.musicplayer.repository.CommentRepository;
import com.musicplayer.repository.SongRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Service
public class AiCommentService {

    // Agent 1：音乐分析师，负责提取歌曲特征
    private final ChatClient analystAgent;
    // Agent 2：乐评人，基于分析结果生成评论
    private final ChatClient criticAgent;

    private final SongRepository songRepository;
    private final CommentRepository commentRepository;
    private final MusicService musicService;

    public AiCommentService(ChatClient.Builder builder,
                            SongRepository songRepository,
                            CommentRepository commentRepository,
                            MusicService musicService) {
        this.songRepository = songRepository;
        this.commentRepository = commentRepository;
        this.musicService = musicService;

        // Agent 1：分析师，输出结构化的音乐特征分析
        this.analystAgent = builder
                .defaultSystem("""
                        你是一个专业的音乐分析师。
                        根据提供的歌曲信息和歌词，输出一段结构化的音乐特征分析。
                        
                        【分析维度】
                        - 情绪基调：用2-3个词描述（例：忧郁、治愈、热血）
                        - 歌词意象：提炼出1-2个核心意象或主题（例：离别、雨夜、故乡）
                        - 音乐风格：判断曲风（例：古风、R&B、民谣、电子）
                        - 适合场景：最适合在什么情境下听（例：深夜独处、雨天通勤）
                        - 亮点：编曲或歌词中最值得一提的一个细节
                        
                        【输出格式】直接输出分析内容，不超过150字，不要任何前缀。
                        """)
                .build();

        // Agent 2：乐评人，基于分析结果写评论
        this.criticAgent = builder
                .defaultSystem("""
                        你是一个资深音乐评论人，同时也懂得网易云评论区的情感表达方式。
                        你会收到一份音乐分析报告，基于这份报告为歌曲生成一条真实自然的评论。
                        
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
                        4. 结合音乐分析报告的内容，不要说空话
                        """)
                .build();
    }

    public String generateAndSaveComment(String songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));

        String lyrics = readLyricsText(song);

        // Agent 1：先做音乐特征分析
        String analysisPrompt = buildAnalysisPrompt(song, lyrics);
        String musicAnalysis = analystAgent.prompt()
                .user(analysisPrompt)
                .call()
                .content();

        System.out.println("[AI乐评] Agent1 分析结果: " + musicAnalysis);

        // Agent 2：基于分析结果生成评论
        String lyricsSection = (lyrics != null && !lyrics.isBlank())
                ? (lyrics.length() > 300 ? lyrics.substring(0, 300) + "..." : lyrics)
                : "（暂无歌词）";
        String criticPrompt = """
        以下是对《%s》（%s）的音乐特征分析报告：
        
        %s
        
        歌词节选：
        %s
        
        请基于以上分析和歌词，生成一条评论。可以直接引用歌词中印象深刻的句子。
        """.formatted(
                song.getTitle(),
                song.getArtist(),
                musicAnalysis,
                lyricsSection.length() > 300
                        ? lyricsSection.substring(0, 300) + "..."
                        : lyricsSection
        );

        String commentText = criticAgent.prompt()
                .user(criticPrompt)
                .call()
                .content();

        System.out.println("[AI乐评] Agent2 生成评论: " + commentText);

        if (commentText == null || commentText.isBlank()) {
            throw new RuntimeException("AI 生成评论为空");
        }

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setSongId(songId);
        comment.setUserId(UserService.AI_CRITIC_ID);
        comment.setNickname("AI 乐评人");
        comment.setAvatarFile("ai-critic-avatar.png");
        comment.setContent(commentText.trim());
        comment.setLikes(0);
        commentRepository.save(comment);

        return commentText.trim();
    }

    private String buildAnalysisPrompt(Song song, String lyrics) {
        String lyricsSection = (lyrics != null && !lyrics.isBlank())
                ? lyrics : "（暂无歌词）";
        return """
                请分析以下歌曲：
                
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
                        ? lyricsSection.substring(0, 300) + "..."
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