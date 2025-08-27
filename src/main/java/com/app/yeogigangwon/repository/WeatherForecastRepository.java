package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기상 예보 데이터 접근을 위한 JPA Repository
 */
@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {
    
    /**
     * 특정 격자 좌표의 최신 기상 예보 조회
     */
    @Query("SELECT w FROM WeatherForecast w " +
           "WHERE w.nx = :nx AND w.ny = :ny " +
           "ORDER BY w.forecastTime DESC")
    List<WeatherForecast> findLatestByGrid(@Param("nx") String nx, @Param("ny") String ny);
    
    /**
     * 특정 시간 이후의 기상 예보 조회
     */
    List<WeatherForecast> findByForecastTimeAfterOrderByForecastTime(LocalDateTime time);
}
