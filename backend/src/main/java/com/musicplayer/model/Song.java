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
    private String audioFile;
    private String lyricsFile;
    private String coverFile;
    private int duration;

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

    public String getAudioFile() { return audioFile; }
    public void setAudioFile(String audioFile) { this.audioFile = audioFile; }

    public String getLyricsFile() { return lyricsFile; }
    public void setLyricsFile(String lyricsFile) { this.lyricsFile = lyricsFile; }

    public String getCoverFile() { return coverFile; }
    public void setCoverFile(String coverFile) { this.coverFile = coverFile; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
