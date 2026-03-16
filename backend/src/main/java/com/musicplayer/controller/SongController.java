package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.service.MusicService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final MusicService musicService;

    public SongController(MusicService musicService) {
        this.musicService = musicService;
    }

    /** 歌曲列表 */
    @GetMapping
    public List<Song> listSongs() {
        return musicService.getAllSongs();
    }

    /** 歌曲详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Song> getSong(@PathVariable String id) {
        return musicService.getSongById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 音频流（依赖 Spring Boot 内置的 Resource 处理，完美支持 Range 请求与大文件） */
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
                        case "m4a"  -> "audio/mp4"; // 顺手帮你补上 m4a 格式的支持
                        default     -> "audio/mpeg";
                    };

                    // 直接返回资源，Spring 会自动处理 bytes=xx-xx 的切片请求并返回 206 状态码
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(mimeType))
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().<Resource>build());
    }

    /** 歌词内容（返回纯文本 LRC） */
    @GetMapping("/{id}/lyrics")
    public ResponseEntity<String> getLyrics(@PathVariable String id) {
        return musicService.getSongById(id)
                .filter(song -> song.getLyricsFile() != null)
                .map(song -> {
                    Path path = musicService.getLyricsPath(song.getLyricsFile());
                    try {
                        // LRC 文件可能是 UTF-8 或 GBK 编码
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

    /** 专辑封面 */
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


}
