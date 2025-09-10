package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.dto.RecommendationRequest;
import com.app.yeogigangwon.dto.TourPlaceRecommendation;
import com.app.yeogigangwon.service.TourPlaceRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 관광지 추천 API 컨트롤러
 * 혼잡도, 거리, 테마, 날씨를 종합적으로 고려한 관광지 추천
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TourPlaceRecommendationController {

    private final TourPlaceRecommendationService recommendationService;

    /**
     * 종합적인 관광지 추천 (POST 방식)
     * 
     * @param request 추천 요청 정보
     * @return 추천된 관광지 목록 (점수순 정렬)
     */
    @PostMapping("/places/recommend")
    public ResponseEntity<List<TourPlaceRecommendation>> getRecommendations(
            @RequestBody RecommendationRequest request) {
        
        log.info("관광지 추천 요청 - 위치: ({}, {}), 선호테마: {}", 
                request.getLatitude(), request.getLongitude(), request.getPreferredThemes());

        // 기본값 설정
        if (request.getMaxDistance() <= 0) request.setMaxDistance(50);
        if (request.getLimit() <= 0) request.setLimit(10);

        List<TourPlaceRecommendation> recommendations = recommendationService.getRecommendations(request);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 간편한 관광지 추천 (GET 방식)
     * 
     * @param lat 위도
     * @param lon 경도
     * @param themes 선호 테마 (쉼표로 구분, 예: "해변,산,실내")
     * @param maxDistance 최대 거리 (km, 기본값: 50)
     * @param limit 추천 개수 (기본값: 10)
     * @param avoidCrowded 혼잡한 곳 피하기 (기본값: true)
     * @param considerWeather 날씨 고려하기 (기본값: true)
     * @param transportationMode 이동 수단 (CAR/WALKING, 기본값: CAR)
     * @param maxTravelTime 최대 이동 시간 (분, 기본값: 60)
     * @param considerTravelTime 이동 시간 고려하기 (기본값: true)
     * @return 추천된 관광지 목록
     */
    @GetMapping("/places/recommend")
    public ResponseEntity<List<TourPlaceRecommendation>> getRecommendationsSimple(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String themes,
            @RequestParam(defaultValue = "50") int maxDistance,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "true") boolean avoidCrowded,
            @RequestParam(defaultValue = "true") boolean considerWeather,
            @RequestParam(defaultValue = "CAR") String transportationMode,
            @RequestParam(defaultValue = "60") int maxTravelTime,
            @RequestParam(defaultValue = "true") boolean considerTravelTime) {

        log.info("간편 관광지 추천 요청 - 위치: ({}, {}), 테마: {}, 이동수단: {}", lat, lon, themes, transportationMode);

        // 테마 파싱
        List<String> preferredThemes = null;
        if (themes != null && !themes.trim().isEmpty()) {
            preferredThemes = Arrays.asList(themes.split(","));
            preferredThemes = preferredThemes.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        // 이동 수단 파싱
        RecommendationRequest.TransportationMode mode;
        try {
            mode = RecommendationRequest.TransportationMode.valueOf(transportationMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 이동 수단: {}, 기본값 CAR 사용", transportationMode);
            mode = RecommendationRequest.TransportationMode.CAR;
        }

        RecommendationRequest request = new RecommendationRequest(
                lat, lon, preferredThemes, maxDistance, limit, avoidCrowded, considerWeather,
                mode, maxTravelTime, considerTravelTime
        );

        List<TourPlaceRecommendation> recommendations = recommendationService.getRecommendations(request);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 날씨 기반 관광지 추천
     * 현재 날씨에 가장 적합한 관광지 추천
     * 
     * @param lat 위도
     * @param lon 경도
     * @param limit 추천 개수 (기본값: 5)
     * @param transportationMode 이동 수단 (CAR/WALKING, 기본값: CAR)
     * @param maxTravelTime 최대 이동 시간 (분, 기본값: 60)
     * @return 날씨에 적합한 관광지 목록
     */
    @GetMapping("/places/recommend/weather-based")
    public ResponseEntity<List<TourPlaceRecommendation>> getWeatherBasedRecommendations(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "CAR") String transportationMode,
            @RequestParam(defaultValue = "60") int maxTravelTime) {

        log.info("날씨 기반 관광지 추천 요청 - 위치: ({}, {}), 이동수단: {}", lat, lon, transportationMode);

        // 날씨에 따른 테마 자동 선택
        List<String> weatherBasedThemes = getWeatherBasedThemes(lat, lon);

        // 이동 수단 파싱
        RecommendationRequest.TransportationMode mode;
        try {
            mode = RecommendationRequest.TransportationMode.valueOf(transportationMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 이동 수단: {}, 기본값 CAR 사용", transportationMode);
            mode = RecommendationRequest.TransportationMode.CAR;
        }

        RecommendationRequest request = new RecommendationRequest(
                lat, lon, weatherBasedThemes, 30, limit, true, true,
                mode, maxTravelTime, true
        );

        List<TourPlaceRecommendation> recommendations = recommendationService.getRecommendations(request);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 현재 날씨에 따른 적합한 테마 추천
     */
    private List<String> getWeatherBasedThemes(double lat, double lon) {
        try {
            // 날씨 정보 조회
            var weatherSummary = recommendationService.getWeatherService().getWeatherSummary(lat, lon);
            var weatherInfo = weatherSummary.getInfo();
            var alerts = weatherSummary.getAlerts();

            // 기상특보가 있으면 실내 추천
            if (!alerts.isEmpty()) {
                return Arrays.asList("실내", "박물관", "미술관");
            }

            // 날씨 조건에 따른 테마 추천
            int temp = weatherInfo.getTemperature();
            int pop = weatherInfo.getPrecipitationProbability();
            int sky = weatherInfo.getSky();

            if (pop >= 60 || sky == 4) {
                // 비가 오거나 흐리면 실내 추천
                return Arrays.asList("실내", "박물관", "미술관", "온천");
            } else if (temp >= 25 && sky == 1) {
                // 더운 날 맑으면 해변 추천
                return Arrays.asList("해변", "실외", "수상레저");
            } else if (temp >= 15 && temp <= 25) {
                // 적당한 날씨면 다양한 실외 활동
                return Arrays.asList("산", "해변", "실외", "문화재");
            } else {
                // 추우면 실내 추천
                return Arrays.asList("실내", "온천", "박물관");
            }

        } catch (Exception e) {
            log.warn("날씨 기반 테마 추천 실패, 기본값 사용: {}", e.getMessage());
            return Arrays.asList("실외", "산", "해변");
        }
    }
}
