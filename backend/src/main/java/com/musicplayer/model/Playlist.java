package com.musicplayer.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    private String id;

    private String name;

    @Column(length = 500)
    private String description;

    private String coverImage;
    private String coverSongId;
    private boolean isSystem;
    private long createdAt;
    private long updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "playlist_songs", joinColumns = @JoinColumn(name = "playlist_id"))
    @Column(name = "song_id")
    @OrderColumn(name = "sort_order")
    private List<String> songIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "playlist_tags", joinColumns = @JoinColumn(name = "playlist_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    public Playlist() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Playlist(String name, boolean isSystem) {
        this();
        this.name = name;
        this.isSystem = isSystem;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getCoverSongId() { return coverSongId; }
    public void setCoverSongId(String coverSongId) { this.coverSongId = coverSongId; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Column(name = "user_id")
    private String userId;

    // getter/setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    //歌单推荐码
    private String shareCode;        // 推荐码
    private Long shareCodeExpiry;    // 过期时间戳（毫秒）
    //推荐码功能
    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }
    public Long getShareCodeExpiry() { return shareCodeExpiry; }
    public void setShareCodeExpiry(Long shareCodeExpiry) { this.shareCodeExpiry = shareCodeExpiry; }
}


