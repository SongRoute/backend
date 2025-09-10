package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.dto.*;
import com.app.yeogigangwon.fetch.KakaoMapApiClient;
import com.app.yeogigangwon.repository.TourPlaceRepository;
import com.app.yeogigangwon.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관광지 추천 서비스
 * 혼잡도, 거리, 테마, 날씨를 종합적으로 고려한 관광지 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourPlaceRecommendationService {

    private final TourPlaceRepository tourPlaceRepository;
    private final WeatherService weatherService;
    private final KtoService ktoService;
    private final KakaoMapApiClient kakaoMapApiClient;

    // 가중치 설정 (이동 시간 고려 시)
    private static final double DISTANCE_WEIGHT = 0.25;
    private static final double CONGESTION_WEIGHT = 0.2;
    private static final double WEATHER_WEIGHT = 0.2;
    private static final double THEME_WEIGHT = 0.15;
    private static final double TRAVEL_TIME_WEIGHT = 0.2;

    /**
     * 종합적인 관광지 추천
     * 
     * @param request 추천 요청 정보
     * @return 추천된 관광지 목록 (점수순 정렬)
     */
    public List<TourPlaceRecommendation> getRecommendations(RecommendationRequest request) {
        log.info("관광지 추천 시작 - 위치: ({}, {}), 선호테마: {}", 
                request.getLatitude(), request.getLongitude(), request.getPreferredThemes());

        try {
            // 1. 기본 조건으로 관광지 필터링
            List<TourPlace> candidatePlaces = getCandidatePlaces(request);
            
            if (candidatePlaces.isEmpty()) {
                log.warn("추천 가능한 관광지가 없습니다");
                return Collections.emptyList();
            }

            // 2. 날씨 정보 조회 (한 번만 조회)
            final WeatherSummary weatherSummary = request.isConsiderWeather() ? 
                getWeatherSummarySafely(request.getLatitude(), request.getLongitude()) : null;

            // 3. 각 관광지에 대해 점수 계산 (이동 시간 포함)
            List<TourPlaceRecommendation> recommendations = candidatePlaces.stream()
                    .map(place -> calculateRecommendationScoreWithTravelTime(place, request, weatherSummary))
                    .filter(Objects::nonNull)
                    .filter(rec -> {
                        // 이동 시간 필터링
                        if (request.isConsiderTravelTime() && request.getMaxTravelTime() > 0) {
                            return rec.getTravelTimeMinutes() <= request.getMaxTravelTime();
                        }
                        return true;
                    })
                    .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore())) // 점수 높은 순
                    .limit(request.getLimit())
                    .collect(Collectors.toList());

            log.info("추천 완료: {}개 관광지 추천", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            log.error("관광지 추천 실패", e);
            throw new RuntimeException("관광지 추천 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기본 조건으로 관광지 필터링
     */
    private List<TourPlace> getCandidatePlaces(RecommendationRequest request) {
        List<TourPlace> allPlaces = tourPlaceRepository.findAll();
        
        return allPlaces.stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .filter(place -> {
                    // 거리 필터링
                    double distance = DistanceCalculator.calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            place.getLatitude(), place.getLongitude()
                    );
                    return distance <= (request.getMaxDistance() * 1000); // km를 미터로 변환
                })
                .collect(Collectors.toList());
    }

    /**
     * 개별 관광지의 추천 점수 계산 (이동 시간 포함)
     */
    private TourPlaceRecommendation calculateRecommendationScoreWithTravelTime(
            TourPlace place, RecommendationRequest request, WeatherSummary weatherSummary) {
        
        try {
            // 거리 점수 계산
            double distance = DistanceCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    place.getLatitude(), place.getLongitude()
            );
            double distanceScore = calculateDistanceScore(distance);

            // 혼잡도 점수 계산
            double congestionScore = calculateCongestionScore(place, request);

            // 날씨 점수 계산
            double weatherScore = calculateWeatherScore(place, weatherSummary, request);

            // 테마 점수 계산
            double themeScore = calculateThemeScore(place, request);

            // 이동 시간 점수 계산
            TravelTimeInfo travelTimeInfo = getTravelTimeInfo(place, request);
            double travelTimeScore = calculateTravelTimeScore(travelTimeInfo, request);

            // 총점 계산 (이동 시간 고려 시)
            double totalScore;
            if (request.isConsiderTravelTime()) {
                totalScore = (distanceScore * DISTANCE_WEIGHT) +
                            (congestionScore * CONGESTION_WEIGHT) +
                            (weatherScore * WEATHER_WEIGHT) +
                            (themeScore * THEME_WEIGHT) +
                            (travelTimeScore * TRAVEL_TIME_WEIGHT);
            } else {
                // 이동 시간 고려하지 않을 때는 기존 가중치 사용
                totalScore = (distanceScore * 0.3) +
                            (congestionScore * 0.25) +
                            (weatherScore * 0.25) +
                            (themeScore * 0.2);
            }

            // 추천 이유 생성
            String reason = generateRecommendationReasonWithTravelTime(
                    distanceScore, congestionScore, weatherScore, themeScore, travelTimeScore, request);

            return new TourPlaceRecommendation(
                    place, distance, totalScore,
                    distanceScore, congestionScore, weatherScore, themeScore, travelTimeScore, reason,
                    request.getTransportationMode() == RecommendationRequest.TransportationMode.CAR ? 
                        travelTimeInfo.getDrivingTime() : travelTimeInfo.getWalkingTime(), 
                    request.getTransportationMode().getDescription(),
                    distance / 1000.0 // km로 변환
            );

        } catch (Exception e) {
            log.warn("관광지 {} 점수 계산 실패: {}", place.getName(), e.getMessage());
            return null;
        }
    }


    /**
     * 거리 점수 계산 (0-100점)
     */
    private double calculateDistanceScore(double distanceInMeters) {
        double distanceInKm = distanceInMeters / 1000.0;
        
        if (distanceInKm <= 5) return 100;
        else if (distanceInKm <= 10) return 80;
        else if (distanceInKm <= 20) return 60;
        else if (distanceInKm <= 30) return 40;
        else return 20;
    }

    /**
     * 혼잡도 점수 계산 (0-100점)
     * KTO 데이터가 없는 관광지에 대해서는 다른 방식으로 점수 계산
     */
    private double calculateCongestionScore(TourPlace place, RecommendationRequest request) {
        try {
            // 1. KTO 혼잡도 데이터 조회 (강원도 주요 관광지만 커버)
            Optional<Double> ktoRate = ktoService.getRateByPlaceName(place.getName(), LocalDate.now());
            
            if (ktoRate.isPresent()) {
                double congestionRate = ktoRate.get();
                log.debug("KTO 혼잡도 데이터 사용 - {}: {}%", place.getName(), congestionRate);
                
                // 혼잡도율을 점수로 변환 (낮을수록 높은 점수)
                if (congestionRate <= 30) return 100;      // 여유
                else if (congestionRate <= 60) return 80;  // 보통
                else if (congestionRate <= 80) return 60;  // 약간 혼잡
                else return 40;                           // 매우 혼잡
            }

            // 2. KTO 데이터가 없으면 관광지 카테고리 기반 추정
            double estimatedScore = estimateCongestionByCategory(place);
            log.debug("카테고리 기반 혼잡도 추정 - {}: {}점", place.getName(), estimatedScore);
            return estimatedScore;

        } catch (Exception e) {
            log.warn("혼잡도 점수 계산 실패: {}", e.getMessage());
            return 70; // 기본값 (중간보다 약간 높게)
        }
    }

    /**
     * 관광지 카테고리 기반 혼잡도 추정
     * KTO 데이터가 없는 관광지에 대한 대안적 점수 계산
     */
    private double estimateCongestionByCategory(TourPlace place) {
        String category = place.getCategory();
        if (category == null) {
            return 70; // 카테고리가 없으면 중간 점수
        }

        String categoryLower = category.toLowerCase();
        
        // 카테고리별 일반적인 혼잡도 패턴 적용
        if (categoryLower.contains("해변") || categoryLower.contains("해수욕장")) {
            // 해변은 성수기(7-8월)에 매우 혼잡하지만, 현재는 중간 정도로 추정
            return 75;
        } else if (categoryLower.contains("산") || categoryLower.contains("등산")) {
            // 산은 상대적으로 여유로움
            return 85;
        } else if (categoryLower.contains("박물관") || categoryLower.contains("미술관")) {
            // 문화시설은 보통 정도
            return 80;
        } else if (categoryLower.contains("온천") || categoryLower.contains("스파")) {
            // 온천은 주말에 혼잡하지만 평일에는 여유로움
            return 75;
        } else if (categoryLower.contains("카페") || categoryLower.contains("식당")) {
            // 음식점은 시간대에 따라 다름, 중간 정도로 추정
            return 70;
        } else if (categoryLower.contains("실내")) {
            // 실내 관광지는 날씨에 관계없이 일정한 혼잡도
            return 75;
        } else if (categoryLower.contains("실외")) {
            // 실외 관광지는 날씨에 따라 혼잡도가 달라짐
            return 80;
        } else {
            // 기타 관광지는 중간 점수
            return 75;
        }
    }

    /**
     * 날씨 점수 계산 (0-100점)
     */
    private double calculateWeatherScore(TourPlace place, WeatherSummary weatherSummary, RecommendationRequest request) {
        if (!request.isConsiderWeather() || weatherSummary == null) {
            return 80; // 날씨 고려하지 않으면 중간 점수
        }

        try {
            WeatherInfo weatherInfo = weatherSummary.getInfo();
            List<WeatherAlert> alerts = weatherSummary.getAlerts();

            // 기상특보가 있으면 해당 지역은 낮은 점수
            if (!alerts.isEmpty()) {
                log.warn("기상특보 발효 중: {}", alerts.get(0).getTitle());
                return 20; // 기상특보 시 낮은 점수
            }

            // 날씨 조건별 점수 계산
            double score = 100;

            // 기온 점수 (15-25도가 최적)
            int temp = weatherInfo.getTemperature();
            if (temp < 5 || temp > 35) score -= 30;
            else if (temp < 10 || temp > 30) score -= 20;
            else if (temp < 15 || temp > 25) score -= 10;

            // 강수확률 점수
            int pop = weatherInfo.getPrecipitationProbability();
            if (pop >= 80) score -= 40;
            else if (pop >= 60) score -= 20;
            else if (pop >= 40) score -= 10;

            // 하늘상태 점수
            int sky = weatherInfo.getSky();
            if (sky == 4) score -= 20; // 흐림
            else if (sky == 3) score -= 10; // 구름많음

            // 풍속 점수 (5m/s 이상이면 감점)
            int windSpeed = weatherInfo.getWindSpeed();
            if (windSpeed >= 10) score -= 20;
            else if (windSpeed >= 5) score -= 10;

            return Math.max(0, Math.min(100, score));

        } catch (Exception e) {
            log.warn("날씨 점수 계산 실패: {}", e.getMessage());
            return 60; // 기본값
        }
    }

    /**
     * 테마 점수 계산 (0-100점)
     */
    private double calculateThemeScore(TourPlace place, RecommendationRequest request) {
        if (request.getPreferredThemes() == null || request.getPreferredThemes().isEmpty()) {
            return 80; // 선호도가 없으면 중간 점수
        }

        String category = place.getCategory();
        if (category == null) {
            return 60; // 카테고리가 없으면 낮은 점수
        }

        // 선호 테마와 매칭 확인
        for (String preferredTheme : request.getPreferredThemes()) {
            if (category.toLowerCase().contains(preferredTheme.toLowerCase())) {
                return 100; // 완전 매칭
            }
        }

        // 부분 매칭 확인
        if (category.contains("실내") && request.getPreferredThemes().contains("실내")) {
            return 90;
        }
        if (category.contains("실외") && request.getPreferredThemes().contains("실외")) {
            return 90;
        }

        return 50; // 매칭되지 않으면 낮은 점수
    }

    /**
     * 이동 시간 정보 조회
     */
    private TravelTimeInfo getTravelTimeInfo(TourPlace place, RecommendationRequest request) {
        try {
            return kakaoMapApiClient.getTravelTime(
                    request.getLatitude(), request.getLongitude(),
                    place.getLatitude(), place.getLongitude()
            );
        } catch (Exception e) {
            log.warn("이동 시간 조회 실패: {}", e.getMessage());
            // 기본값 반환
            return new TravelTimeInfo(
                    "사용자 위치", place.getName(),
                    30, 15, // 차량 30분, 도보 15분 기본값
                    5.0, 1.2, // 차량 5km, 도보 1.2km 기본값
                    "이동 시간 정보 없음", "이동 시간 정보 없음"
            );
        }
    }

    /**
     * 이동 시간 점수 계산 (0-100점)
     */
    private double calculateTravelTimeScore(TravelTimeInfo travelTimeInfo, RecommendationRequest request) {
        if (!request.isConsiderTravelTime()) {
            return 80; // 이동 시간 고려하지 않으면 중간 점수
        }

        int travelTime;
        if (request.getTransportationMode() == RecommendationRequest.TransportationMode.CAR) {
            travelTime = travelTimeInfo.getDrivingTime();
        } else {
            travelTime = travelTimeInfo.getWalkingTime();
        }

        // 이동 시간에 따른 점수 계산 (짧을수록 높은 점수)
        if (travelTime <= 15) return 100;      // 15분 이내: 100점
        else if (travelTime <= 30) return 80;   // 30분 이내: 80점
        else if (travelTime <= 45) return 60;   // 45분 이내: 60점
        else if (travelTime <= 60) return 40;   // 60분 이내: 40점
        else return 20;                         // 60분 초과: 20점
    }

    /**
     * 이동 시간을 고려한 추천 이유 생성
     */
    private String generateRecommendationReasonWithTravelTime(double distanceScore, double congestionScore, 
                                                            double weatherScore, double themeScore, 
                                                            double travelTimeScore, RecommendationRequest request) {
        List<String> reasons = new ArrayList<>();

        if (distanceScore >= 80) reasons.add("가까운 거리");
        if (congestionScore >= 80) reasons.add("여유로운 분위기");
        else if (congestionScore >= 60) reasons.add("적당한 분위기");
        if (weatherScore >= 80) reasons.add("좋은 날씨");
        else if (weatherScore >= 60) reasons.add("괜찮은 날씨");
        if (themeScore >= 80) reasons.add("선호 테마");
        else if (themeScore >= 60) reasons.add("적합한 테마");
        
        if (request.isConsiderTravelTime()) {
            if (travelTimeScore >= 80) reasons.add("빠른 이동");
            else if (travelTimeScore >= 60) reasons.add("적당한 이동시간");
        }

        if (reasons.isEmpty()) {
            return "종합적으로 추천";
        }

        return String.join(", ", reasons) + "로 추천";
    }

    /**
     * 추천 이유 생성 (기존 메서드 - 호환성 유지)
     */
    private String generateRecommendationReason(double distanceScore, double congestionScore, 
                                              double weatherScore, double themeScore) {
        List<String> reasons = new ArrayList<>();

        if (distanceScore >= 80) reasons.add("가까운 거리");
        if (congestionScore >= 80) reasons.add("여유로운 분위기");
        else if (congestionScore >= 60) reasons.add("적당한 분위기");
        if (weatherScore >= 80) reasons.add("좋은 날씨");
        else if (weatherScore >= 60) reasons.add("괜찮은 날씨");
        if (themeScore >= 80) reasons.add("선호 테마");
        else if (themeScore >= 60) reasons.add("적합한 테마");

        if (reasons.isEmpty()) {
            return "종합적으로 추천";
        }

        return String.join(", ", reasons) + "로 추천";
    }

    /**
     * 안전한 날씨 정보 조회 (예외 처리 포함)
     */
    private WeatherSummary getWeatherSummarySafely(double lat, double lon) {
        try {
            return weatherService.getWeatherSummary(lat, lon);
        } catch (Exception e) {
            log.warn("날씨 정보 조회 실패, 날씨 점수는 기본값으로 처리: {}", e.getMessage());
            return null;
        }
    }

    /**
     * WeatherService 접근을 위한 메서드 (컨트롤러에서 사용)
     */
    public WeatherService getWeatherService() {
        return weatherService;
    }
}
