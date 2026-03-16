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

    @PostConstruct
    public void init() {
        // 确保"我喜欢"系统歌单存在
        if (playlistRepository.findByIsSystemTrue().isEmpty()) {
            Playlist favorites = new Playlist("我喜欢", true);
            favorites.setId("favorites");
            playlistRepository.save(favorites);
            System.out.println("[PlaylistService] 创建系统歌单: 我喜欢");
        }
        System.out.println("[PlaylistService] 数据库共 " + playlistRepository.count() + " 个歌单");
    }

    public List<Playlist> getAllPlaylists() {
        return playlistRepository.findAll();
    }

    public Optional<Playlist> getPlaylist(String id) {
        return playlistRepository.findById(id);
    }

    public Playlist createPlaylist(String name, String description, List<String> tags) {
        Playlist pl = new Playlist(name, false);
        pl.setDescription(description);
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

    public boolean isFavorite(String songId) {
        return isSongInPlaylist("favorites", songId);
    }

    /** 调整歌单列表顺序 */
    public void reorderPlaylists(List<String> idOrder) {
        // 暂不实现排序持久化
    }

    public boolean toggleFavorite(String songId) {
        if (isFavorite(songId)) {
            removeSong("favorites", songId);
            return false;
        } else {
            addSong("favorites", songId);
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
}
