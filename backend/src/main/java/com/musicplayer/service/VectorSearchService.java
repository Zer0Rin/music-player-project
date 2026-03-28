package com.musicplayer.service;

import com.musicplayer.model.Song;
import com.musicplayer.repository.SongRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorSearchService {

    private final SongRepository songRepository;
    private final EmbeddingService embeddingService;

    // 内存索引：songId -> embedding向量
    private final Map<String, float[]> index = new ConcurrentHashMap<>();

    public VectorSearchService(SongRepository songRepository,
                               EmbeddingService embeddingService) {
        this.songRepository = songRepository;
        this.embeddingService = embeddingService;
    }

    /** 启动时从数据库加载所有已有 embedding 到内存 */
    @PostConstruct
    public void loadIndex() {
        List<Song> songs = songRepository.findAll();
        int loaded = 0;
        for (Song song : songs) {
            if (song.getEmbedding() != null) {
                float[] vec = embeddingService.fromJson(song.getEmbedding());
                if (vec != null) {
                    index.put(song.getId(), vec);
                    loaded++;
                }
            }
        }
        System.out.println("[VectorSearch] 加载索引完成，共 " + loaded + " 首歌曲");
    }

    /** 新歌入库后，更新内存索引 */
    public void addToIndex(String songId, float[] vector) {
        if (vector != null) {
            index.put(songId, vector);
        }
    }

    /** 移除索引（删歌时调用） */
    public void removeFromIndex(String songId) {
        index.remove(songId);
    }

    /**
     * 语义搜索：返回最相似的 topK 首歌曲
     * 先向量检索，再从数据库取完整 Song 对象
     */
    public List<Song> search(String keyword, int topK) {
        float[] queryVec = embeddingService.embed(keyword);

        // embedding 失败时降级到关键词搜索
        if (queryVec == null || index.isEmpty()) {
            System.out.println("[VectorSearch] 降级到关键词搜索: " + keyword);
            return songRepository.searchSongs(keyword);
        }

        // 计算所有歌曲的余弦相似度
        List<Map.Entry<String, Float>> scores = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : index.entrySet()) {
            float sim = cosineSimilarity(queryVec, entry.getValue());
            scores.add(Map.entry(entry.getKey(), sim));
        }

        // 按相似度降序，取 topK
        scores.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        List<String> topIds = scores.stream()
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();

        // 批量从数据库取 Song 对象，保持排序
        Map<String, Song> songMap = new HashMap<>();
        songRepository.findAllById(topIds).forEach(s -> songMap.put(s.getId(), s));

        return topIds.stream()
                .map(songMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0f;
        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) return 0f;
        return dot / (float)(Math.sqrt(normA) * Math.sqrt(normB));
    }
}