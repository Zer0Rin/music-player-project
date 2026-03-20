package com.musicplayer.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-dj")
public class AiDjController {

    private final ChatClient chatClient;

    public AiDjController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个资深且品味极佳的音乐DJ。你的任务是根据用户的需求，生成一个精选歌单。\n" +
                        "规则：\n" +
                        "1. 你【必须】使用 'searchLocalMusic' 工具在本地曲库中搜索歌曲。\n" +
                        "2. 【绝对禁止】推荐搜索结果中不存在的歌曲！如果没有合适的，就从结果中挑最接近的。\n" +
                        "3. 你必须返回一个纯 JSON 格式的数据，【不要】包含任何 markdown 标记（例如不要输出 ```json ），只要原生的 JSON 字符串。\n" +
                        "返回的 JSON 必须包含以下三个属性：\n" +
                        "  - playlistName: 字符串，生成的歌单名字\n" +
                        "  - description: 字符串，歌单推荐语\n" +
                        "  - songs: 数组，包含挑选出的歌曲对象，每个对象必须有 id, title, artist 三个字段")
                .defaultFunctions("searchLocalMusic")
                .build();
    }

    @PostMapping("/generate")
    public String generatePlaylist(@RequestBody String userPrompt) {
        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }
}