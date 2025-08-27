package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.TourPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 관광지 정보 데이터 접근을 위한 JPA Repository
 */
@Repository
public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {
    
    /**
     * 위도/경도 기준으로 가까운 관광지 조회
     */
    @Query("SELECT t FROM TourPlace t ORDER BY " +
           "SQRT(POWER(t.latitude - :lat, 2) + POWER(t.longitude - :lon, 2))")
    List<TourPlace> findNearbyPlaces(@Param("lat") double lat, @Param("lon") double lon);
}
