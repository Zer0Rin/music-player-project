package com.musicplayer.repository;

import com.musicplayer.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    List<Playlist> findByUserId(String userId);
    Optional<Playlist> findByIsSystemTrueAndUserId(String userId);
    //List<Playlist> findByIsSystemTrue();

    //推荐码
    Optional<Playlist> findByShareCode(String shareCode);

}
