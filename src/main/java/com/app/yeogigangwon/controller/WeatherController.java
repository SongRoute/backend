package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.domain.WeatherForecast;
import com.app.yeogigangwon.dto.WeatherInfo;
import com.app.yeogigangwon.dto.WeatherSummary;
import com.app.yeogigangwon.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 날씨 정보 API 컨트롤러
 * 기상청 API 호출, 날씨 데이터 저장, 조회 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 실시간 날씨 요약 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 날씨 요약 정보
     */
    @GetMapping("/weather")
    public ResponseEntity<WeatherSummary> getWeatherSummary(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        log.info("날씨 요약 조회 요청 - 위도: {}, 경도: {}", lat, lon);
        
        WeatherSummary summary = weatherService.getWeatherSummary(lat, lon);
        return ResponseEntity.ok(summary);
    }

    /**
     * 기상청 API에서 날씨 데이터를 가져와서 DB에 저장
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 저장 완료 메시지
     */
    @PostMapping("/weather/fetch-save")
    public ResponseEntity<String> fetchAndSave(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        log.info("날씨 데이터 저장 요청 - 위도: {}, 경도: {}", lat, lon);
        
        WeatherForecast saved = weatherService.fetchAndSave(lat, lon);
        return ResponseEntity.ok("저장 완료: " + saved.getId());
    }

    /**
     * DB에서 최신 날씨 데이터 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 최신 날씨 정보 (없으면 204 No Content)
     */
    @GetMapping("/weather/latest")
    public ResponseEntity<WeatherInfo> getLatest(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        log.info("최신 날씨 데이터 조회 요청 - 위도: {}, 경도: {}", lat, lon);
        
        WeatherInfo info = weatherService.getLatestFromDb(lat, lon);
        if (info == null) {
            log.warn("해당 좌표의 날씨 데이터가 없습니다");
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(info);
    }
}
