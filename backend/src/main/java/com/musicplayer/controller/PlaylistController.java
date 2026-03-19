package com.musicplayer.controller;

import com.musicplayer.model.Playlist;
import com.musicplayer.service.PlaylistService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private static final long MAX_COVER_SIZE = 7 * 1024 * 1024;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public List<Playlist> listPlaylists(Authentication auth) {
        return playlistService.getAllPlaylists(auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Playlist> getPlaylist(@PathVariable String id) {
        return playlistService.getPlaylist(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Playlist createPlaylist(@RequestBody Map<String, Object> body, Authentication auth) {
        String name = (String) body.getOrDefault("name", "新建歌单");
        String description = (String) body.getOrDefault("description", "");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");
        return playlistService.createPlaylist(name, description, tags, auth.getName());
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<Map<String, String>> uploadCover(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        if (file.getSize() > MAX_COVER_SIZE) return ResponseEntity.badRequest().body(Map.of("error", "文件超过7MB限制"));
        try {
            String fileName = playlistService.saveCoverImage(id, file.getBytes(), file.getOriginalFilename());
            if (fileName != null) return ResponseEntity.ok(Map.of("coverImage", fileName));
            return ResponseEntity.internalServerError().body(Map.of("error", "保存失败"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable String id) {
        return playlistService.getPlaylist(id)
                .filter(pl -> pl.getCoverImage() != null)
                .map(pl -> {
                    Path path = playlistService.getCoverImagePath(pl.getCoverImage());
                    if (!Files.exists(path)) return ResponseEntity.notFound().<Resource>build();
                    Resource resource = new FileSystemResource(path);
                    String ext = pl.getCoverImage().substring(pl.getCoverImage().lastIndexOf('.') + 1).toLowerCase();
                    MediaType mediaType = switch (ext) {
                        case "png" -> MediaType.IMAGE_PNG;
                        case "webp" -> MediaType.parseMediaType("image/webp");
                        default -> MediaType.IMAGE_JPEG;
                    };
                    return ResponseEntity.ok().contentType(mediaType).body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/cover")
    public ResponseEntity<Void> deleteCover(@PathVariable String id) {
        return playlistService.getPlaylist(id).map(pl -> {
            if (pl.getCoverImage() != null) {
                try {
                    Path path = playlistService.getCoverImagePath(pl.getCoverImage());
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {}
                pl.setCoverImage(null);
                playlistService.updatePlaylist(id, null, null, null);
            }
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable String id) {
        if (playlistService.deletePlaylist(id)) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<Playlist> renamePlaylist(@PathVariable String id, @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        if (newName == null || newName.isBlank()) return ResponseEntity.badRequest().build();
        return playlistService.renamePlaylist(id, newName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Playlist> updatePlaylist(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");
        return playlistService.updatePlaylist(id, name, description, tags)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/songs")
    public ResponseEntity<Playlist> addSong(@PathVariable String id, @RequestBody Map<String, String> body) {
        String songId = body.get("songId");
        if (songId == null) return ResponseEntity.badRequest().build();
        return playlistService.addSong(id, songId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<Playlist> removeSong(@PathVariable String id, @PathVariable String songId) {
        return playlistService.removeSong(id, songId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/reorder")
    public ResponseEntity<Playlist> reorderSongs(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        List<String> order = body.get("songIds");
        if (order == null) return ResponseEntity.badRequest().build();
        return playlistService.reorderSongs(id, order)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderPlaylists(@RequestBody Map<String, List<String>> body) {
        List<String> order = body.get("playlistIds");
        if (order == null) return ResponseEntity.badRequest().build();
        playlistService.reorderPlaylists(order);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/favorites/toggle")
    public ResponseEntity<Map<String, Boolean>> toggleFavorite(
            @RequestBody Map<String, String> body, Authentication auth) {
        String songId = body.get("songId");
        if (songId == null) return ResponseEntity.badRequest().build();
        boolean isFav = playlistService.toggleFavorite(songId, auth.getName());
        return ResponseEntity.ok(Map.of("favorite", isFav));
    }

    @GetMapping("/favorites/check/{songId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @PathVariable String songId, Authentication auth) {
        return ResponseEntity.ok(Map.of("favorite", playlistService.isFavorite(songId, auth.getName())));
    }
}