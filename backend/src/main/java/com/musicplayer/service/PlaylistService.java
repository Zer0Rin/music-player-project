package com.musicplayer.service;

import com.musicplayer.model.Playlist;
import com.musicplayer.repository.PlaylistRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@Transactional
public class PlaylistService {

    @Value("${music.data.path:./music-data}")
    private String musicDataPath;

    private final PlaylistRepository playlistRepository;

    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }


    public List<Playlist> getAllPlaylists(String userId) {
        return playlistRepository.findByUserId(userId);
    }

    public Optional<Playlist> getPlaylist(String id) {
        return playlistRepository.findById(id);
    }

    public Playlist createPlaylist(String name, String description, List<String> tags, String userId) {
        Playlist pl = new Playlist(name, false);
        pl.setDescription(description);
        pl.setUserId(userId);
        if (tags != null) pl.setTags(tags);
        return playlistRepository.save(pl);
    }

    public boolean deletePlaylist(String id) {
        return playlistRepository.findById(id)
                .filter(pl -> !pl.isSystem())
                .map(pl -> { playlistRepository.delete(pl); return true; })
                .orElse(false);
    }

    public Optional<Playlist> renamePlaylist(String id, String newName) {
        return playlistRepository.findById(id).map(pl -> {
            pl.setName(newName);
            pl.setUpdatedAt(System.currentTimeMillis());
            return playlistRepository.save(pl);
        });
    }

    /** 更新歌单信息 */
    public Optional<Playlist> updatePlaylist(String id, String name, String description, List<String> tags) {
        return playlistRepository.findById(id).map(pl -> {
            if (name != null && !name.isBlank()) pl.setName(name);
            if (description != null) pl.setDescription(description);
            if (tags != null) pl.setTags(tags);
            pl.setUpdatedAt(System.currentTimeMillis());
            return playlistRepository.save(pl);
        });
    }

    public Optional<Playlist> addSong(String playlistId, String songId) {
        return playlistRepository.findById(playlistId).map(pl -> {
            if (!pl.getSongIds().contains(songId)) {
                pl.getSongIds().add(0, songId);
                if (pl.getCoverSongId() == null) pl.setCoverSongId(songId);
                pl.setUpdatedAt(System.currentTimeMillis());
            }
            return playlistRepository.save(pl);
        });
    }

    public Optional<Playlist> removeSong(String playlistId, String songId) {
        return playlistRepository.findById(playlistId).map(pl -> {
            pl.getSongIds().remove(songId);
            pl.setUpdatedAt(System.currentTimeMillis());
            return playlistRepository.save(pl);
        });
    }

    public boolean isSongInPlaylist(String playlistId, String songId) {
        return playlistRepository.findById(playlistId)
                .map(pl -> pl.getSongIds().contains(songId))
                .orElse(false);
    }

    public Optional<Playlist> reorderSongs(String playlistId, List<String> newOrder) {
        return playlistRepository.findById(playlistId).map(pl -> {
            pl.setSongIds(new ArrayList<>(newOrder));
            pl.setUpdatedAt(System.currentTimeMillis());
            return playlistRepository.save(pl);
        });
    }

    public boolean isFavorite(String songId, String userId) {
        return isSongInPlaylist("favorites-" + userId, songId);
    }

    /** 调整歌单列表顺序 */
    public void reorderPlaylists(List<String> idOrder) {
        // 暂不实现排序持久化
    }

    public boolean toggleFavorite(String songId, String userId) {
        String favId = "favorites-" + userId;
        if (isFavorite(songId, userId)) {
            removeSong(favId, songId);
            return false;
        } else {
            addSong(favId, songId);
            return true;
        }
    }

    /** 保存歌单封面 */
    public String saveCoverImage(String playlistId, byte[] imageData, String originalFilename) {
        try {
            Path coverDir = Paths.get(musicDataPath, "playlist-covers");
            Files.createDirectories(coverDir);

            String ext = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : ".jpg";
            String fileName = playlistId + ext;
            Files.write(coverDir.resolve(fileName), imageData);

            playlistRepository.findById(playlistId).ifPresent(pl -> {
                pl.setCoverImage(fileName);
                pl.setUpdatedAt(System.currentTimeMillis());
                playlistRepository.save(pl);
            });

            return fileName;
        } catch (IOException e) {
            System.err.println("[PlaylistService] 保存封面失败: " + e.getMessage());
            return null;
        }
    }

    public Path getCoverImagePath(String fileName) {
        return Paths.get(musicDataPath, "playlist-covers", fileName);
    }


    /** 推荐码相关 */
    /** 生成推荐码 */
    public Playlist generateShareCode(String playlistId, String username) {
        Playlist pl = playlistRepository.findById(playlistId).orElseThrow();
        // 生成 6 位大写字母+数字
        String random = java.util.UUID.randomUUID().toString()
                .replaceAll("[^A-Z0-9]", "").substring(0, 6);
        String code = username.toUpperCase() + "-" + random;
        pl.setShareCode(code);
        pl.setShareCodeExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        return playlistRepository.save(pl);
    }

    /** 通过推荐码导入歌单 */
    public Playlist importByShareCode(String code, String targetUserId) {
        Playlist source = playlistRepository.findByShareCode(code)
                .orElseThrow(() -> new RuntimeException("推荐码不存在"));

        // 检查有效期
        if (source.getShareCodeExpiry() == null ||
                System.currentTimeMillis() > source.getShareCodeExpiry()) {
            throw new RuntimeException("推荐码已过期");
        }

        // 不能导入自己的歌单
        if (source.getUserId().equals(targetUserId)) {
            throw new RuntimeException("不能导入自己的歌单");
        }

        // 复制为独立副本
        Playlist copy = new Playlist(source.getName() + "（导入）", false);
        copy.setDescription(source.getDescription());
        copy.setUserId(targetUserId);
        copy.setSongIds(new ArrayList<>(source.getSongIds()));
        copy.setTags(new ArrayList<>(source.getTags() != null ? source.getTags() : List.of()));
        if (source.getCoverImage() != null) copy.setCoverImage(source.getCoverImage());
        return playlistRepository.save(copy);
    }

    /** 查询推荐码对应的歌单信息（预览用） */
    public Map<String, Object> previewShareCode(String code) {
        Playlist pl = playlistRepository.findByShareCode(code)
                .orElseThrow(() -> new RuntimeException("推荐码不存在"));
        if (pl.getShareCodeExpiry() == null ||
                System.currentTimeMillis() > pl.getShareCodeExpiry()) {
            throw new RuntimeException("推荐码已过期");
        }
        return Map.of(
                "name", pl.getName(),
                "songCount", pl.getSongIds().size(),
                "description", pl.getDescription() != null ? pl.getDescription() : ""
        );
    }

}
