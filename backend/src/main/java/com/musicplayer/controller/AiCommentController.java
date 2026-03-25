package com.musicplayer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import com.musicplayer.service.AiCommentService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
public class AiCommentController {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper; // 用于将 List 转换为 JSON 字符串
    private final AiCommentService aiCommentService;

    public AiCommentController(ChatClient.Builder builder, AiCommentService aiCommentService) {
        this.objectMapper = new ObjectMapper();

        this.aiCommentService = aiCommentService;

        // 配置系统提示词，严格约束大模型的角色和输出格式
        this.chatClient = builder
                .defaultSystem("你是一个专业、客观的社区评论审核助手。\n" +
                        "你的任务是分析传入的 JSON 格式的评论列表，并为每条评论打上分类标签。\n\n" +
                        "【分类标签定义】\n" +
                        "- positive: 正常、正面、友善、普通的交流评论，或表达对音乐的喜爱。\n" +
                        "- negative: 负面、引战、谩骂、恶意攻击、极度消极的评论。\n" +
                        "- spam: 广告、无意义灌水、求关注、求互粉等垃圾评论。\n\n" +
                        "【严格输出规则】\n" +
                        "1. 你必须严格返回一个 JSON 对象。Key 是评论的 id，Value 是分类结果字符串。\n" +
                        "2. 绝对禁止输出任何 Markdown 标记。\n" +
                        "3. 绝对禁止包含任何解释性文字，只允许输出纯正的 JSON 字符串。\n" +
                        "示例：id为101的评论是positive，id为102的评论是negative，以此类推。")
                .build();
    }

    @PostMapping("/ai-analyze")
    public String analyzeComments(@RequestBody List<CommentPayload> comments) {
        try {
            String userPrompt = objectMapper.writeValueAsString(comments);

            // 用 PromptTemplate 的 literal 模式，避免 {} 被当成模板变量
            return chatClient.prompt()
                    .user(u -> u.text("{input}").param("input", userPrompt))
                    .call()
                    .content();

        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    // 定义一个静态内部类，用于接收前端传来的精简数据 (只有 id 和 content)
    public static class CommentPayload {
        private String id;  // ← Long 改成 String
        private String content;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }



    //生成端点  AI乐评人
    @PostMapping("/generate/{songId}")
    public ResponseEntity<String> generateAiComment(@PathVariable String songId) {
        try {
            String result = aiCommentService.generateAndSaveComment(songId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


}