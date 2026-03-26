package com.musicplayer.repository;

import com.musicplayer.model.RecentPlay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface RecentPlayRepository extends JpaRepository<RecentPlay, String> {
    List<RecentPlay> findByUserIdOrderByPlayedAtDesc(String userId, Pageable pageable);
    Optional<RecentPlay> findByUserIdAndSongId(String userId, String songId);
    long countByUserId(String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RecentPlay r WHERE r.userId = :userId AND r.playedAt = (SELECT MIN(r2.playedAt) FROM RecentPlay r2 WHERE r2.userId = :userId)")
    void deleteOldestByUserId(@Param("userId") String userId);

    long countBySongId(String songId);

    List<RecentPlay> findByUserIdOrderByPlayedAtDesc(String userId);

}