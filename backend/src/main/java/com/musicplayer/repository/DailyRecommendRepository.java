package com.musicplayer.repository;

import com.musicplayer.model.DailyRecommendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyRecommendRepository extends JpaRepository<DailyRecommendEntity, String> {
    Optional<DailyRecommendEntity> findByUserIdAndDate(String userId, String date);
    void deleteByDate(String date); // 清理过期数据
}