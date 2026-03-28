package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.service.HotScoreService;
import com.musicplayer.service.MusicService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.musicplayer.repository.SongRepository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import java.util.stream.Collectors;
import java.util.Arrays;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final MusicService musicService;
    private final HotScoreService hotScoreService;
    private final SongRepository songRepository;

    public SongController(MusicService musicService, HotScoreService hotScoreService, SongRepository songRepository) {
        this.musicService = musicService;
        this.hotScoreService = hotScoreService;
        this.songRepository = songRepository;
    }

    @GetMapping
    public List<Song> listSongs() {
        return musicService.getAllSongs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Song> getSong(@PathVariable String id) {
        return musicService.getSongById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> getAudio(@PathVariable String id) {
        return musicService.getSongById(id)
                .filter(song -> song.getAudioFile() != null)
                .map(song -> {
                    Path path = musicService.getAudioPath(song.getAudioFile());
                    if (!Files.exists(path)) return ResponseEntity.notFound().<Resource>build();
                    Resource resource = new FileSystemResource(path);
                    String ext = song.getAudioFile().substring(song.getAudioFile().lastIndexOf('.') + 1).toLowerCase();
                    String mimeType = switch (ext) {
                        case "flac" -> "audio/flac";
                        case "wav"  -> "audio/wav";
                        case "ogg"  -> "audio/ogg";
                        case "m4a"  -> "audio/mp4";
                        default     -> "audio/mpeg";
                    };
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(mimeType))
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(path.toFile().length()))
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().<Resource>build());
    }

    @GetMapping("/{id}/lyrics")
    public ResponseEntity<String> getLyrics(@PathVariable String id) {
        return musicService.getSongById(id)
                .filter(song -> song.getLyricsFile() != null)
                .map(song -> {
                    Path path = musicService.getLyricsPath(song.getLyricsFile());
                    try {
                        String content;
                        try {
                            content = Files.readString(path);
                        } catch (java.nio.charset.MalformedInputException e) {
                            content = new String(Files.readAllBytes(path), java.nio.charset.Charset.forName("GBK"));
                        }
                        return ResponseEntity.ok()
                                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                                .body(content);
                    } catch (IOException e) {
                        return ResponseEntity.internalServerError().<String>body("读取歌词失败");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable String id) {
        return musicService.getSongById(id)
                .filter(song -> song.getCoverFile() != null)
                .map(song -> {
                    Path path = musicService.getCoverPath(song.getCoverFile());
                    if (!Files.exists(path)) return ResponseEntity.notFound().<Resource>build();
                    String ext = song.getCoverFile().substring(song.getCoverFile().lastIndexOf('.') + 1).toLowerCase();
                    MediaType mediaType = switch (ext) {
                        case "png" -> MediaType.IMAGE_PNG;
                        case "webp" -> MediaType.parseMediaType("image/webp");
                        default -> MediaType.IMAGE_JPEG;
                    };
                    Resource resource = new FileSystemResource(path);
                    String etag = "\"" + path.toFile().lastModified() + "-" + path.toFile().length() + "\"";
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .eTag(etag)
                            .cacheControl(CacheControl.noCache())
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadSongs(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            try {
                if (!isAudioFile(filename)) {
                    results.add(Map.of("file", filename, "status", "error", "message", "不支持的格式"));
                    continue;
                }
                Path dest = Paths.get(musicService.getAudioDir(), filename);
                file.transferTo(dest);
                results.add(Map.of("file", filename, "status", "ok"));
            } catch (Exception e) {
                results.add(Map.of("file", filename, "status", "error", "message", e.getMessage()));
            }
        }
        musicService.scanMusic();
        return ResponseEntity.ok(Map.of("results", results));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSong(@PathVariable String id) {
        musicService.deleteSong(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSong(@PathVariable String id,
                                        @RequestBody Map<String, String> body) {
        return musicService.getSongById(id).map(song -> {
            if (body.containsKey("title")) song.setTitle(body.get("title"));
            if (body.containsKey("artist")) song.setArtist(body.get("artist"));
            if (body.containsKey("album")) song.setAlbum(body.get("album"));
            if (body.containsKey("genre")) song.setGenre(body.get("genre"));
            if (body.containsKey("year")) song.setYear(body.get("year"));
            return ResponseEntity.ok(musicService.saveSong(song));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cover/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCover(@PathVariable String id,
                                         @RequestParam("file") MultipartFile file) {
        return musicService.getSongById(id).map(song -> {
            try {
                Path coverDir = musicService.getCoverDir();
                Files.createDirectories(coverDir);
                String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                        : ".jpg";
                String baseName = song.getAudioFile().substring(0, song.getAudioFile().lastIndexOf('.'));
                String filename = baseName + ext;
                file.transferTo(coverDir.resolve(filename));
                song.setCoverFile(filename);
                musicService.saveSong(song);
                return ResponseEntity.ok(Map.of("coverFile", filename));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/lyrics/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadLyrics(@PathVariable String id,
                                          @RequestParam("file") MultipartFile file) {
        return musicService.getSongById(id).map(song -> {
            try {
                Path lyricsDir = musicService.getLyricsDir();
                Files.createDirectories(lyricsDir);
                String baseName = song.getAudioFile().substring(0, song.getAudioFile().lastIndexOf('.'));
                String filename = baseName + ".lrc";
                file.transferTo(lyricsDir.resolve(filename));
                song.setLyricsFile(filename);
                musicService.saveSong(song);
                return ResponseEntity.ok(Map.of("lyricsFile", filename));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hot")
    public List<Song> getHotSongs() {
        return songRepository.findByHotScoreGreaterThanOrderByHotScoreDesc(
                0, org.springframework.data.domain.PageRequest.of(0, 20)
        );
    }

    @PostMapping("/hot/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refreshHot() {
        hotScoreService.updateNow();
        return ResponseEntity.ok(Map.of("message", "热度刷新完成"));
    }

    private boolean isAudioFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".flac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a");
    }



    //歌词搜索
    @GetMapping("/search/lyrics")
    public List<Map<String, Object>> searchByLyrics(@RequestParam String q) {
        if (q == null || q.isBlank()) return List.of();

        String keyword = q.trim().toLowerCase();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Song song : musicService.getAllSongs()) {
            if (song.getLyricsFile() == null) continue;
            try {
                Path lyricsPath = musicService.getLyricsPath(song.getLyricsFile());
                if (!Files.exists(lyricsPath)) continue;

                String raw;
                try {
                    raw = Files.readString(lyricsPath, StandardCharsets.UTF_8);
                } catch (java.nio.charset.MalformedInputException e) {
                    raw = new String(Files.readAllBytes(lyricsPath),
                            java.nio.charset.Charset.forName("GBK"));
                }

                String text = Arrays.stream(raw.split("\n"))
                        .map(line -> line.trim())
                        // 跳过 JSON 元信息行（网易云混合格式）
                        .filter(line -> !line.startsWith("{"))
                        // 跳过 ASS 非对话行
                        .filter(line -> !line.startsWith("[") || line.matches("^\\[\\d+:\\d+.*"))
                        .map(line -> {
                            // ASS Dialogue 行：提取文字，去除所有 {\\xxx} 标签
                            if (line.startsWith("Dialogue:")) {
                                // 取最后一个逗号后的内容
                                int last = line.lastIndexOf(',');
                                String content = last >= 0 ? line.substring(last + 1) : line;
                                return content.replaceAll("\\{[^}]*\\}", "").trim();
                            }
                            // SRT 时间行跳过
                            if (line.matches("\\d+:\\d+:\\d+[,.]\\d+\\s*-->.*")) return "";
                            // SRT 序号行跳过
                            if (line.matches("^\\d+$")) return "";
                            // LRC/ELRC 行：去行首时间戳 [mm:ss.ms]
                            String stripped = line.replaceAll("^\\[\\d+:\\d+[.:]\\d+\\]", "");
                            // 去行内时间戳（LDDC 逐字格式）[mm:ss.ms]
                            stripped = stripped.replaceAll("\\[\\d+:\\d+[.:]\\d+\\]", "");
                            // 去 ESlyric 逐字时间戳 <mm:ss.ms>
                            stripped = stripped.replaceAll("<\\d+:\\d+[.:]\\d+>", "");
                            // 去 ELRC {offset} 标记
                            stripped = stripped.replaceAll("\\{[\\d.]+\\}", "");
                            // 去斜杠翻译右侧（保留原文，去翻译）
                            int slashIdx = stripped.indexOf(" / ");
                            if (slashIdx >= 0) stripped = stripped.substring(0, slashIdx);
                            return stripped.trim();
                        })
                        .filter(l -> !l.isBlank())
                        .collect(Collectors.joining(" "));

                if (!text.toLowerCase().contains(keyword)) continue;

                // 提取匹配片段
                int idx = text.toLowerCase().indexOf(keyword);
                int start = Math.max(0, idx - 15);
                int end = Math.min(text.length(), idx + keyword.length() + 30);
                String snippet = (start > 0 ? "..." : "")
                        + text.substring(start, end).trim()
                        + (end < text.length() ? "..." : "");

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", song.getId());
                map.put("title", song.getTitle());
                map.put("artist", song.getArtist());
                map.put("coverFile", song.getCoverFile());
                map.put("snippet", snippet);
                results.add(map);
            } catch (Exception ignored) {}
        }
        return results;
    }



}