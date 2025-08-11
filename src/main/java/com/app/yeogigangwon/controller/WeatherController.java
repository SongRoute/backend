package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.domain.WeatherForecast;
import com.app.yeogigangwon.dto.WeatherInfo;
import com.app.yeogigangwon.dto.WeatherSummary;
import com.app.yeogigangwon.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    // 실시간 요약
    @GetMapping("/weather")
    public ResponseEntity<WeatherSummary> getWeatherSummary(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        WeatherSummary summary = weatherService.getWeatherSummary(lat, lon);
        return ResponseEntity.ok(summary);
    }

    // API 호출 → DB 저장
    @PostMapping("/weather/fetch-save")
    public ResponseEntity<String> fetchAndSave(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        WeatherForecast saved = weatherService.fetchAndSave(lat, lon);
        return ResponseEntity.ok("saved: " + saved.getId());
    }

    // DB 최신 데이터 조회
    @GetMapping("/weather/latest")
    public ResponseEntity<WeatherInfo> getLatest(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        WeatherInfo info = weatherService.getLatestFromDb(lat, lon);
        if (info == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(info);
    }

    // 관광지 추천용 날씨 점수
    @GetMapping("/weather/score")
    public ResponseEntity<Map<String, Object>> getWeatherScore(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        WeatherSummary summary = weatherService.getWeatherSummary(lat, lon);

        Map<String, Object> response = new HashMap<>();
        response.put("overallScore", summary.getOverallWeatherScore());
        response.put("baseScore", summary.getInfo().getWeatherScore());
        response.put("recommendationGrade", summary.getRecommendationGrade());
        response.put("isRecommendable", summary.isRecommendable());
        response.put("weatherInfo", summary.getInfo());
        response.put("alerts", summary.getAlerts());

        return ResponseEntity.ok(response);
    }
}
