package com.musicplayer.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "daily_recommendations")
public class DailyRecommendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String date; // 格式 "2026-03-25"

    @ElementCollection
    @CollectionTable(name = "daily_recommend_songs",
            joinColumns = @JoinColumn(name = "recommend_id"))
    @Column(name = "song_id")
    private List<String> songIds;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }
}