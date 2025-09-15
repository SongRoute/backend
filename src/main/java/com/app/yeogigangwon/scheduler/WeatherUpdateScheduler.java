package com.app.yeogigangwon.scheduler;

import com.app.yeogigangwon.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 날씨 데이터 업데이트 스케줄러
 * 15분마다 새로운 API 호출로 최신 데이터 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherUpdateScheduler {

    private final WeatherService weatherService;

    /**
     * 15분마다 날씨 데이터 업데이트
     * 강원도 주요 지역들의 날씨 데이터를 주기적으로 갱신
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15분마다 실행
    public void updateWeatherData() {
        log.info("=== 날씨 데이터 업데이트 스케줄러 시작 ===");
        
        try {
            // 강원도 주요 지역들의 날씨 데이터 업데이트
            updateWeatherForLocation(37.7491, 128.8785, "춘천시 소양강댐");
            updateWeatherForLocation(37.2756, 128.8950, "원주시");
            updateWeatherForLocation(37.4500, 128.9000, "강릉시");
            updateWeatherForLocation(37.3000, 128.7000, "태백시");
            updateWeatherForLocation(38.2000, 128.6000, "속초시");
            
            log.info("=== 날씨 데이터 업데이트 완료 ===");
        } catch (Exception e) {
            log.error("날씨 데이터 업데이트 중 오류 발생", e);
        }
    }

    /**
     * 특정 위치의 날씨 데이터 업데이트
     */
    private void updateWeatherForLocation(double lat, double lon, String locationName) {
        try {
            log.info("{} 날씨 데이터 업데이트 중...", locationName);
            weatherService.fetchAndSave(lat, lon);
            log.info("{} 날씨 데이터 업데이트 완료", locationName);
        } catch (Exception e) {
            log.warn("{} 날씨 데이터 업데이트 실패: {}", locationName, e.getMessage());
        }
    }
}
