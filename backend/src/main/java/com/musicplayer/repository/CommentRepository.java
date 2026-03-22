package com.musicplayer.repository;

import com.musicplayer.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findBySongIdOrderByCreatedAtDesc(String songId);
    List<Comment> findByParentIdOrderByCreatedAtAsc(String parentId);
    void deleteBySongId(String songId);
}