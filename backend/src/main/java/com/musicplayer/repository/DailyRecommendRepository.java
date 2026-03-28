package com.musicplayer.repository;

import com.musicplayer.model.DailyRecommendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyRecommendRepository extends JpaRepository<DailyRecommendEntity, String> {
    Optional<DailyRecommendEntity> findFirstByUserIdAndDate(String userId, String date);
    List<DailyRecommendEntity> findAllByUserIdAndDate(String userId, String date);
    void deleteByDate(String date);
}