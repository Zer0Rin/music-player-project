package com.musicplayer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    private String id;

    private String songId;
    private String userId;
    private String nickname;
    private String avatarFile;

    @Column(length = 500)
    private String content;

    private int likes;

    private String parentId; // null = 顶级评论，有值 = 回复

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarFile() { return avatarFile; }
    public void setAvatarFile(String avatarFile) { this.avatarFile = avatarFile; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}