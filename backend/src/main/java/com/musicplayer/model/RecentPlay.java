package com.musicplayer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recent_plays")
public class RecentPlay {

    @Id
    private String id;

    private String userId;
    private String songId;
    private LocalDateTime playedAt;

    @PrePersist
    protected void onCreate() {
        if (playedAt == null) playedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}