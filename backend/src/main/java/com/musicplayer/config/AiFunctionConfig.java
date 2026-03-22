package com.musicplayer.config;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiFunctionConfig {

    private final SongRepository songRepository;

    public AiFunctionConfig(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    // 定义 AI 传递的参数结构
    public record SongSearchRequest(String keyword) {}


    @Bean
    @Description("在本地音乐库中搜索歌曲。参数 keyword 可以是歌曲名、歌手、流派或情绪标签（如：周杰伦、轻音乐、悲伤）。")
    public Function<SongSearchRequest, List<Song>> searchLocalMusic() {
        return request -> {
            if (request.keyword() == null || request.keyword().isBlank()) {
                // 如果 AI 没传关键词，随便给它塞 20 首歌兜底
                return songRepository.findAll().stream().limit(20).toList();
            }
            return songRepository.searchSongs(request.keyword());
        };
    }
}