package com.musicplayer.repository;

import com.musicplayer.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, String> {

    //给 AI 使用的全局模糊搜索（忽略大小写，匹配 标题、歌手 或 专辑）
    @Query("SELECT s FROM Song s WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.album) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Song> searchSongs(@Param("keyword") String keyword);

}