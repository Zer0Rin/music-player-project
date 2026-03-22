package com.musicplayer.controller;

import com.musicplayer.model.Comment;
import com.musicplayer.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/song/{songId}")
    public List<Comment> getComments(@PathVariable String songId) {
        return commentService.getComments(songId);
    }

    @GetMapping("/replies/{parentId}")
    public List<Comment> getReplies(@PathVariable String parentId) {
        return commentService.getReplies(parentId);
    }

    @PostMapping("/song/{songId}")
    public ResponseEntity<?> addComment(@PathVariable String songId,
                                        @RequestBody Map<String, String> body,
                                        Authentication auth) {
        String content = body.get("content");
        String parentId = body.get("parentId");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "内容不能为空"));
        if (content.length() > 500)
            return ResponseEntity.badRequest().body(Map.of("message", "评论不超过500字"));
        Comment comment = commentService.addComment(songId, auth.getName(), content, parentId);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeComment(@PathVariable String id) {
        return ResponseEntity.ok(commentService.toggleLike(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable String id, Authentication auth) {
        try {
            commentService.deleteComment(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}