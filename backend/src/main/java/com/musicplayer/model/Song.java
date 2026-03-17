package com.musicplayer.model;

import jakarta.persistence.*;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    private String id;

    private String title;
    private String artist;
    private String album;
    private String genre;
    private String year;
    private String audioFile;
    private String lyricsFile;
    private String coverFile;
    private int duration;           // 秒
    private boolean hasEmbeddedCover;   // 音频文件内嵌封面
    private boolean hasEmbeddedLyrics;  // 音频文件内嵌歌词

    public Song() {}

    public Song(String id, String title, String artist, String album,
                String audioFile, String lyricsFile, String coverFile, int duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.audioFile = audioFile;
        this.lyricsFile = lyricsFile;
        this.coverFile = coverFile;
        this.duration = duration;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getAudioFile() { return audioFile; }
    public void setAudioFile(String audioFile) { this.audioFile = audioFile; }

    public String getLyricsFile() { return lyricsFile; }
    public void setLyricsFile(String lyricsFile) { this.lyricsFile = lyricsFile; }

    public String getCoverFile() { return coverFile; }
    public void setCoverFile(String coverFile) { this.coverFile = coverFile; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isHasEmbeddedCover() { return hasEmbeddedCover; }
    public void setHasEmbeddedCover(boolean hasEmbeddedCover) { this.hasEmbeddedCover = hasEmbeddedCover; }

    public boolean isHasEmbeddedLyrics() { return hasEmbeddedLyrics; }
    public void setHasEmbeddedLyrics(boolean hasEmbeddedLyrics) { this.hasEmbeddedLyrics = hasEmbeddedLyrics; }
}
