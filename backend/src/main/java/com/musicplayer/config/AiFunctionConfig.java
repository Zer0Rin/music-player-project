package com.musicplayer.config;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;
import com.musicplayer.service.VectorSearchService;

@Configuration
public class AiFunctionConfig {

    private final SongRepository songRepository;
    private final VectorSearchService vectorSearchService; // 新增注入

    public AiFunctionConfig(SongRepository songRepository,
                            VectorSearchService vectorSearchService) {
        this.songRepository = songRepository;
        this.vectorSearchService = vectorSearchService;
    }

    public record SongSearchRequest(String keyword) {}

    @Bean
    @Description("在本地音乐库中搜索歌曲。参数 keyword 可以是歌曲名、歌手、流派或情绪标签（如：周杰伦、轻音乐、悲伤、适合深夜听的歌）。")
    public Function<SongSearchRequest, List<SongBrief>> searchLocalMusic() {
        return request -> {
            List<Song> songs;
            if (request.keyword() == null || request.keyword().isBlank()) {
                songs = songRepository.findAll().stream().limit(20).toList();
            } else {
                songs = vectorSearchService.search(request.keyword(), 15);
            }
            // 只返回模型需要的字段，去掉 embedding 等大字段
            return songs.stream()
                    .map(s -> new SongBrief(s.getId(), s.getTitle(), s.getArtist(), s.getAlbum(), s.getGenre()))
                    .toList();
        };
    }



    public record SongBrief(String id, String title, String artist, String album, String genre) {}

}