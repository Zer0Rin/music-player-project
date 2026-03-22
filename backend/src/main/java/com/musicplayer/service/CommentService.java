package com.musicplayer.service;

import com.musicplayer.model.Comment;
import com.musicplayer.model.User;
import com.musicplayer.repository.CommentRepository;
import com.musicplayer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public List<Comment> getComments(String songId) {
        return commentRepository.findBySongIdOrderByCreatedAtDesc(songId);
    }

    public List<Comment> getReplies(String parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    public Comment addComment(String songId, String userId, String content, String parentId) {
        User user = userRepository.findById(userId).orElseThrow();
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setSongId(songId);
        comment.setUserId(userId);
        comment.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        comment.setAvatarFile(user.getAvatarFile());
        comment.setContent(content.trim());
        comment.setParentId(parentId);
        comment.setLikes(0);
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment toggleLike(String commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.setLikes(comment.getLikes() + 1);
        return commentRepository.save(comment);
    }

    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("无权删除");
        commentRepository.delete(comment);
    }
}