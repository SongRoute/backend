package com.app.yeogigangwon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 기상청 단기예보 데이터를 저장하는 MySQL 엔티티
 */
@Entity
@Table(name = "weather_forecasts")
@Data
@NoArgsConstructor
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String baseDate;    // 기준 날짜
    
    @Column(nullable = false)
    private String baseTime;    // 기준 시간
    
    @Column(nullable = false)
    private String nx;          // 격자 X 좌표
    
    @Column(nullable = false)
    private String ny;          // 격자 Y 좌표
    
    @Column(nullable = false)
    private LocalDateTime forecastTime; // 예보 시간
    
    @Column(length = 1000)
    private String weatherData; // 기상 데이터 (JSON 형태)
    
    @Column(nullable = false)
    private LocalDateTime createdAt; // 생성 시간
}
